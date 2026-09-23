# Personal Finance Intelligence App — Documentation Package

Implementation documentation for the personal finance application.

Technology direction: React + TypeScript, Java + Spring Boot, PostgreSQL, PDF/OCR processing and tool-based AI.

The architecture starts as a modular monolith. Canonical confirmed transactions are the source of truth, and AI is grounded through allowlisted backend finance tools.

## Where to start

- `09-project/development-roadmap.md` — implementation order and phase gates.
- `09-project/open-decisions.md` — what is still undecided and what it blocks. Read this before starting a phase.
- `02-architecture/architecture-decisions.md` — the thirteen ADRs that constrain everything else.
- `02-architecture/analytics-specification.md` — the authoritative definition of every number the product displays.

## Structure

| Folder | Contents |
|---|---|
| `01-product` | PRD, functional requirements, user stories |
| `02-architecture` | ADRs, HLD, LLD, database design, analytics specification |
| `03-api` | API specification, authentication, error contract |
| `04-ai` | AI architecture, tools, prompt design, evaluation |
| `05-statement-processing` | PDF processing, normalization, duplicate detection |
| `06-frontend` | Design system, screens, UI/UX |
| `07-quality` | Security, testing, privacy and retention, edge cases |
| `08-devops` | Deployment, environments, observability |
| `09-project` | Roadmap, open decisions, coding standards, definition of done |
