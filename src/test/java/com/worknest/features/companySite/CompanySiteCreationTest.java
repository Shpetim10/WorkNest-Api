package com.worknest.features.companySite;

import com.worknest.domain.enums.GeofenceShapeType;
import com.worknest.domain.enums.LocationDetectionSource;
import com.worknest.domain.enums.NetworkIpVersion;
import com.worknest.domain.enums.NetworkType;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.domain.enums.SiteType;
import com.worknest.features.companySite.application.CompanySiteCreationService;
import com.worknest.features.companySite.application.CompanySiteQueryService;
import com.worknest.features.companySite.dto.CompanySiteResponse;
import com.worknest.features.companySite.dto.CreateSiteRequest;
import com.worknest.features.companySite.dto.CreateSiteResponse;
import com.worknest.features.companySite.dto.SiteLocationRequest;
import com.worknest.features.companySite.dto.TrustedNetworkRequest;
import com.worknest.features.companySite.exception.InvalidCidrException;
import com.worknest.features.companySite.exception.InvalidGeofenceException;
import com.worknest.features.companySite.exception.SiteCodeAlreadyExistsException;
import com.worknest.features.companySite.validation.CidrValidator;
import com.worknest.features.companySite.validation.GeofenceValidator;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration and unit tests for the company site creation flow.
 *
 * <p>Tests are organized by concern:
 * <ul>
 *   <li>{@link CidrValidatorTests} – pure unit tests for CIDR validation.</li>
 *   <li>{@link GeofenceValidatorTests} – pure unit tests for geofence completeness.</li>
 *   <li>{@link CompanySiteCreationTests} – Spring integration tests using the full app context.</li>
 * </ul>
 */
public class CompanySiteCreationTest {

    // ══════════════════════════════════════════════════════════════════════════
    // CidrValidator unit tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class CidrValidatorTests {

        @Test
        void validIpv4SingleHost_passes() {
            // Should not throw
            CidrValidator.validate("203.0.113.42/32", NetworkIpVersion.IPV4);
        }

        @Test
        void validIpv4Range_passes() {
            CidrValidator.validate("203.0.113.0/24", NetworkIpVersion.IPV4);
        }

        @Test
        void validIpv6Host_passes() {
            CidrValidator.validate("2001:db8::1/128", NetworkIpVersion.IPV6);
        }

        @Test
        void missingSlash_throws() {
            assertThatThrownBy(() -> CidrValidator.validate("203.0.113.42", NetworkIpVersion.IPV4))
                    .isInstanceOf(InvalidCidrException.class)
                    .hasMessageContaining("CIDR notation");
        }

        @Test
        void invalidPrefixNonNumeric_throws() {
            assertThatThrownBy(() -> CidrValidator.validate("203.0.113.0/abc", NetworkIpVersion.IPV4))
                    .isInstanceOf(InvalidCidrException.class);
        }

        @Test
        void prefixOutOfRange_throws() {
            assertThatThrownBy(() -> CidrValidator.validate("203.0.113.0/33", NetworkIpVersion.IPV4))
                    .isInstanceOf(InvalidCidrException.class)
                    .hasMessageContaining("out of range");
        }

        @Test
        void ipVersionMismatch_throws() {
            // IPv4 CIDR declared as IPV6
            assertThatThrownBy(() -> CidrValidator.validate("203.0.113.0/24", NetworkIpVersion.IPV6))
                    .isInstanceOf(InvalidCidrException.class)
                    .hasMessageContaining("does not match");
        }

        @Test
        void isSingleHost_trueForSlash32() {
            assertThat(CidrValidator.isSingleHost("203.0.113.42/32")).isTrue();
        }

        @Test
        void isSingleHost_falseForSlash24() {
            assertThat(CidrValidator.isSingleHost("203.0.113.0/24")).isFalse();
        }

