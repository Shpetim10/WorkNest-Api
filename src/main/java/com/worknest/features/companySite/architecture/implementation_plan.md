# Company Site Update Architecture

This document details a robust, production-ready backend architecture for updating `CompanySite` entities using a modular, per-responsibility approach. 

## 1. Final Architecture Decision

We will adopt a **Granular Write / Unified Read** architecture.

### Read Strategy: Single Comprehensive Read Endpoint
**Decision:** We will use **one comprehensive site details endpoint** (`GET /api/company-sites/{siteId}`) that returns the full site aggregate (including its `SiteTrustedNetwork` collection).

**Why this is the better production architecture:**
1. **Client-Side Caching (React Query / SWR):** Modern frontends use cache keys (e.g., `['site', siteId]`). Having a single GET endpoint means any update to the site automatically invalidates/updates this single cache entry, ensuring total consistency across the entire UI.
2. **Instant Modal Opening:** The UI can open modals instantly from the cached aggregate. If you want ultra-fresh data on modal open, the UI simply triggers a background refetch of the single endpoint, updating the cache without blocking the render.
3. **Reduced Backend Complexity:** Granular reads would require multiple DTOs and multiple controller endpoints for fetching data that resides on the same JPA entity (`CompanySite`).

### Write Strategy: Segregated Update Endpoints
**Decision:** We will use dedicated update endpoints (`PUT .../details`, `PUT .../location`, and granular network endpoints) that map precisely to the frontend modals.

**Why:**
1. **Reduced Payload Size & Complexity:** Clients only send what changed.
2. **Clear Ownership & Validation Boundaries:** Location validation logic won't interfere with a simple name change.
3. **Zero Risk of Accidental Nullification:** Sending only the location fields guarantees you aren't accidentally wiping out the `name` or `code` because you forgot to include them in a giant payload.

## 2. Endpoint List

### Read Endpoints
- `GET /api/v1/company-sites/{id}` -> Returns `CompanySiteDetailsDto` (includes `List<SiteTrustedNetworkDto>`).

### Update Endpoints
- `PUT /api/v1/company-sites/{id}/details` -> Update Main Details.
- `PUT /api/v1/company-sites/{id}/location` -> Update Location & Geofence.

### Network Management Endpoints
- `POST /api/v1/company-sites/{id}/networks` -> Create Trusted Network.
- `PUT /api/v1/company-sites/{id}/networks/{networkId}` -> Update Trusted Network.
- `DELETE /api/v1/company-sites/{id}/networks/{networkId}` -> Delete Trusted Network.
- `PATCH /api/v1/company-sites/{id}/networks/{networkId}/status` -> Toggle Network Status.

### Detection Helper
- `GET /api/v1/company-sites/network-assist/detect-ip` -> Returns the client's current IP and derived connection type (advisory only).

## 3. DTO Definitions

### Read Flow
```java
public record CompanySiteDetailsDto(
    UUID id,
    String code,
    String name,
    SiteType type,
    SiteStatus status,
    Long version, // Critical for all updates
    
    // Location Data
    String addressLine1, String addressLine2, String city, String stateRegion, 
    String postalCode, String countryCode, String timezone,
    BigDecimal latitude, BigDecimal longitude,
    GeofenceShapeType geofenceShapeType, Integer geofenceRadiusMeters,
    String geofencePolygonGeoJson, Integer entryBufferMeters, Integer exitBufferMeters, 
    Integer maxLocationAccuracyMeters, Boolean locationRequired,
    
    // Config Data
    Boolean qrEnabled, Boolean checkInEnabled, Boolean checkOutEnabled,
    
    String notes,
    Instant createdAt, Instant updatedAt,
    
    // Nested Networks
    List<SiteTrustedNetworkDto> trustedNetworks
) {}

public record SiteTrustedNetworkDto(
    UUID id, String name, NetworkType networkType, String cidrBlock,
    NetworkIpVersion ipVersion, Boolean isActive, Integer priorityOrder, 
    String notes, Instant expiresAt, Long version
) {}
```

