# Coding Standards

## Design Principles

SOLID is mandatory, not aspirational, and is checked in review alongside correctness and authorization.

- **Program to an interface, never a concrete implementation.** Every service that a controller or another service depends on is declared as an interface, with the implementation in a separate class (e.g. `AccountService` / `AccountServiceImpl`). Constructor injection wires the interface type, never the concrete class. This is already the pattern `lld.md` uses for infrastructure ports (`StatementParser`, `TransactionCategorizer`, `FinanceTool`, `AIModelClient`, `SessionStore`, `TenantContext`); it extends to every application-service and domain-service class, not just those named ports.
- **Single Responsibility.** A service class orchestrates one use case area (per LLD module boundary). If a class accumulates unrelated reasons to change, split it.
- **Open/Closed.** New statement formats, AI tools, or categorization rules extend an existing interface (`StatementParser`, `FinanceTool`) rather than adding conditionals to existing implementations.
- **Liskov Substitution.** Any implementation of an interface must be usable anywhere the interface is expected, with no caller needing to know which implementation it received. A `FinanceTool` implementation, for example, never assumes it's the only one registered.
- **Interface Segregation.** Prefer several small, focused interfaces over one large one a caller only partially needs — e.g. read-only finance tools should not share an interface with anything that writes.
- **Dependency Inversion.** High-level modules (services) depend on abstractions (interfaces) that low-level modules (repositories, infrastructure adapters) implement, never the reverse. Spring's `@Repository` interfaces already satisfy this for persistence; apply the same shape to services and infrastructure adapters (`infrastructure/pdf`, `infrastructure/ocr`, `infrastructure/ai`, `infrastructure/storage`).

Practical consequence for this codebase: a new feature always means at least an interface plus an implementation, wired by Spring through constructor injection. This is deliberately more files than a quick concrete-class-only approach, and that tradeoff is accepted here for testability (mock the interface) and for the AI/statement-processing modules where multiple implementations (per-provider, per-bank-format) are expected from the roadmap itself.

### Layering: domain / application / infrastructure

Every backend feature is organized by architectural layer, not left flat in a single `com.finance.<feature>` package. Four package roots, per feature:

- **`com.finance.domain.<feature>`** — the business entity: plain Java (fields + behavior methods like `rename()`/`update()`), no JPA or Spring annotations. Framework-free enums live here too. Also the **repository port**: an interface in domain types only, never a Spring Data supertype (`JpaRepository`, `JpaSpecificationExecutor`).
- **`com.finance.application.<feature>`** — the use-case layer: `<Feature>Service` interface + `<Feature>ServiceImpl`, depending only on the domain repository port. Owns **command objects** (e.g. `CreateCategoryCommand`) — the application's own input shape, distinct from the HTTP request DTO, so this layer never depends on `infrastructure.web`.
- **`com.finance.infrastructure.persistence.<feature>`** — the JPA adapter: `<Feature>JpaEntity` (the actual `@Entity`), `<Feature>JpaRepository` (Spring Data interface), a `<Feature>Mapper` (domain ↔ JPA entity, a plain static utility — no interface, same reasoning as `SessionCookieFactory`), and `<Feature>RepositoryImpl` (implements the domain port).
- **`com.finance.infrastructure.web.<feature>`** — the REST adapter: `<Feature>Controller` (maps request DTO → command, calls the service, maps the result → response DTO) and the `Create/UpdateXRequest`/`XResponse` records.

Pragmatic exceptions, decided rather than accidental:
- A pure, side-effect-free business-rule interface+impl with no persistence/web I/O (e.g. `MerchantNormalizer`) lives entirely in `domain.<feature>` — it doesn't need an infrastructure adapter just to be Spring-registered.
- Purely technical, non-domain concerns don't get the four-way split: the auth mechanism (`Session`, `PasswordResetToken`, token hashing/generation, cookies) lives as a unit in `infrastructure.security`; idempotency bookkeeping lives as a unit in `infrastructure.idempotency`. `User` itself, being a genuine domain concept, still gets the full split.
- The `ApiException` hierarchy and `ApiError`/`ErrorCode` live in `application.exception`, since application/domain code throws them — keeping them out of `infrastructure` preserves the dependency direction (infra depends on application, never the reverse). `GlobalExceptionHandler` (the HTTP translation of those exceptions) is a genuine web-adapter concern and stays in `infrastructure.web.common`.
- `TenantContext`/`CurrentUserGuard`/`ApiResponse`/`PageMeta` and other generic request-scoped plumbing live in `infrastructure.tenancy` / `infrastructure.web.common` — controllers resolve `UUID userId` via `CurrentUserGuard` before calling any service, so application services only ever take a plain `UUID`, never a `TenantContext`.
- `Pageable`/`Page` (Spring Data Commons, not JPA-specific) are an accepted exception in domain repository port signatures, rather than inventing a parallel pagination abstraction for no real benefit at this project's scale.

See `DECISIONS.md` ("Retrofit backend to domain/application/infrastructure layering") for the full worked example and rationale.

## Java/Spring
Constructor injection, thin controllers, DTO boundaries, service-layer business orchestration, focused repositories, explicit enums/types and validation at boundaries.

## Naming
Classes: PascalCase. Methods/variables: camelCase. Constants: UPPER_SNAKE_CASE. Database names: snake_case.

## Errors
Use domain exceptions mapped to the API error contract. Never silently swallow errors.

## Transactions
Use DB transactions for atomic business operations. Do not hold long DB transactions during PDF/OCR/AI work.

## Frontend
TypeScript strict mode, reusable components, API client layer and no duplicated financial calculation logic in the UI.

## Git
```text
feature/<name>
fix/<name>
chore/<name>
```

Commit examples:
```text
feat: add statement review
fix: prevent duplicate import
test: add PDF parser fixture
```

## Review
Check correctness, authorization, financial calculations, error handling, tests, maintainability and observability.
