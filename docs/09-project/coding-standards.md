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