### Write Flow (Modals)
```java
// Modal 1: Details Update Payload
public record CompanySiteDetailsUpdateRequest(
    @NotBlank @Size(max=50) String code,
    @NotBlank @Size(max=255) String name,
    @NotNull SiteType type,
    Boolean qrEnabled,
    Boolean checkInEnabled,
    Boolean checkOutEnabled,
    String notes,
    @NotNull Long version // Required for Optimistic Locking
) {}

// Modal 2: Location Update Payload
public record CompanySiteLocationUpdateRequest(
    @Size(max=255) String addressLine1,
    @Size(max=255) String addressLine2,
    @Size(max=100) String city,
    @Size(max=100) String stateRegion,
    @Size(max=30) String postalCode,
    @NotBlank @Size(min=2, max=2) String countryCode,
    @NotBlank @Size(max=100) String timezone,
    
    BigDecimal latitude, 
    BigDecimal longitude,
    GeofenceShapeType geofenceShapeType, 
    Integer geofenceRadiusMeters,
    String geofencePolygonGeoJson, 
    
    @Min(0) Integer entryBufferMeters, 
    @Min(0) Integer exitBufferMeters, 
    @Min(0) Integer maxLocationAccuracyMeters,
    
    @NotNull Boolean locationRequired,
    @NotNull Long version
) {}

// Modal 3: Network Create/Update Payload
public record SiteTrustedNetworkSaveRequest(
    @NotBlank @Size(max=100) String name,
    @NotNull NetworkType networkType,
    @NotBlank @Size(max=100) String cidrBlock,
    String notes,
    Instant expiresAt,
    Long version // Required ONLY for PUT (Update)
) {}

public record NetworkStatusToggleRequest(
    @NotNull Boolean isActive,
    @NotNull Long version
) {}
```
*(Note: IP Version and Priority Order are omitted from the Network Request as requested—these are backend-derived).*

## 4. Service Classes & Responsibilities

| Service | Responsibility |
|---|---|
| `CompanySiteQueryService` | Fetches site aggregates. Enforces authorization (tenant checks). |
| `CompanySiteUpdateService` | Handles modal updates for `Details` and `Location`. Loads entity, verifies tenant, applies locking check, delegates to validators, maps data, and saves. |
| `SiteTrustedNetworkService` | Handles CRUD operations for trusted networks. Includes computation logic for `ipVersion` (e.g., checking if the CIDR contains `:` or `.`) and re-calculating `priorityOrder`. |

## 5. Validation Rules per Endpoint

**Details Endpoint Validation:**
*   **Unique Constraint:** Verify `code` is unique within the company scope (excluding self).
*   **State Constraints:** Cannot disable `qrEnabled`, `checkInEnabled` and `checkOutEnabled` all at once (business rule assumption). 

**Location Endpoint Validation:**
*   **Geofence Completeness:** If `locationRequired` is true, the `countryCode` must be present. The geofence must be fully defined: if `CIRCLE`, require lat/lng and radius; if `POLYGON`, require GeoJSON string. 
*   Warning: No more reliance on `@PrePersist`/`@PreUpdate` for validation. All strict validation goes here.
*   **Buffer rules:** Optional, but `entryBuffer` and `exitBuffer` shouldn't exceed logical maximums.

**Network Endpoints Validation:**
*   **CIDR Format:** Strictly validate `cidrBlock` using a regex/library.
*   **Unique Rule:** (Site, CIDR, Network Type) must be unique. Throw 409 Conflict if violated.

## 6. Concurrency Strategy with `@Version`

1.  **Frontend Requirement:** The client MUST include the `version` property from the DTO in the update payload.
2.  **Backend Implementation:** In the Service layer, compare the `payload.version` with `entity.getVersion()`. If they do not match, immediately throw a `ConcurrentModificationException` or a custom `StaleDataConflictException`.
3.  **JPA Fallback:** If the service layer check passes but another transaction commits a millisecond before yours, JPA will throw `OptimisticLockException` automatically.
4.  **Global Exception Handler:** Catch both the manual exception and JPA's `OptimisticLockException` in a `@RestControllerAdvice` and translate to `409 Conflict` with a user-friendly message: *"This record was recently modified in another session. Please hit refresh to get the latest data before making changes."*

## 7. Security / Authorization boundaries

Every update endpoint must perform a **Tenant Check**:
1.  Extract `companyId` from the authenticated JWT (e.g., via `SecurityContext`).
2.  When retrieving the site from the DB, do **not** just use `findById(siteId)`. Use `findByIdAndCompanyId(siteId, userCompanyId)`. This completely eliminates IDOR (Insecure Direct Object Reference) vulnerabilities.

## 8. Transaction Design

-   **`@Transactional` boundary:** Put `@Transactional` on public Service methods, NOT controllers.
-   **Atomic Execution:** A single HTTP request equals a single transaction.
-   **Propagation:** Default propagation (`REQUIRED`).
-   **Rollback:** Ensure it rolls back on all RuntimeExceptions, which is the Spring default.

## 9. Error Response Model

