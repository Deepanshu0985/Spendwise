# Duplicate Detection

## Signals
Same account, date, amount, external/reference ID, normalized merchant/description and nearby posting dates.

```mermaid
flowchart TD
    A[Staged Row] --> B{Exact Reference?}
    B -->|Yes| C[Duplicate]
    B -->|No| D{Date + Amount + Merchant?}
    D -->|Yes| C
    D -->|No| E{Similarity}
    E -->|High| C
    E -->|Medium| F[Review]
    E -->|Low| G[Import Candidate]
```

## Scoring
Exact reference + account is strongest. Date + amount + merchant is high confidence. Nearby-date similarity is medium evidence.

## Overlapping Statements
A September 1–30 upload and September 15–October 15 upload must not duplicate the overlapping canonical events.

## Override
Users can review/override flags where the application permits it, with audit metadata for material overrides.
