# Company Site Main Details Update Architecture

This document defines the backend architecture specifically for the **Main Details** modal of an existing `CompanySite`. It enforces strict boundaries, ensuring this flow is completely decoupled from location and network updates.

## 1. Endpoints

### Read Endpoint
**`GET /api/v1/company-sites/{siteId}/main-details`**

While a unified read endpoint was proposed in the previous design, a targeted modal requires only a subset. If optimizing heavily for modal performance without overfetching, this dedicated read endpoint is ideal.

**Response:** `200 OK` with `CompanySiteMainDetailsReadDto`

### Update Endpoint
**`PUT /api/v1/company-sites/{siteId}/main-details`**

**Request:** `CompanySiteMainDetailsUpdateRequest`
**Response:** `200 OK` with `CompanySiteMainDetailsReadDto` (returning the updated view)

---

## 2. DTO Definitions

### Read Flow
```java
public record CompanySiteMainDetailsReadDto(
    UUID id,
    String code,
    String name,
    SiteType type,
    SiteStatus status,
    String countryCode,
    String timezone,
    String notes,
    Boolean qrEnabled,
    Boolean checkInEnabled,
    Boolean checkOutEnabled,
    Long version
) {}
```

### Write Flow
The payload is strictly limited to the fields the modal is permitted to edit. No address or geofence properties exist here.

```java
public record CompanySiteMainDetailsUpdateRequest(
    @NotBlank(message = "Site code is required.")
    @Size(max = 50, message = "Code must not exceed 50 characters.")
    String code,

    @NotBlank(message = "Site name is required.")
    @Size(max = 255, message = "Name must not exceed 255 characters.")
    String name,

    @NotNull(message = "Site type is required.")
    SiteType type,

    @NotNull(message = "Status cannot be null.")
    SiteStatus status,

    @NotBlank(message = "Country code is required.")
    @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters.")
    String countryCode,

    @NotBlank(message = "Timezone is required.")
    @Size(max = 100, message = "Timezone must not exceed 100 characters.")
    String timezone,

    String notes,

    // Toggles are optional in the request, but if provided, must be valid.
    // In many UI designs, missing boolean implies false, or we enforce NotNull
    // if the UI always sends them. Assuming UI always sends the current toggle state:
    @NotNull(message = "QR Enabled flag must be provided.")
    Boolean qrEnabled,

    @NotNull(message = "Check-in Enabled flag must be provided.")
    Boolean checkInEnabled,

    @NotNull(message = "Check-out Enabled flag must be provided.")
    Boolean checkOutEnabled,

    @NotNull(message = "Version is required for optimistic locking.")
    Long version
) {}
```

---

## 3. Validation Rules

1. **JSR-380 (Bean Validation):** Executed automatically on the controller layer (`@Valid`). Validates string lengths, nullity, and basic formats.
2. **Timezone validation:** A custom validator or service-layer check must verify that the `timezone` string is a valid `ZoneId` (e.g., `ZoneId.of(timezone)` does not throw).
3. **Country code validation:** Implement a service-layer check or custom `@CountryCode` validator.
4. **Attendance Toggles:** A service-layer rule should enforce that a site cannot have all three (`qrEnabled`, `checkInEnabled`, `checkOutEnabled`) set to `false` if the site is intended to be used for attendance.
5. **Code Uniqueness:** The `code` must be unique per `companyId`.

---

## 4. Status Transition Design

We must guard against blind status transitions given by the client.

- **DRAFT -> ACTIVE:** Prohibited via this endpoint. Moving to `ACTIVE` requires full geofence/location validation, which this service does not check. (Use a dedicated `POST /activate` endpoint for this).
- **ACTIVE -> DISABLED:** Allowed.
- **DISABLED -> ACTIVE:** Allowed, **BUT** the service must verify that `locationRequired` and geofences are still valid before allowing it. If invalid, throw an exception.
- **ACTIVE/DISABLED -> ARCHIVED:** Allowed (if ARCHIVED exists).
- **ARCHIVED -> (Anything):** Prohibited. Archiving is generally a terminal state.

```java
// Example State Transition Guard logic:
if (currentStatus != newStatus) {
    if (newStatus == SiteStatus.ACTIVE && currentStatus == SiteStatus.DRAFT) {
        throw new InvalidStatusTransitionException("Cannot activate a DRAFT site via Main Details update. Use the activation endpoint.");
    }
    if (newStatus == SiteStatus.ACTIVE && currentStatus == SiteStatus.DISABLED) {
       // verifyLocationCompleteness(site); // Must pull full entity to check location
    }
    // and so on...
}
```

---

## 5. Conflict Resolution & Security

### Optimistic Locking
- The client sends the `version` read from the GET request.
- The service retrieves the entity and compares `request.version()` with `entity.getVersion()`.
- If mismatched, throw a custom `StaleDataConflictException` mapping to **409 Conflict** with message: *"This site was recently modified by another user. Please refresh and try again."*

### Code Uniqueness (Duplicate Conflict)
- Prior to updating the entity, the service queries: `existsByCompanyIdAndCodeIgnoreCaseAndIdNot(companyId, request.code(), siteId)`.
- If `true`, throw `SiteCodeAlreadyExistsException` mapped to **409 Conflict** with message: *"A site with code [CODE] already exists in your company."*

### Authorization Boundaries
- Retrieve `companyId` from the authenticated user's JWT.
- Lookup the site using **`findByIdAndCompanyId(siteId, companyId)`**.
- If no record is found, throw `EntityNotFoundException` mapped to **404 Not Found**. Do not throw 403 to prevent leaking existence.
- Only users with `ADMIN` or `SITE_MANAGER` authority are permitted to call the `PUT` endpoint (enforced via `@PreAuthorize`).

