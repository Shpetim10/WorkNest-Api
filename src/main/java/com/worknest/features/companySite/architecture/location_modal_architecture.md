# Company Site Location Modals Architecture

This document details the backend architectural design strictly concerning the **Location** modal updates and detection mechanisms for an existing `CompanySite`. 

This enforces exact boundaries decoupling the geofence logic, validation, and location data processing from other functional sections without muddying other domains.

---

## 1. Endpoints

### Read Endpoint
**`GET /api/v1/companies/{companyId}/sites/{siteId}/location`**
Designed for the most performant modal load initialization possible without exposing details outside the Location scope.
* **Response:** `200 OK` with `LocationDetailsReadDto`.

### Update Endpoint
**`PUT /api/v1/companies/{companyId}/sites/{siteId}/location`**
Substitutes an entirely new location state enforcing normalization over the old layout.
* **Request:** `LocationDetailsUpdateRequest`
* **Response:** `200 OK` with `LocationDetailsReadDto`

### Existing-Site Location Assessment (Helper Endpoint)
**`POST /api/v1/companies/{companyId}/sites/{siteId}/detect-location`**
Assesses current browser coordinates iteratively provided against the current site configuration. Does **NOT** mutate database.
* **Request:** `DetectLocationRequest` (browser `latitude`, `longitude`, `accuracyMeters`)
* **Response:** `DetectLocationResponse` (warnings, inside/outside geofence flag, normalized suggestions matching the site format)

---

## 2. DTO Contracts

### Location Details Prefill (Read)
```java
public record LocationDetailsReadDto(
    UUID id,
    String addressLine1, String addressLine2, String city, String stateRegion,
    String postalCode, String countryCode, String timezone,
    BigDecimal latitude, BigDecimal longitude,
    GeofenceShapeType geofenceShapeType,
    Integer geofenceRadiusMeters,
    String geofencePolygonGeoJson,
    Integer entryBufferMeters, Integer exitBufferMeters,
    Integer maxLocationAccuracyMeters,
    Boolean locationRequired,
    Long version
) {}
```


### Update Request
```java
public record LocationDetailsUpdateRequest(
    @Size(max=255) String addressLine1,
    @Size(max=255) String addressLine2,
    @Size(max=100) String city,
    @Size(max=100) String stateRegion,
    @Size(max=30) String postalCode,
    
    @NotBlank(message = "Country code is required.")
    @Size(min=2, max=2) String countryCode,
    
    @NotBlank(message = "Timezone is required.")
    @Size(max=100) String timezone,

    // Ranges strictly capped for coordinates
    @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,

    GeofenceShapeType geofenceShapeType,
    @Min(10) @Max(50000) Integer geofenceRadiusMeters,
    String geofencePolygonGeoJson,

    @Min(0) @Max(1000) Integer entryBufferMeters,
    @Min(0) @Max(1000) Integer exitBufferMeters,
    @Min(1) @Max(5000) Integer maxLocationAccuracyMeters,

    @NotNull Boolean locationRequired,
    @NotNull Long version
) {}
```


### Assessment Assessment Payload
```java
// What the UI sends from browser navigator API
public record DetectLocationRequest(
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
    Integer accuracyMeters // Optional from the client
) {}

// What the server returns to help the UI
public record DetectLocationResponse(
    boolean isWithinGeofence,
    Double distanceMeters, // Distance from center/polygon edge
    List<String> warnings, // E.g., "Accuracy too poor", "Outside geofence"
    String recommendedTimezone // Resolved dynamically based on given lat/long
) {}
```

---

## 3. Strict Validation & Normalization Rules

This process is handled entirely within the `CompanySiteUpdateService`. No logic is shifted purely to database hooks anymore.

### Normalization Logic Loop
**Step 1:** If `locationRequired == false`:
   - Force **CLEAR** `geofenceShapeType`, `latitude`, `longitude`, `geofenceRadiusMeters`, `geofencePolygonGeoJson`, entry/exit buffers. Server overrides anything client passed.  
   - Skip to Step 4 (saving).

