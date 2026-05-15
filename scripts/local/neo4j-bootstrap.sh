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
# (`local-dev-user`). AccountService.findByUserId queries on the `user_id`
# Neo4j property; without this row, AuditorController throws
# UnknownAccountException / MissingSubscriptionException on every audit
# start under the local profile. Idempotent via MERGE.
LOCAL_USER_ID="${LOCAL_DEV_USER_ID:-local-dev-user}"
echo "[neo4j-bootstrap] seeding local-dev Account row for user_id=${LOCAL_USER_ID}"
cypher-shell -a "bolt://${NEO4J_HOST}:${NEO4J_BOLT_PORT}" \
  -u "${NEO4J_USERNAME}" -p "${NEO4J_PASSWORD}" \
  "MERGE (a:Account {user_id: \$user_id})
   ON CREATE SET
     a.email = 'local-dev@example.test',
     a.name = 'Local Dev',
     a.subscription_type = 'pro',
     a.subscription_token = 'local-disabled-subscription',
     a.customer_token = 'local-disabled-customer',
     a.api_token = 'local-disabled-api-token'
   RETURN a.user_id" \
  --param "user_id => '${LOCAL_USER_ID}'"

echo "[neo4j-bootstrap] done"
