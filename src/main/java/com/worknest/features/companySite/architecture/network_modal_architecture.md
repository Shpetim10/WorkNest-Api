# Company Site Trusted Network Modal Architecture

This document defines the highly decoupled, production-ready backend architecture for the **Trusted Network Modal**. It emphasizes server-side computation of technical properties (IP Version, Priority), removing reliance on potentially compromised or incorrect client hints.

---

## 1. Network Endpoints List

By scoping all endpoints tightly under the site hierarchy (`/api/v1/companies/{companyId}/sites/{siteId}/networks`), we guarantee tenant isolation automatically.

### CRUD & Management
* **`GET    .../networks`** — Returns a list of all current networks for the site. Used to instantly populate the Network Modal data grid.
* **`POST   .../networks`** — Creates a new trusted network. Response includes server-computed fields.
* **`PUT    .../networks/{networkId}`** — Updates an existing rule (name, CIDR, notes, expiration). Requires `version`. 
* **`PATCH  .../networks/{networkId}/status`** — Toggles `isActive`. Fast action, separate from heavy updates. Requires `version`.
* **`DELETE .../networks/{networkId}`** — Permanently removes a rule.

### Detection Assistant
* **`POST   .../networks/detect`** — Analyzes the incoming HTTP request context to return an advisory suggestion (IP, /32 CIDR, version). Never saves anything.

---

## 2. Server-Side Derivation & Validation Rules

*   **Deriving `ipVersion`**: The client never sends `ipVersion`. The backend `CidrValidator` must parse the provided CIDR block. If it contains `:`, it returns `IPv6`. If it contains `.`, it returns `IPv4`.
*   **Computing `priorityOrder`**: The client never sends priority. When creating a new rule, the service must execute `countByCompanyIdAndSiteId` and assign `count + 1`. If we need reordering later, introduce a separate `POST .../networks/reorder` endpoint using arrays of IDs.
*   **CIDR Normalization**: The backend must strip empty spaces and enforce standard masking (e.g., lowercase IPv6 representations).
*   **Unique Database Integrity**: Ensure `(siteId, normalizedCidrBlock, networkType)` are strictly unique through `boolean existsBy...AndIdNot`.
*   **Expiration Guarding**: If the client provides an `expiresAt` timestamp, the service must verify it is `> Instant.now()`. Inherently expired rules cannot be created/saved.

---

## 3. Data Transfer Objects (DTOs)

### Response Model
```java
public record TrustedNetworkResponseDto(
    UUID id,
    String name,
    NetworkType networkType,
    String cidrBlock,
    NetworkIpVersion ipVersion,      // Derived by server
    Boolean isActive,
    Integer priorityOrder,           // Derived by server
    String notes,
    Instant expiresAt,
    Long version
) {}
```

### Mutating Models (Client → Server)
Notice the structural omission of `ipVersion` and `priorityOrder`.

```java
public record CreateNetworkRequest(
    @NotBlank @Size(max=100) String name,
    @NotNull NetworkType networkType,
    @NotBlank @Size(max=100) String cidrBlock,
    String notes,
    @Future(message = "Expiration time must be in the future") Instant expiresAt
) {}

public record UpdateNetworkRequest(
    @NotBlank @Size(max=100) String name,
    @NotNull NetworkType networkType,
    @NotBlank @Size(max=100) String cidrBlock,
    String notes,
    @Future Instant expiresAt,
    @NotNull Long version  // Optimistic lock
) {}

public record ToggleNetworkStatusRequest(
    @NotNull Boolean isActive,
    @NotNull Long version
) {}
```

### Detection Helper
```java
// What the server returns to advise the client UI
public record DetectNetworkResponse(
    String observedIp,        // e.g. "192.168.1.15" or "2001:db8::1"
    String normalizedIp,      
    String suggestedCidr,     // Enforced as /32 for IPv4, or /128 for IPv6
    NetworkIpVersion ipVersion,
    NetworkType suggestedNetworkType, // Default to OFFICE
    Integer estimatedPriorityOrder,
    List<String> warnings,
    List<String> metadataHints // E.g., "Matched ISP: Cloudflare", "Possible proxy detected"
) {}
```

---

## 4. Service Architecture & Operations

### `SiteTrustedNetworkService`
| Method | Responsibility |
| --- | --- |
| `listNetworks(companyId, siteId)` | Fast aggregate fetch ordered by `priorityOrder ASC`. |
| `createNetwork(companyId, siteId, req)` | Generates priority, resolves IP version, verifies duplicate bounds, saves entity. |
| `updateNetwork(companyId, siteId, netId, req)` | Verifies optimistic lock (`version`). Assesses uniqueness against self. Resolves IP version if CIDR changed. |
| `toggleStatus(companyId, siteId, netId, req)` | Verifies optimistic lock. Simply flips `isActive`. |
| `deleteNetwork(companyId, siteId, netId)` | Removes the entity. Recalculate priority order immediately across the remaining networks if gaps arise. |
| `detectNetworkSuggestion(companyId, siteId, HttpRequest)` | Extracts the raw HTTP request actor IP. Runs normalization. Maps `DetectNetworkResponse`. |

---

## 5. Enum Recommendations

### `NetworkType`
We need classifications that provide distinct behavioral differences or auditing contexts:
*   `CORPORATE_OFFICE` - Physical primary location networks.
*   `VPN` - Known remote access gateways.
*   `GUEST` - Unsecured or decoupled physical nodes.
*   `DATA_CENTER` - Authorized server-to-server CIDRs.

### `NetworkIpVersion`
Must be incredibly basic and rigidly typed to prevent enum parse failures.
*   `IPv4`
*   `IPv6`

---

## 6. Concurrency Handling & Testing

* **Update & Toggle Conflicts**: Both `PUT` and `PATCH` actions strictly enforce `if (!entity.getVersion().equals(req.version())) throw StaleDataConflictException`. Converts to `409 Conflict`.
* **Delete Synchronization**: `DELETE` does not strictly require a version check because deletion of stale data is usually acceptable. However, to guarantee total safety alongside `toggleStatus`, you can add `Long version` to the `DELETE` query string param `@RequestParam("version") Long version` and execute a version assertion before repository purge.
* **Test Plan**:
   1. Attempt to create conflicting CIDR + Types. Ensure `409 Conflict`.
   2. Pass `10.0.0.1/32` into Create request. Assert that the DB captures `ipVersion = IPv4` cleanly without client supplying it.
   3. Update a rule applying a past expiration time. Assert schema rejects via `400 Bad Request`.
   4. Extract test proxy headers (`X-Forwarded-For`). Assert `detect` returns the valid origin IP, correctly masked to a `/32`.

---
**Does this fully encompass all functional boundaries required to build and connect the modal?**
