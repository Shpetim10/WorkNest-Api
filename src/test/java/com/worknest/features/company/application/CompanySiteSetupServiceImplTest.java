package com.worknest.features.company.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.worknest.audit.service.SiteSetupAuditService;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.SiteTrustedNetwork;
import com.worknest.domain.enums.GeofenceShapeType;
import com.worknest.domain.enums.NetworkIpVersion;
import com.worknest.domain.enums.NetworkType;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.domain.enums.SiteType;
import com.worknest.features.company.dto.CreateSiteDraftRequest;
import com.worknest.features.company.dto.SiteActivationResponse;
import com.worknest.features.company.dto.SiteLocationRequest;
import com.worknest.features.company.dto.SiteSetupStatusResponse;
import com.worknest.features.company.dto.TrustedNetworkUpsertRequest;
import com.worknest.features.company.exception.CompanySiteActivationBlockedException;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.company.repository.CompanySiteRepository;
import com.worknest.features.company.repository.SiteTrustedNetworkRepository;
import com.worknest.tenant.TenantContextHolder;
import com.worknest.tenant.TenantSessionContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanySiteSetupServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanySiteRepository companySiteRepository;

    @Mock
    private SiteTrustedNetworkRepository siteTrustedNetworkRepository;

    @Mock
    private SiteSetupAuditService siteSetupAuditService;

    @Mock
    private SiteDetectionServiceImpl siteDetectionService;

    @InjectMocks
    private CompanySiteSetupServiceImpl service;

    private final UUID companyId = UUID.randomUUID();
    private final UUID siteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContextHolder.set(new TenantSessionContext(
                companyId,
                "worknest",
                UUID.randomUUID(),
                PlatformRole.ADMIN,
                PlatformAccess.WEB
        ));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createDraftCreatesDraftSite() {
        Company company = new Company();
        company.setId(companyId);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companySiteRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, "HQ-TIR")).thenReturn(false);
        when(companySiteRepository.save(any(CompanySite.class))).thenAnswer(invocation -> {
            CompanySite site = invocation.getArgument(0);
            site.setId(siteId);
            site.setVersion(0L);
            return site;
        });

        var response = service.createDraft(companyId, new CreateSiteDraftRequest(
                "hq-tir",
                "Tirana HQ",
                SiteType.HQ,
                "AL",
                "Europe/Tirane",
                "Draft"
        ));

        assertEquals(SiteStatus.DRAFT, response.status());
        assertEquals("HQ-TIR", response.code());
        verify(siteSetupAuditService).appendSiteDraftCreated(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getSetupStatusReturnsBlockingIssuesForIncompleteDraft() {
        CompanySite site = buildSite();
        site.setLocationRequired(true);
        site.setGeofenceShapeType(null);

        when(companySiteRepository.findByIdAndCompanyId(siteId, companyId)).thenReturn(Optional.of(site));
        when(siteTrustedNetworkRepository.findAllBySiteIdOrderByPriorityOrderAscIdAsc(siteId)).thenReturn(List.of());

        SiteSetupStatusResponse response = service.getSetupStatus(siteId);

        assertFalse(response.locationComplete());
        assertFalse(response.networkComplete());
        assertFalse(response.readyToActivate());
        assertTrue(response.blockingIssues().stream().anyMatch(issue -> issue.code().equals("SITE_LOCATION_INCOMPLETE")));
        assertTrue(response.blockingIssues().stream().anyMatch(issue -> issue.code().equals("SITE_NETWORK_INCOMPLETE")));
    }

    @Test
    void saveLocationClearsGeofenceFieldsWhenLocationNotRequired() {
        CompanySite site = buildSite();
        site.setGeofenceShapeType(GeofenceShapeType.CIRCLE);
        site.setLatitude(BigDecimal.ONE);
        site.setLongitude(BigDecimal.TEN);
        site.setGeofenceRadiusMeters(100);
        site.setVersion(2L);

        when(companySiteRepository.findByIdAndCompanyId(siteId, companyId)).thenReturn(Optional.of(site));
        when(companySiteRepository.save(any(CompanySite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.saveLocation(siteId, new SiteLocationRequest(
                BigDecimal.ONE,
                BigDecimal.TEN,
                GeofenceShapeType.CIRCLE,
                100,
                null,
                5,
                5,
                30,
                false,
                2L
        ));

        assertFalse(response.locationRequired());
        assertNull(response.geofenceShapeType());
        assertNull(response.latitude());
        assertNull(response.longitude());
        assertNull(response.geofenceRadiusMeters());
    }

    @Test
    void dryRunActivationValidatesWithoutChangingStatus() {
        CompanySite site = buildReadySite();
        SiteTrustedNetwork network = buildActiveNetwork(site);

        when(companySiteRepository.findByIdAndCompanyId(siteId, companyId)).thenReturn(Optional.of(site));
        when(siteTrustedNetworkRepository.findAllBySiteIdOrderByPriorityOrderAscIdAsc(siteId)).thenReturn(List.of(network));

        SiteActivationResponse response = service.activate(siteId, true);

        assertTrue(response.readyToActivate());
        assertFalse(response.activated());
        assertEquals(SiteStatus.DRAFT, response.status());
        verify(companySiteRepository, never()).save(any(CompanySite.class));
    }

    @Test
    void activationFailsWhenSetupIsIncomplete() {
        CompanySite site = buildSite();

        when(companySiteRepository.findByIdAndCompanyId(siteId, companyId)).thenReturn(Optional.of(site));
        when(siteTrustedNetworkRepository.findAllBySiteIdOrderByPriorityOrderAscIdAsc(siteId)).thenReturn(List.of());

        assertThrows(CompanySiteActivationBlockedException.class, () -> service.activate(siteId, false));
    }

    @Test
    void trustedNetworkUpsertDerivesIpVersionFromCidr() {
        CompanySite site = buildSite();

        when(companySiteRepository.findByIdAndCompanyId(siteId, companyId)).thenReturn(Optional.of(site));
        when(siteTrustedNetworkRepository.findByIdAndSiteId(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());
        when(siteTrustedNetworkRepository.save(any(SiteTrustedNetwork.class))).thenAnswer(invocation -> {
            SiteTrustedNetwork network = invocation.getArgument(0);
            network.setVersion(0L);
            return network;
        });

        var response = service.upsertTrustedNetwork(siteId, UUID.randomUUID(), new TrustedNetworkUpsertRequest(
                "Office WiFi",
                NetworkType.CIDR_RANGE,
                "192.168.0.0/24",
                true,
                1,
                Instant.now().plusSeconds(3600),
                null,
                null
        ));

        assertEquals(NetworkIpVersion.IPV4, response.ipVersion());
        verify(siteDetectionService).validateCidrForUpsert(any(), any(), any());
        verify(siteSetupAuditService).appendTrustedNetworkUpserted(
                any(UUID.class), any(UUID.class), any(UUID.class), any(String.class), anyBoolean(), any(), any(), any()
        );
    }

    @Test
    void assertSiteVersionThrowsConflictOnMismatch() {
        CompanySite site = buildSite();
        site.setVersion(5L);

        when(companySiteRepository.findByIdAndCompanyId(siteId, companyId)).thenReturn(Optional.of(site));

        var request = new com.worknest.features.company.dto.SiteBasicInfoRequest(
                "HQ-TIR", "New Name", SiteType.HQ,
                null, null, null, null, null, "AL", "Europe/Tirane",
                true, true, true, 1L, null // 1L is stale version
        );

        assertThrows(com.worknest.features.company.exception.CompanySiteConflictException.class, () ->
                service.saveBasicInfo(siteId, request)
        );
    }

    private CompanySite buildSite() {
        Company company = new Company();
        company.setId(companyId);

        CompanySite site = new CompanySite();
        site.setId(siteId);
        site.setCompany(company);
        site.setCode("HQ-TIR");
        site.setName("Tirana HQ");
        site.setType(SiteType.HQ);
        site.setStatus(SiteStatus.DRAFT);
        site.setCountryCode("AL");
        site.setTimezone("Europe/Tirane");
        site.setLocationRequired(true);
        site.setVersion(1L);
        return site;
    }

    private CompanySite buildReadySite() {
        CompanySite site = buildSite();
        site.setGeofenceShapeType(GeofenceShapeType.CIRCLE);
        site.setLatitude(new BigDecimal("41.3275"));
        site.setLongitude(new BigDecimal("19.8189"));
        site.setGeofenceRadiusMeters(100);
        return site;
    }

    private SiteTrustedNetwork buildActiveNetwork(CompanySite site) {
        SiteTrustedNetwork network = new SiteTrustedNetwork();
        network.setId(UUID.randomUUID());
        network.setSite(site);
        network.setName("Office");
        network.setNetworkType(NetworkType.CIDR_RANGE);
        network.setCidrBlock("192.168.0.0/24");
        network.setIpVersion(NetworkIpVersion.IPV4);
        network.setIsActive(true);
        network.setVersion(1L);
        network.setCreatedAt(Instant.now());
        network.setUpdatedAt(Instant.now());
        return network;
    }
}