---

## 6. Repository & Service Structure

### Repository
```java
public interface CompanySiteRepository extends JpaRepository<CompanySite, UUID> {
    Optional<CompanySite> findByIdAndCompanyId(UUID id, UUID companyId);
    boolean existsByCompanyIdAndCodeIgnoreCaseAndIdNot(UUID companyId, String code, UUID id);
}
```

### Transaction Boundary
The single service method handling the `PUT` request will be marked with `@Transactional`. If any validation fails, an unmarshaled exception is thrown, rolling back any implicit writes.

---

## 7. Sample Code Skeletons

**Controller:**
```java
@RestController
@RequestMapping("/api/v1/company-sites/{siteId}/main-details")
@RequiredArgsConstructor
public class CompanySiteMainDetailsController {

    private final CompanySiteMainDetailsService mainDetailsService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN', 'SITE_MANAGER')")
    public ResponseEntity<CompanySiteMainDetailsReadDto> getDetails(@PathVariable UUID siteId) {
        UUID companyId = AuthUtils.getCurrentCompanyId();
        return ResponseEntity.ok(mainDetailsService.getDetails(companyId, siteId));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN', 'SITE_MANAGER')")
    public ResponseEntity<CompanySiteMainDetailsReadDto> updateDetails(
            @PathVariable UUID siteId,
            @Valid @RequestBody CompanySiteMainDetailsUpdateRequest request) {
        
        UUID companyId = AuthUtils.getCurrentCompanyId();
        CompanySiteMainDetailsReadDto updated = mainDetailsService.updateDetails(companyId, siteId, request);
        return ResponseEntity.ok(updated);
    }
}
```

**Service Implementation:**
```java
@Service
@Transactional
@RequiredArgsConstructor
public class CompanySiteMainDetailsServiceImpl implements CompanySiteMainDetailsService {

    private final CompanySiteRepository repository;

    @Override
    public CompanySiteMainDetailsReadDto updateDetails(UUID companyId, UUID siteId, CompanySiteMainDetailsUpdateRequest request) {
        CompanySite site = repository.findByIdAndCompanyId(siteId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company site not found"));

        // 1. Optimistic Lock Check
        if (!site.getVersion().equals(request.version())) {
            throw new StaleObjectStateException("Site was modified in another session.");
        }

        // 2. Uniqueness Check
        if (!site.getCode().equalsIgnoreCase(request.code()) && 
            repository.existsByCompanyIdAndCodeIgnoreCaseAndIdNot(companyId, request.code(), siteId)) {
            throw new DuplicateResourceException("Site code '" + request.code() + "' already exists.");
        }

        // 3. Timezone Validation
        try {
            ZoneId.of(request.timezone());
        } catch (DateTimeException e) {
            throw new ValidationException("Invalid timezone identifier.");
        }

        // 4. Status Transition Guard
        guardStatusTransition(site.getStatus(), request.status(), site);

        // 5. Apply Updates
        site.setCode(request.code());
        site.setName(request.name());
        site.setType(request.type());
        site.setStatus(request.status());
        site.setCountryCode(request.countryCode());
        site.setTimezone(request.timezone());
        site.setNotes(request.notes());
        site.setQrEnabled(request.qrEnabled());
        site.setCheckInEnabled(request.checkInEnabled());
        site.setCheckOutEnabled(request.checkOutEnabled());

        repository.save(site);

        return mapToDto(site);
    }
    
    private void guardStatusTransition(SiteStatus current, SiteStatus target, CompanySite site) {
        if (current == target) return;
        
        if (target == SiteStatus.ACTIVE && current == SiteStatus.PENDING_REVIEW /* or DRAFT */) {
            throw new IllegalStateException("Cannot implicitly activate site. Use the explicit activation flow.");
        }
    }
    
    // mapToDto()...
}
```

---

## 8. Test Plan

1. **Security Isolation:**
    - Authenticate as Tenant A. Attempt to view/edit Tenant B's site. Expect `404 Not Found`.
2. **Optimistic Locking:**
    - Retrieve site with version 1.
    - Submit update with `version: 0`. Expect `409 Conflict`.
    - Submit update with `version: 1`. Expect `200 OK` and returned DTO has `version: 2`.
3. **Field Validation:**
    - Submit invalid timezone (`"Fake/Zone"`). Expect `400 Bad Request`.
    - Submit missing code or name. Expect `400 Bad Request` with structured field errors.
4. **Duplicate Code:**
    - In Tenant A, create Site "HQ". Attempt to rename "BRANCH" to "HQ" in Tenant A. Expect `409 Conflict`.
    - Attempt to rename "BRANCH" in Tenant A to "HQ" where "HQ" belongs to Tenant B. Expect `200 OK` (uniqueness is company-scoped).
5. **State Transition Constraint:**
    - Attempt to update a `DRAFT/PENDING_REVIEW` site directly to `ACTIVE`. Expect failure `400/409` (depending on exception mapping).
    - Attempt to update an `ACTIVE` site to `DISABLED`. Expect success.
6. **Immutable Fields Protection:**
    - Verify that even if the request payload was manually crafted to include JSON properties like `latitude` or `geofenceRadiusMeters` (since they are in the entity), Jackson ignores them (as they are not in the DTO) and the service code never maps them to the entity, ensuring location data remains completely untouched.
