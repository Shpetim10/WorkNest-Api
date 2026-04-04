# WorkNest API Architecture

## Package Structure

The project follows a **feature-first** modular structure. Business capabilities are encapsulated within feature slices, while cross-cutting concerns and shared domain models are centralized.

### 1. Domain Layer (`com.worknest.domain`)
Shared business models and types that are used across multiple features.
- **`entities`**: All JPA `@Entity` classes (e.g., `User`, `Company`).
- **`enums`**: All business-related enumerations (e.g., `UserStatus`, `PlatformRole`).

### 2. Feature Layer (`com.worknest.features.<feature_name>`)
Encapsulates business capabilities. Each feature owns its business logic and API surface.
- **`web`**: REST Controllers and API entry points.
- **`application`**: Services that implement business use cases and orchestrate workflows.
- **`repository`**: JPA repositories owned by the feature.
- **`dto`**: Data Transfer Objects for API requests and responses.
- **`exception`**: Feature-specific exceptions.

### 3. Core & Infrastructure
- **`common`**: Shared utilities, global error handling, and agnostic wrappers.
- **`security`**: Security configuration, JWT handling, and access control.
- **`tenant`**: Multi-tenancy context and resolution logic.
- **`audit`**: Platform-wide audit logging.

## Architectural Rules

1. **Dependency Direction**: Features may depend on `domain`, `common`, and `security`. Features should **not** depend on the internal implementation of other features.
2. **Feature Communication**: If Feature A needs functionality from Feature B, it should use a well-defined application service interface, not a direct controller or DTO reach-through.
3. **Layering**: Controllers must only call application services. Services interact with repositories and domain entities.
4. **No ServiceImpl Sprawl**: Prefer a single concrete service class unless multiple implementations are truly required by the business logic.
