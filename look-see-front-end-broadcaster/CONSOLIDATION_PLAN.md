# Consolidation: this service is being merged into `audit-service`

Wave 3.3 of the architecture review (`/root/.claude/plans/gleaming-jumping-thunder.md`)
calls for retiring `look-see-front-end-broadcaster` and moving its single
responsibility — broadcasting `page_created` Pub/Sub messages to Pusher — into
`audit-service`, which already broadcasts `audit_update` messages to the same
Pusher cluster. Two services maintaining two separate Pusher publishers and two
separate Cloud Run revisions for one logical concern is unnecessary duplication.

## Status

- **Code path:** `src/main/java/com/looksee/frontEndBroadcaster/AuditController.java`
  has been migrated to use SLF4J (Wave 1.3, no functional change).
- **Migration:** the controller has **not yet** been moved to `audit-service`
  because the change requires coordinated Pub/Sub subscription repointing in
  production and end-to-end testing of the unified broadcaster behind a feature
  flag. Tracked as a follow-up to Wave 3.3.

## Migration steps (when ready)

1. Add a new `PageCreatedBroadcaster` bean to `audit-service` that:
   - Subscribes to the `page_created` Pub/Sub topic via a new push subscription.
   - Reuses the existing `MessageBroadcaster` Pusher client.
   - Emits the same channel/event names as today's broadcaster so the UI does
     not need to change.
2. In Terraform, add a second push subscription on `page_created` pointing at
   `audit-service`'s Cloud Run URL. Keep the existing broadcaster subscription
   alive in parallel.
3. Verify in staging that both subscribers emit the same Pusher events for the
   same `page_created` message. Cut traffic over by removing the old
   subscription.
4. Delete this directory, the per-service `docker-ci-*.yml` workflows, and any
   IaC references.
5. Mark the existing `look-see-front-end-broadcaster:*` Docker Hub tags as
   deprecated.

## Why deferred

- The service is *not* deployed via Terraform today (`grep -rn broadcaster
  LookseeIaC/GCP` returns nothing), so the Terraform repoint described in the
  plan does not apply. Whatever pipeline deploys this service must be
  documented and migrated first.
- Pusher channel naming conventions differ subtly between the two services and
  must be reconciled before consolidation, which requires real Pusher
  credentials to test against.
