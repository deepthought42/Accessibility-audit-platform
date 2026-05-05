// Enforces the (pubsubMessageId, serviceName) invariant the new atomic
// IdempotencyService.claim() (issue #81) relies on for atomic MERGE
// semantics under concurrent Pub/Sub redelivery.
//
// Depends on V001 having dedupe'd any pre-existing collisions, otherwise
// constraint creation fails. neo4j-migrations runs files in version order.

CREATE CONSTRAINT processed_message_unique IF NOT EXISTS
FOR (pm:ProcessedMessage)
REQUIRE (pm.pubsubMessageId, pm.serviceName) IS UNIQUE;
