package com.worknest.features.company;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknest.audit.repository.AuditLogRepository;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.SiteTrustedNetwork;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.NetworkType;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.domain.enums.SiteType;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.company.dto.CreateSiteDraftRequest;
import com.worknest.features.company.dto.SiteBasicInfoRequest;
import com.worknest.features.company.dto.TrustedNetworkUpsertRequest;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.company.repository.CompanySiteRepository;
import com.worknest.features.company.repository.SiteTrustedNetworkRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompanySiteIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("worknest_test")
            .withUsername("worknest")
            .withPassword("worknest");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanySiteRepository companySiteRepository;

    @Autowired
    private SiteTrustedNetworkRepository siteTrustedNetworkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Company companyA;
    private Company companyB;
    private User adminUser;
    private UUID roleAssignmentId;

    @BeforeEach
    void setUp() {
        companyRepository.deleteAll();
        userRepository.deleteAll();
        auditLogRepository.deleteAll();

        companyA = createCompany("Company A", "comp-a");
        companyB = createCompany("Company B", "comp-b");

        adminUser = new User();
        adminUser.setEmail("admin@comp-a.com");
        adminUser.setFullName("Admin User");
        adminUser.setPasswordHash("hash");
        adminUser = userRepository.save(adminUser);

        RoleAssignment ra = new RoleAssignment();
        ra.setUser(adminUser);
        ra.setCompany(companyA);
        ra.setRole(PlatformRole.ADMIN);
        ra.setIsActive(true);
        ra = roleAssignmentRepository.save(ra);
        roleAssignmentId = ra.getId();
    }

    private Company createCompany(String name, String slug) {
        Company company = new Company();
        company.setName(name);
        company.setSlug(slug);
        return companyRepository.save(company);
    }

    private void setAuth(Company company, PlatformRole role) {
        AuthSessionPrincipal principal = new AuthSessionPrincipal(
                adminUser.getId(), adminUser.getEmail(), company.getId(), company.getSlug(), roleAssignmentId, role, PlatformAccess.WEB
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ── 1. AUTHORIZATION & TENANCY ──────────────────────────────────────────

    @Test
    void shouldCreateDraftWhenAdminOfCompany() throws Exception {
        setAuth(companyA, PlatformRole.ADMIN);

        CreateSiteDraftRequest request = new CreateSiteDraftRequest(
                "HQ-01", "Main Headquarters", SiteType.HQ, "US", "UTC", "Note"
        );

        mockMvc.perform(post("/api/v1/companies/{companyId}/sites", companyA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("HQ-01")))
                .andExpect(jsonPath("$.status", is("DRAFT")));

        // Verify audit log
        assertEquals(1, auditLogRepository.count());
        assertEquals("SITE_DRAFT_CREATED", auditLogRepository.findAll().get(0).getAction());
    }

    @Test
    void shouldReturn403WhenManagerTriesToCreateSite() throws Exception {
        setAuth(companyA, PlatformRole.MANAGER);

        CreateSiteDraftRequest request = new CreateSiteDraftRequest(
                "HQ-01", "Main Headquarters", SiteType.HQ, "US", "UTC", "Note"
        );

        mockMvc.perform(post("/api/v1/companies/{companyId}/sites", companyA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenAdminOfA_AccessesSiteOfB() throws Exception {
        // Create site for company B
        CompanySite siteB = new CompanySite();
        siteB.setCompany(companyB);
        siteB.setCode("SITE-B");
        siteB.setName("Site B");
        siteB.setStatus(SiteStatus.DRAFT);
        siteB = companySiteRepository.save(siteB);

        // Authenticate as Admin of Company A
        setAuth(companyA, PlatformRole.ADMIN);

        mockMvc.perform(get("/api/v1/sites/{siteId}/setup-status", siteB.getId()))
                .andExpect(status().isNotFound());
    }

    // ── 2. CONCURRENCY & OPTIMISTIC LOCKING ─────────────────────────────────

    @Test
    void shouldReturn409OnManualVersionMismatch() throws Exception {
        setAuth(companyA, PlatformRole.ADMIN);

        CompanySite site = new CompanySite();
        site.setCompany(companyA);
        site.setCode("HQ-02");
        site.setName("Main HQ");
        site.setStatus(SiteStatus.DRAFT);
        site.setVersion(5L); // Current version is 5
        site = companySiteRepository.save(site);

        SiteBasicInfoRequest request = new SiteBasicInfoRequest(
                "HQ-02", "Updated Name", SiteType.HQ, "US", "UTC",
                null, null, null, null, null, true, true, true, "Notes",
                1L // Stale version provided by client
        );

        mockMvc.perform(put("/api/v1/sites/{siteId}/basic-info", site.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SITE_CONFLICT")));
    }

    @Test
    void shouldReturn409AndGenericMessageOnJPAVersionConflict() throws Exception {
        setAuth(companyA, PlatformRole.ADMIN);

        CompanySite site = new CompanySite();
        site.setCompany(companyA);
        site.setCode("HQ-03");
        site.setName("JPA Lock Test");
        site.setStatus(SiteStatus.DRAFT);
        site = companySiteRepository.save(site);
        UUID siteId = site.getId();

        // Simulate another transaction updating the version in the background
        companySiteRepository.updateVersionManually(siteId, 10L); 

        SiteBasicInfoRequest request = new SiteBasicInfoRequest(
                "HQ-03", "Concurrent Update", SiteType.HQ, "US", "UTC",
                null, null, null, null, null, true, true, true, "Notes",
                0L // Original version client thought it had
        );

        mockMvc.perform(put("/api/v1/sites/{siteId}/basic-info", siteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("OPTIMISTIC_LOCK_CONFLICT")))
                .andExpect(jsonPath("$.message", is("Updated in another tab — please refresh and try again.")));
    }

    // ── 3. CIDR VALIDATION ──────────────────────────────────────────────────

    @Test
    void shouldReturn409OnCidrOverlap() throws Exception {
        setAuth(companyA, PlatformRole.ADMIN);

        CompanySite site = new CompanySite();
        site.setCompany(companyA);
        site.setCode("HQ-04");
        site.setName("Overlap Test");
        site.setStatus(SiteStatus.DRAFT);
        site = companySiteRepository.save(site);

        // Add first network
        SiteTrustedNetwork net1 = new SiteTrustedNetwork();
        net1.setSite(site);
        net1.setName("Net 1");
        net1.setCidrBlock("192.168.1.0/24");
        net1.setIsActive(true);
        siteTrustedNetworkRepository.save(net1);

        // Try to add overlapping network
        TrustedNetworkUpsertRequest request = new TrustedNetworkUpsertRequest(
                "Net 2", NetworkType.CIDR_RANGE, "192.168.1.128/25", true, 1, null, null, null
        );

        mockMvc.perform(put("/api/v1/sites/{siteId}/trusted-networks/{id}", site.getId(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("overlaps")));
    }

    // ── 4. ACTIVATION LOGIC ─────────────────────────────────────────────────

    @Test
    void shouldBlockActivationForIncompleteDraft() throws Exception {
        setAuth(companyA, PlatformRole.ADMIN);

        CompanySite site = new CompanySite();
        site.setCompany(companyA);
        site.setCode("HQ-05");
        site.setName("Incomplete Site");
        site.setStatus(SiteStatus.DRAFT);
        site.setLocationRequired(true); // Missing geofence
        site = companySiteRepository.save(site);

        mockMvc.perform(post("/api/v1/sites/{siteId}/activate", site.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SITE_ACTIVATION_BLOCKED")))
                .andExpect(jsonPath("$.fieldErrors", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))));
        
        // Verify failed activation is audited
        assertEquals(1, auditLogRepository.findAll().stream()
                .filter(a -> a.getAction().equals("SITE_ACTIVATION_FAILED"))
                .count());
    }
}
