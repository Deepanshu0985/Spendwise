# RAG Plan (Phase 13)

Status: planned, not started. The Phase 12 assistant stays as it is until this phase.

## The problem

The Phase 12 assistant has seven fixed tools. Anything outside them gets "I can't answer that". People ask about records, not only totals:

- "What was that payment to Sharma in March?"
- "Show me all my Uber rides this year."
- "Did I pay the electricity bill in June?"
- "What did I spend at coffee shops?" (the data says *Cafe Coffee Day*, *Starbucks*, *Third Wave*)

## Principle

**Retrieval finds records. Code does the arithmetic. The model only words the result.** Everything Phase 12 established still holds: identity comes from the session and never from an argument, tools are read-only, retrieved text is untrusted data, and every figure in an answer must be one the system returned.

## Two kinds of retrieval, built in this order

### 1. Structured retrieval (no embeddings, build first)
Most "ask anything" questions are really filters over the user's own rows. Adding a few tools covers a large share of them, with exact results and no new infrastructure:

| Tool | Purpose |
|---|---|
| `search_transactions` | Filters: date range, amount range, free text (description, merchant), category, merchant, type, account. Returns at most 20 rows with a stable result number each. |
| `aggregate` | Total and count, grouped by category, merchant, month or account, with the same filters. Done in SQL, never by the model. |
| `compare_periods` | Two periods side by side with the difference and percentage computed in code, so the model quotes them instead of calculating. |
| `get_transaction` | One record in full, by result number. |

Free text uses PostgreSQL full-text search plus trigram similarity, so "sharma" finds "UPI-SHARMA TRADERS" and a typo still matches.

### 2. Semantic retrieval (embeddings)
Structured search cannot know that "coffee" means *Cafe Coffee Day*. For that:

- Embed each transaction's cleaned, redacted text (description, merchant, category) and store the vector.
- Add `semantic_search(query, optional filters)` returning the nearest 20 rows.
- Always combine with the structured filters (dates, amounts), so "coffee last month" is a vector match inside a date filter.

Measure before building part 2: if structured plus text search answers the golden questions well, semantic retrieval may only need to cover the remainder.

## Design decisions to make

| ID | Decision | Recommendation |
|---|---|---|
| D-20 | Embedding model and provider | Mistral's embedding model through the existing `AiModelClient` gateway (a new `embed` method), so there is still exactly one place data leaves the application. Confirm current price and dimension. |
| D-21 | What gets indexed | Confirmed transactions only (description, merchant, category, type). Not raw statement text, not staged rows, not account numbers. |
| D-22 | Vector store | `pgvector` in the existing PostgreSQL database, in a table with `user_id`, row-level security and a foreign key to the transaction, so a deleted transaction or user takes its vector with it. A separate vector database adds a system and a tenant-isolation problem for no gain at this scale. |
| D-23 | Spend caps for embeddings | A third usage kind alongside categorisation rows and chat messages: embedding rows per user per day and in total per month, reserved atomically like the others. Backfill runs in capped batches. |

## Pieces

1. **Schema.** `CREATE EXTENSION vector` (Neon supports it; the local and CI PostgreSQL images must switch to a pgvector build). `transaction_embeddings(transaction_id, user_id, embedding, text_hash, model_version)` with row-level security and an HNSW index. `text_hash` makes re-embedding skip unchanged text; `model_version` allows a clean re-index when the model changes.
2. **Indexing.** Embed when a transaction is confirmed or edited, in the background and in small capped batches; a backfill job for existing rows. Text goes through the same redaction as categorisation before it is sent.
3. **Retrieval tools** as above, behind the same `AssistantTool` contract: user id from the session, validated bounded arguments, read-only.
4. **Cited answers.** Retrieved rows get numbered results; the model refers to a number, the server maps it to the real record (the model never sees or returns record ids). The panel shows the rows an answer relied on, each linking to its transaction.
5. **Grounding gate, extended.** A figure is grounded if a tool returned it, including amounts shown verbatim in retrieved rows. A total, average, difference or percentage is grounded only if `aggregate` or `compare_periods` returned it. The one-rewrite-then-plain-listing behaviour stays.
6. **Prompt-injection surface.** Retrieved descriptions are free text from statements and UPI notes, so the delimiter, the instruction-like-name placeholder and the read-only tools matter more here, and the golden set gets more adversarial rows.
7. **Privacy and retention.** Embeddings are derived financial data and are sent, redacted, to the provider. They are deleted with the transaction and the user (relevant to D-07 and the deletion path in Phase 15).

## Risks

| Risk | Mitigation |
|---|---|
| Cross-tenant retrieval | Row-level security on the vector table plus an explicit user filter; a test that user A's query can never return user B's row, even with an identical description. |
| Wrong or missing retrieval presented confidently | Show the sources; say when nothing matched; prefer "I found none" to guessing. |
| Cost growth | Embedding caps (D-23), `text_hash` skipping unchanged rows, top-k and text length limits. |
| Stale index | Index on confirm and edit; a scheduled sweep for rows whose hash changed. |
| Model drift on re-index | `model_version` column and a re-index path. |

## Evaluation

A golden dataset, anonymised and synthetic: about 60 questions across lookup, filtered totals, comparisons, fuzzy matching and refusals, plus injection rows, each with the expected records or figures. Report recall@20 for retrieval, answer accuracy, the share of answers that fall back to a plain listing, latency and tokens per answer. Run on every prompt, model or index change. The bar for the exit gate is set from the first baseline, then held.

## Out of scope for Phase 13

Writing or changing data from chat, saved conversation history (needs the D-07 retention decision), and indexing documents other than transactions.

## Exit gate

The Phase 12 grounding tests still pass with retrieval in the loop; cross-tenant retrieval is shown impossible by test; recall and answer accuracy on the golden dataset meet the bar agreed from the first baseline.
