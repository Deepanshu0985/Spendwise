-- All category aggregation reads this view, never transactions.category_id
-- directly (ADR-013, analytics-specification.md).
--
-- security_invoker = true is not optional here: without it, a Postgres view
-- runs with its OWNER's permissions by default - the privileged migration
-- role this view is created under - which would silently bypass RLS on
-- transactions/transaction_splits for every caller, regardless of FORCE
-- ROW LEVEL SECURITY on the underlying tables. This would be exactly the
-- kind of quietly-inert protection already found twice in this project
-- (see DECISIONS.md) if missed. Verified empirically, not just by reading
-- the option name, before trusting it - see the IT test suite.
CREATE VIEW transaction_category_allocations WITH (security_invoker = true) AS
SELECT t.id AS transaction_id, t.user_id, t.category_id, t.amount, t.transaction_date,
       t.currency, t.transaction_type, t.status
FROM transactions t
WHERE NOT EXISTS (SELECT 1 FROM transaction_splits s WHERE s.transaction_id = t.id)
UNION ALL
SELECT s.transaction_id, s.user_id, s.category_id, s.amount, t.transaction_date,
       t.currency, t.transaction_type, t.status
FROM transaction_splits s
JOIN transactions t ON t.id = s.transaction_id;
