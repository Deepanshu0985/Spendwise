# Open Decisions

Decisions that are not yet made, and the phases they block. An undecided item that is not written down becomes an accidental default.

| # | Decision | Blocks | Status |
|---|---|---|---|
| D-01 | Which bank and card PDF formats ship in MVP | Phase 6 | Decided: ADR-014, HDFC Bank, SBI and Axis Bank |
| D-02 | Default category taxonomy: names, hierarchy, system set | Phase 2 | Decided: ADR-015, flat taxonomy |
| D-03 | LLM provider and model | Phase 11 | Open |
| D-04 | Email provider for password reset and verification | Phase 1 | Decided: ADR-016, Resend |
| D-05 | Hosting provider, instance size and region | Phase 0 | Decided: ADR-017, DigitalOcean droplet, Bangalore (BLR1) |
| D-06 | Are raw statement PDFs retained after successful import | Phase 6, privacy | Open |
| D-07 | Retention periods: staging rows, audit logs, AI conversations, deleted-user data | Privacy | Open |
| D-08 | Is AI chat history persisted, or stateless per request | Phase 12 | Open |
| D-09 | AI cost caps: per-user daily quota and global monthly ceiling | Phase 11 | Open |
| D-10 | Backup RPO and RTO targets | Phase 0 | Open |
| D-11 | Web-only or PWA for V1 | Frontend scope | Open |
| D-12 | Financial-health score methodology | Post-MVP | Deferred |
| D-13 | Auth mechanism | — | Decided: ADR-009, cookie sessions |
| D-14 | Tenant isolation mechanism | — | Decided: ADR-010, RLS plus app layer |
| D-15 | Currency handling | — | Decided: ADR-011, store multi, aggregate single |
| D-16 | `INTEREST` and `FEE` direction | — | Decided: ADR-012, explicit ledger-side types |
| D-17 | Transaction splits | — | Decided: ADR-013, schema now, UI later |
| D-18 | Refund treatment | — | Decided: reduces expenses |

## Gating Decisions

Four items gated the start of work: **D-05** (Phase 0 cannot deploy without it), **D-02** (transactions need categories), **D-04** (login is incomplete without recovery) and **D-01** (parser scope). All four are now decided (ADR-014 through ADR-017); Phase 0 work can begin. The rest can ride until their phase.

**D-06** is worth deciding early for a different reason: retained PDFs are simultaneously the largest storage cost and the largest breach liability. Deleting the file after successful import and keeping only the parsed rows is the cheap, defensible answer, and it makes D-07 substantially simpler.

## Process

When a decision is made, record it as an ADR in `02-architecture/architecture-decisions.md`, change the status here to `Decided` with the ADR reference, and update any document the decision touches in the same commit.
