#!/usr/bin/env bash
set -euo pipefail

NEO4J_HOST="${NEO4J_HOST:-neo4j}"
NEO4J_BOLT_PORT="${NEO4J_BOLT_PORT:-7687}"
NEO4J_USERNAME="${NEO4J_USERNAME:-neo4j}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-looksee-local}"
SRC_CQL="${SRC_CQL:-/init/create-indexes-and-constraints.cql}"
DST_CQL="/tmp/init-neo4j5.cql"

echo "[neo4j-bootstrap] translating CQL to Neo4j 5 syntax"
# Neo4j 5 renames ASSERT -> REQUIRE and removes CALL db.indexes()
sed -E \
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

echo "[neo4j-bootstrap] done"
