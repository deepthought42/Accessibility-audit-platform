#!/usr/bin/env bash
set -euo pipefail

NEO4J_HOST="${NEO4J_HOST:-neo4j}"
NEO4J_BOLT_PORT="${NEO4J_BOLT_PORT:-7687}"
NEO4J_USERNAME="${NEO4J_USERNAME:-neo4j}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-change-me}"
SRC_CQL="${SRC_CQL:-/init/create-indexes-and-constraints.cql}"
DST_CQL="/tmp/init-neo4j5.cql"

echo "[neo4j-bootstrap] translating CQL to Neo4j 5 syntax"
# Neo4j 5 changes:
#   * Index/constraint introducer changed from ON (var:Label) to FOR (var:Label)
#   * Constraint assertion changed from ASSERT ... IS UNIQUE to REQUIRE ... IS UNIQUE
#   * `CALL db.indexes()` was renamed to SHOW INDEXES (and is not needed at bootstrap time)
sed -E \
  -e '/CREATE CONSTRAINT/ s/ ON \(/ FOR (/' \
  -e 's/ASSERT[[:space:]]+(.*)[[:space:]]+IS UNIQUE/REQUIRE \1 IS UNIQUE/g' \
  -e '/^[[:space:]]*CALL[[:space:]]+db\.indexes\(\)/d' \
  "${SRC_CQL}" > "${DST_CQL}"

echo "[neo4j-bootstrap] waiting for bolt://${NEO4J_HOST}:${NEO4J_BOLT_PORT}"
for _ in $(seq 1 60); do
  if cypher-shell -a "bolt://${NEO4J_HOST}:${NEO4J_BOLT_PORT}" \
      -u "${NEO4J_USERNAME}" -p "${NEO4J_PASSWORD}" \
      "RETURN 1" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "[neo4j-bootstrap] applying indexes and constraints"
cypher-shell -a "bolt://${NEO4J_HOST}:${NEO4J_BOLT_PORT}" \
  -u "${NEO4J_USERNAME}" -p "${NEO4J_PASSWORD}" \
  -f "${DST_CQL}"

# Seed an Account row for the principal that LocalSecurityConfig installs
# (`local-dev-user`). Two naming conventions need to coexist on the node:
#
#   - The `@Query` in AccountRepository and the indexes in
#     `create-indexes-and-constraints.cql` reference snake_case property
#     names (e.g. `user_id`, `subscription_token`, `api_token`,
#     `customer_token`, `subscription_type`), so the lookup query and
#     index hits depend on those keys.
#   - Spring Data Neo4j's default mapping uses Java field names verbatim
#     (camelCase), so `Account.subscriptionToken` hydrates from a Neo4j
#     `subscriptionToken` property, not `subscription_token`.
#
# Set both spellings so the account is both findable by user_id and
# fully hydrated when SDN reads it back (otherwise `acct.getSubscriptionToken()`
# is null and AuditorController throws MissingSubscriptionException).
# Idempotent via MERGE.
LOCAL_USER_ID="${LOCAL_DEV_USER_ID:-local-dev-user}"
echo "[neo4j-bootstrap] seeding local-dev Account row for user_id=${LOCAL_USER_ID}"
cypher-shell -a "bolt://${NEO4J_HOST}:${NEO4J_BOLT_PORT}" \
  -u "${NEO4J_USERNAME}" -p "${NEO4J_PASSWORD}" \
  "MERGE (a:Account {user_id: \$user_id})
   ON CREATE SET
     a.userId = \$user_id,
     a.email = 'local-dev@example.test',
     a.name = 'Local Dev',
     a.subscription_type = 'pro',
     a.subscriptionType = 'pro',
     a.subscription_token = 'local-disabled-subscription',
     a.subscriptionToken = 'local-disabled-subscription',
     a.customer_token = 'local-disabled-customer',
     a.customerToken = 'local-disabled-customer',
     a.api_token = 'local-disabled-api-token',
     a.apiToken = 'local-disabled-api-token'
   RETURN a.user_id" \
  --param "user_id => '${LOCAL_USER_ID}'"

echo "[neo4j-bootstrap] done"
