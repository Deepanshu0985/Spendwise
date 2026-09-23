# AI Evaluation

## Areas
- Category and type accuracy.
- Merchant normalization.
- Intent and tool selection.
- Numerical grounding.
- Hallucination rate.
- Uncertainty behaviour.
- Prompt injection resistance.
- Latency and cost per request.

## Golden Dataset
Anonymized examples covering UPI, POS, ATM, NEFT/IMPS, salary, refund, card payment, transfer, subscription, interest earned, interest charged, fee charged and unknown-merchant rows. Never built from real user financial data.

Includes adversarial rows whose descriptions contain injection attempts.

## Grounding Test
Every monetary statement in an AI answer must map to a value returned by a tool in that same exchange. A number that cannot be traced to tool output is an evaluation failure, not a stylistic issue.

## Injection Test
An adversarial description must not induce a tool call the user did not ask for, must not cause any disclosure beyond the authenticated user's own data, and must not alter the assistant's stated instructions. Because user identity is session-derived and never a tool argument, a successful cross-tenant read is treated as a critical defect rather than an evaluation miss.

## Cost Regression
Track tokens and cost per categorization and per assistant answer. A change that raises either materially is reviewed before it ships, since AI cost is the only line item that scales with usage.

## Regression
Run the same golden dataset against every model or prompt version change. Prompt versions are recorded on `ai_insights` rows so past output remains attributable.

## Human Review
Low-confidence classifications remain reviewable by the user, and corrections feed merchant history so the rules layer improves over time.
