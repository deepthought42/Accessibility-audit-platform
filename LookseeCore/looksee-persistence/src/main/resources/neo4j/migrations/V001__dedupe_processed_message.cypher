// One-shot dedupe of any (pubsubMessageId, serviceName) collisions
// produced by the pre-claim() TOCTOU race in IdempotencyService.
//
// For each (msgId, svc) group with more than one node, keeps the first
// returned by collect() and DETACH DELETEs the rest. Which specific row
// "wins" is not semantically important — they're duplicates by definition;
// what matters is collapsing the group to exactly one node so V002's
// uniqueness constraint can be created.
//
// Idempotent and safe to re-run, but neo4j-migrations records it in
// :__Neo4jMigration so it never re-runs in practice. No-op against a
// fresh database.

MATCH (pm:ProcessedMessage)
WITH pm.pubsubMessageId AS msgId,
     pm.serviceName     AS svc,
     collect(pm)        AS dupes
WHERE size(dupes) > 1
UNWIND dupes[1..] AS extra
DETACH DELETE extra;
