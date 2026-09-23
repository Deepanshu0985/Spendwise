# Data Privacy & Retention

## Data Categories
Profile and account data, transactions, uploaded statements, analytics output, AI insights, AI conversations and audit metadata.

## Principles
Collect only what is needed. Protect financial documents. Minimize third-party AI exposure. Provide export. Make deletion real and understandable.

## AI Minimization
Send normalized merchant, transaction type, candidate category and a bounded description. Never send credentials, account numbers, full statement text or whole PDFs when structured data is sufficient. All external model calls pass through the single `AIModelClient` chokepoint, which performs redaction and records what shape of data was sent.

## Retention
Explicit policies are required for raw statement files, staging rows, audit logs, AI conversations and deleted-user data. These are tracked as **D-06** and **D-07** in `09-project/open-decisions.md`.

Deleting the raw PDF after a successful import, while keeping the parsed rows, is the recommended default: retained statements are simultaneously the largest storage cost and the largest breach liability.

## Deletion
Transaction records are soft-deleted and retained, so history remains reconstructible. Account deletion is different: it removes or irreversibly anonymizes the user's data across every table and purges their statement files from object storage, and must be demonstrable rather than merely claimed.

## User Controls
Export, account deactivation, deletion request, session revocation, AI preferences and privacy information.

## Legal
Holding other people's financial data in India brings the Digital Personal Data Protection Act into scope, including breach notification obligations. A privacy policy, a working deletion path and a written incident response plan are gating requirements for the public beta, not post-launch work. This document is engineering guidance and not legal advice; the policy itself should be reviewed by someone qualified before strangers use the product.