**Step 2:** If `locationRequired == true`, enforce presence:
   - `geofenceShapeType` must not be null. If it is, throw Validation Exception.
   - If `geofenceShapeType == CIRCLE`:
     - Assert `latitude`, `longitude`, `geofenceRadiusMeters` exist.
     - **Normalize**: Clear `geofencePolygonGeoJson = null`.
   - If `geofenceShapeType == POLYGON`:
     - Assert `geofencePolygonGeoJson` exists. (Bonus: Try to parse JSON to ensure valid structure).
     - **Normalize**: Clear `latitude`, `longitude`, `geofenceRadiusMeters` to null.

**Step 3:** Buffers and Accuracy Constraints:
   - If `maxLocationAccuracyMeters` is provided, ensure it's not absurdly restrictive (< `5` meters usually fails on mobile devices). Throw soft warning or reject.

**Step 4:** Pass and map.

---

## 4. Conflict Handling & Security Context

* **Transaction boundary:** `@Transactional` around `updateLocation` service.  
* **Optimistic locking:** Throw `StaleSiteDataException` exactly as designed on the Main Details endpoint if versions diverge.  
* **Tenant checking:** Fetch with `findByIdAndCompanyId`. 

---

## 5. Implementation Code Skeleton 

**Controller Details:**
```java
@RestController
@RequestMapping("/api/v1/companies/{companyId}/sites/{siteId}")
@RequiredArgsConstructor
public class CompanySiteLocationController {

    private final CompanySiteLocationService locationService;

    @GetMapping("/location")
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN')")
    public ApiResponse<LocationDetailsReadDto> getSiteLocation(...) {
        // delegates to service
    }

    @PutMapping("/location")
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN')")
    public ApiResponse<LocationDetailsReadDto> updateSiteLocation(...) {
        // delegates to service
    }

    @PostMapping("/detect-location")
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN')")
    public ApiResponse<DetectLocationResponse> detectLocation(
             @PathVariable UUID companyId,
             @PathVariable UUID siteId,
             @Valid @RequestBody DetectLocationRequest request) {
        // Evaluates against persisted entity state
        return ApiResponse.success("Geofence collision assessed", locationService.assessLocation(companyId, siteId, request));
    }
}
```

**Service Structure:**
```java
@Service
@Transactional
@RequiredArgsConstructor
public class CompanySiteLocationServiceImpl implements CompanySiteLocationService {

    private final CompanySiteRepository repository;

    @Override
    public LocationDetailsReadDto updateLocation(UUID companyId, UUID siteId, LocationDetailsUpdateRequest request) {
        // Fetch
        CompanySite site = repository.findByIdAndCompanyId(siteId, companyId).orElseThrow(...);

        // Lock validation
        if (!site.getVersion().equals(request.version())) { throw new StaleSiteDataException(); }

        // Timezone/Country validation
        validateTimezone(request.timezone());
        
        // Strict Business Constraints validation
        if (!request.locationRequired()) {
            clearGeofenceData(site);
        } else {
            validateAndNormalizeActiveGeofenceBounds(site, request);
        }
        
        // standard DTO setters 
        site.setAddressLine1(request.addressLine1());
        // ... (etc.) ...
        
        return mapToDto(repository.save(site));
    }

    // Other service logic...
}
```

---

## 6. Test Plan

**Scenario 1: True Normalization Override**
* **Input:** Submit a location update with `locationRequired: false`, but illegally include `geofenceRadiusMeters: 50`.
* **Assertion:** Confirm HTTP returns `200 OK`, but fetching the site proves the database saved `geofenceRadiusMeters: null`.

**Scenario 2: Boundary/Constraint Rejection**
* **Input:** Submit `locationRequired: true`, `geofenceShapeType: CIRCLE`, but intentionally omit `latitude`.
* **Assertion:** Confirm HTTP returns `400 Bad Request` citing "Latitude is required for circular geofences."

**Scenario 3: Polygon Clears Circle Parameters**
* **Input:** Submit `locationRequired: true`, `geofenceShapeType: POLYGON`, providing valid JSON polygon string, but also provide `geofenceRadiusMeters`.
* **Assertion:** Confirm HTTP 200 OK, but ensure fetched entity has `latitude` & `radius` strictly nulled out. 

**Scenario 4: Valid Detection Feedback**
* **Input:** Execute `POST /detect-location` using a coordinate `200` meters away from the site's center when radius is `50` meters.
* **Assertion:** Ensure returning JSON validates `isWithinGeofence: false`, indicates ~`200` meters of margin distance, and issues a "Far outside perimeter" warning list.