We must use a standard structured error response (RFC 7807 Problem Detail format is recommended).

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2026-04-12T10:00:00Z",
  "fieldErrors": [
    {
      "field": "code",
      "message": "Company code must be unique."
    },
    {
      "field": "geofenceRadiusMeters",
      "message": "Radius is required when shape is CIRCLE."
    }
  ]
}
```

For Optimistic Locking:
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Data was updated by another user. Please refresh and try again.",
  "timestamp": "2026-04-12T10:00:00Z",
  "errorCode": "OPTIMISTIC_LOCK_CONFLICT"
}
```

## 10. Repository Methods Needed

**CompanySiteRepository**
```java
// Core fetch with auth boundary
Optional<CompanySite> findByIdAndCompanyId(UUID id, UUID companyId);

// Check uniqueness for update
boolean existsByCompanyIdAndCodeIgnoreCaseAndIdNot(UUID companyId, String code, UUID id);
```

**SiteTrustedNetworkRepository**
```java
// Fetch with auth routing through Site
@Query("SELECT n FROM SiteTrustedNetwork n JOIN n.site s WHERE n.id = :networkId AND s.company.id = :companyId")
Optional<SiteTrustedNetwork> findByIdAndCompanyId(@Param("networkId") UUID networkId, @Param("companyId") UUID companyId);

// Check duplicate rules
boolean existsBySiteIdAndCidrBlockAndNetworkTypeAndIdNot(UUID siteId, String cidrBlock, NetworkType networkType, UUID networkId);

// Fetch all rules for a site to recalculate priorities
List<SiteTrustedNetwork> findAllBySiteIdOrderByPriorityOrderAsc(UUID siteId); // Or just PriorityOrderDesc
```

## 11. Test Plan

1.  **Concurrency Test (Optimistic Locking):** 
    - Fetch a site entity twice. Modify and save thread A. Then attempt to save thread B using the old version. Verify `409 Conflict` is returned.
2.  **Validation Test:**
    - Send a Location Update with `locationRequired = true` but omitting `geofenceRadiusMeters`. Assert a `400 Bad Request` with field-specific errors.
3.  **Boundary Test (Security):**
    - Authenticate user from Company A. Attempt to update site belonging to Company B. Assert `404 Not Found` or `403 Forbidden` (since `findByIdAndCompanyId` will return empty).
4.  **Network Logic derivation:**
    - Provide IPv4 and IPv6 CIDRs to Network Update endpoint. Verify backend correctly derives `NetworkIpVersion.IPv4` or `IPv6` respectively. 
    - Verify client cannot override Priority Order. Add two rules; they should receive Priority `1` and `2` automatically in DB.

## 12. Sample Controller code skeleton

```java
@RestController
@RequestMapping("/api/v1/company-sites/{siteId}")
@RequiredArgsConstructor
public class CompanySiteUpdateController {

    private final CompanySiteUpdateService updateService;
    private final SecurityContextHolder authContext;

    @PutMapping("/details")
    public ResponseEntity<CompanySiteDetailsDto> updateDetails(
            @PathVariable UUID siteId,
            @Valid @RequestBody CompanySiteDetailsUpdateRequest request) {
        
        UUID companyId = authContext.getCurrentCompanyId();
        var updatedSite = updateService.updateMainDetails(companyId, siteId, request);
        return ResponseEntity.ok(updatedSite);
    }

    @PutMapping("/location")
    public ResponseEntity<CompanySiteDetailsDto> updateLocation(
            @PathVariable UUID siteId,
            @Valid @RequestBody CompanySiteLocationUpdateRequest request) {
        
        UUID companyId = authContext.getCurrentCompanyId();
        var updatedSite = updateService.updateLocation(companyId, siteId, request);
        return ResponseEntity.ok(updatedSite);
    }
}
```

## 13. Enum Adjustments Needed

-   None are strictly required for the *update* flow itself, but if we need a state transition out of `PENDING_REVIEW` when a user updates critical details, we may need to introduce logic to flip `SiteStatus` (e.g., from `PENDING_REVIEW` to `ACTIVE` if location validation passes, or `DRAFT` if validation fails). However, the prompt implies strict validation applies during updates. So, any update endpoint will simply force the entity to remain in or move to `ACTIVE` only if strict validation holds.  We will enforce that `status` must not regress back to Draft equivalents unknowingly. 

> [!IMPORTANT]
> - Do not include `status` in the update payload. Backend should manage state transitions automatically through a business layer state machine based on the completeness of data.
> - Trust no input from the client except raw data.

## Review Requested
Take a look at the strategy above—especially the decision to use a Unified Single Read alongside Granular Write endpoints. Let me know if you would like me to proceed with implementation or make any adjustments!