        @Test
        void normalize_lowercasesAndTrims() {
            assertThat(CidrValidator.normalize("  203.0.113.0/24  ")).isEqualTo("203.0.113.0/24");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GeofenceValidator unit tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class GeofenceValidatorTests {

        @Test
        void circleWithRadius_passes() {
            GeofenceValidator.validate(circleLocation(150, null));
        }

        @Test
        void circleWithoutRadius_throws() {
            assertThatThrownBy(() -> GeofenceValidator.validate(circleLocation(null, null)))
                    .isInstanceOf(InvalidGeofenceException.class)
                    .hasMessageContaining("geofenceRadiusMeters");
        }

        @Test
        void circleWithPolygonJson_throws() {
            assertThatThrownBy(() -> GeofenceValidator.validate(circleLocation(150, "{}")))
                    .isInstanceOf(InvalidGeofenceException.class)
                    .hasMessageContaining("geofencePolygonGeoJson");
        }

        @Test
        void polygonWithGeoJson_passes() {
            GeofenceValidator.validate(polygonLocation("{\"type\":\"Polygon\",\"coordinates\":[]}", null));
        }

        @Test
        void polygonWithBlankGeoJson_throws() {
            assertThatThrownBy(() -> GeofenceValidator.validate(polygonLocation("  ", null)))
                    .isInstanceOf(InvalidGeofenceException.class)
                    .hasMessageContaining("geofencePolygonGeoJson");
        }

        @Test
        void polygonWithRadius_throws() {
            assertThatThrownBy(() -> GeofenceValidator.validate(polygonLocation("{\"type\":\"Polygon\"}", 100)))
                    .isInstanceOf(InvalidGeofenceException.class)
                    .hasMessageContaining("geofenceRadiusMeters");
        }

        @Test
        void invalidGeoJsonNotObject_throws() {
            assertThatThrownBy(() -> GeofenceValidator.validate(polygonLocation("[1,2,3]", null)))
                    .isInstanceOf(InvalidGeofenceException.class)
                    .hasMessageContaining("GeoJSON");
        }

        // ── Helpers ──

        private SiteLocationRequest circleLocation(Integer radius, String geoJson) {
            return new SiteLocationRequest(
                    new BigDecimal("41.3275"), new BigDecimal("19.8187"),
                    null, null, "Tirana", null, null,
                    GeofenceShapeType.CIRCLE, radius, geoJson,
                    null, null, null, LocationDetectionSource.BROWSER_GEOLOCATION
            );
        }

        private SiteLocationRequest polygonLocation(String geoJson, Integer radius) {
            return new SiteLocationRequest(
                    new BigDecimal("41.3275"), new BigDecimal("19.8187"),
                    null, null, "Tirana", null, null,
                    GeofenceShapeType.POLYGON, radius, geoJson,
                    null, null, null, LocationDetectionSource.BROWSER_GEOLOCATION
            );
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Integration tests — requires full Spring Boot context + test DB
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @Transactional
    class CompanySiteCreationTests {

        @Autowired
        CompanySiteCreationService creationService;

        @Autowired
        CompanySiteQueryService queryService;

        /** Must be a valid, existing company ID seeded in the test data. */
        private static final UUID TEST_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

        @Test
        void createSite_withCircleGeofenceAndOneNetwork_returnsCorrectResponse() {
            CreateSiteRequest request = buildValidRequest("INTEG-01", List.of(
                    new TrustedNetworkRequest("Office LAN", NetworkType.OFFICE_NETWORK,
                            "203.0.113.0/24", NetworkIpVersion.IPV4, 1, "Test rule", null)
            ));

            CreateSiteResponse response = creationService.createSite(TEST_COMPANY_ID, request, "203.0.113.1");

            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
            assertThat(response.code()).isEqualTo("INTEG-01");
            assertThat(response.status()).isEqualTo(SiteStatus.PENDING_REVIEW);
            assertThat(response.trustedNetworks()).hasSize(1);
            assertThat(response.trustedNetworks().getFirst().cidrBlock()).isEqualTo("203.0.113.0/24");
        }

        @Test
        void createSite_withNoNetworks_succeeds() {
            CreateSiteRequest request = buildValidRequest("INTEG-02", List.of());
            CreateSiteResponse response = creationService.createSite(TEST_COMPANY_ID, request, "10.0.0.1");
            assertThat(response.trustedNetworks()).isEmpty();
        }

        @Test
        void createSite_duplicateCode_throwsSiteCodeAlreadyExistsException() {
            CreateSiteRequest request = buildValidRequest("DUPE-CODE", List.of());
            // First create succeeds
            creationService.createSite(TEST_COMPANY_ID, request, "10.0.0.1");
            // Second create with same code must fail
            assertThatThrownBy(() -> creationService.createSite(TEST_COMPANY_ID, request, "10.0.0.1"))
                    .isInstanceOf(SiteCodeAlreadyExistsException.class);
        }

        @Test
        void createSite_duplicateCidrInRequest_throwsDuplicateTrustedNetworkException() {
            CreateSiteRequest request = buildValidRequest("DUP-NET-01", List.of(
                    new TrustedNetworkRequest("Rule A", NetworkType.OFFICE_NETWORK,
                            "203.0.113.0/24", NetworkIpVersion.IPV4, 1, null, null),
                    new TrustedNetworkRequest("Rule B", NetworkType.OFFICE_NETWORK,
                            "203.0.113.0/24", NetworkIpVersion.IPV4, 2, null, null)
            ));

            assertThatThrownBy(() -> creationService.createSite(TEST_COMPANY_ID, request, "10.0.0.1"))
                    .isInstanceOf(com.worknest.features.companySite.exception.DuplicateTrustedNetworkException.class);
        }

        @Test
        void createSite_invalidCidr_throwsInvalidCidrException() {
            CreateSiteRequest request = buildValidRequest("BAD-CIDR", List.of(
                    new TrustedNetworkRequest("Bad", NetworkType.MANUAL_CIDR,
                            "not-a-cidr", NetworkIpVersion.IPV4, 1, null, null)
            ));

            assertThatThrownBy(() -> creationService.createSite(TEST_COMPANY_ID, request, "10.0.0.1"))
                    .isInstanceOf(InvalidCidrException.class);
        }

        @Test
        void createSite_circleGeofenceMissingRadius_throwsInvalidGeofenceException() {
            SiteLocationRequest badLocation = new SiteLocationRequest(
                    new BigDecimal("41.3275"), new BigDecimal("19.8187"),
                    null, null, "Tirana", null, null,
                    GeofenceShapeType.CIRCLE, null /* missing radius */, null,
                    null, null, null, LocationDetectionSource.BROWSER_GEOLOCATION
            );
            CreateSiteRequest request = new CreateSiteRequest(
                    "Bad Geofence Site", "BAD-GEO", SiteType.HQ,
                    "AL", "Europe/Tirane", null,
                    true, true, true, true,
                    badLocation, List.of()
            );

            assertThatThrownBy(() -> creationService.createSite(TEST_COMPANY_ID, request, "10.0.0.1"))
                    .isInstanceOf(InvalidGeofenceException.class);
        }

        @Test
        void listSites_returnsNewestFirst() {
            creationService.createSite(TEST_COMPANY_ID, buildValidRequest("LIST-01", List.of()), "10.0.0.1");
            creationService.createSite(TEST_COMPANY_ID, buildValidRequest("LIST-02", List.of()), "10.0.0.1");

            List<CompanySiteResponse> sites = queryService.listSites(TEST_COMPANY_ID)
                    .stream()
                    .filter(site -> site.code().startsWith("LIST-"))
                    .toList();

            assertThat(sites).hasSize(2);
            assertThat(sites.get(0).code()).isEqualTo("LIST-02");
            assertThat(sites.get(1).code()).isEqualTo("LIST-01");
            assertThat(sites.get(0).createdAt()).isAfterOrEqualTo(sites.get(1).createdAt());
        }

        // ── Helpers ──

        private CreateSiteRequest buildValidRequest(String code, List<TrustedNetworkRequest> networks) {
            SiteLocationRequest location = new SiteLocationRequest(
                    new BigDecimal("41.3275"), new BigDecimal("19.8187"),
                    "Rruga Ismail Qemali", null, "Tirana", "Tirana County", "1001",
                    GeofenceShapeType.CIRCLE, 150, null,
                    20, 30, 50, LocationDetectionSource.BROWSER_GEOLOCATION
            );
            return new CreateSiteRequest(
                    "Test Site " + code, code, SiteType.HQ,
                    "AL", "Europe/Tirane", "Integration test",
                    true, true, true, true,
                    location, networks
            );
        }
    }
}
