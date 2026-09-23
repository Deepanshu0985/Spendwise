# AI Architecture

AI is an orchestration/explanation layer, not the financial source of truth.

```mermaid
sequenceDiagram
    participant U as User
    participant A as Assistant
    participant L as LLM
    participant T as Finance Tool
    participant S as Finance Service
    participant D as PostgreSQL

    U->>A: Spending question
    A->>L: Interpret intent
    L-->>A: Approved tool call
    A->>T: Tool input
    T->>S: Verified query
    S->>D: Read confirmed transactions
    D-->>S: Deterministic metrics
    S-->>T: Typed result
    T-->>A: Verified data
    A->>L: Explain result
    L-->>A: Grounded answer
    A-->>U: Answer
```

## AI Responsibilities
Intent understanding, categorization suggestions, merchant normalization, explanations and insight generation.

## Backend Responsibilities
Arithmetic, authorization, persistence, transaction state, budgets, duplicate decisions and source-of-truth analytics.

## Failure
If AI is down, manual tracking and deterministic analytics continue.
