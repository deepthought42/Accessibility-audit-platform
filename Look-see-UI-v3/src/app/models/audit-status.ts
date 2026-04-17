/**
 * Canonical audit lifecycle states.
 *
 * See docs/design/02-audit-status-and-progress.md §1.
 *
 * Backend today returns `status: string` on AuditRecord with varying tokens.
 * Until backend contract converges, use `normaliseAuditStatus()` to map any
 * legacy value into this enum.
 */
export enum AuditStatus {
  QUEUED = 'QUEUED',
  RUNNING = 'RUNNING',
  COMPLETE = 'COMPLETE',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

const TERMINAL_STATUSES: ReadonlySet<AuditStatus> = new Set([
  AuditStatus.COMPLETE,
  AuditStatus.FAILED,
  AuditStatus.CANCELLED,
]);

/** Chip `kind` value for a given status — passed to `<looksee-status-chip>`. */
export function auditStatusChipKind(status: AuditStatus):
  'queued' | 'running' | 'complete' | 'failed' | 'cancelled' {
  switch (status) {
    case AuditStatus.QUEUED:    return 'queued';
    case AuditStatus.RUNNING:   return 'running';
    case AuditStatus.COMPLETE:  return 'complete';
    case AuditStatus.FAILED:    return 'failed';
    case AuditStatus.CANCELLED: return 'cancelled';
  }
}

export function isTerminalStatus(status: AuditStatus): boolean {
  return TERMINAL_STATUSES.has(status);
}

/**
 * Map a raw backend `status` string into AuditStatus.
 *
 * Current backend emits values like 'COMPLETE', 'RUNNING', 'IN_PROGRESS',
 * 'ERROR', 'DONE', 'PENDING', lowercase variants, etc. Unknown values default
 * to RUNNING when scores are absent and COMPLETE when scores are present —
 * this is a bridge until the backend contract formalises (see spec §Risks #1).
 */
export function normaliseAuditStatus(
  raw: string | null | undefined,
  opts?: { hasScores?: boolean }
): AuditStatus {
  const v = (raw ?? '').toString().trim().toUpperCase();

  switch (v) {
    case 'QUEUED':
    case 'PENDING':
    case 'SCHEDULED':
      return AuditStatus.QUEUED;

    case 'RUNNING':
    case 'IN_PROGRESS':
    case 'PROCESSING':
    case 'STARTED':
      return AuditStatus.RUNNING;

    case 'COMPLETE':
    case 'COMPLETED':
    case 'DONE':
    case 'SUCCESS':
    case 'FINISHED':
      return AuditStatus.COMPLETE;

    case 'FAILED':
    case 'ERROR':
    case 'ERRORED':
      return AuditStatus.FAILED;

    case 'CANCELLED':
    case 'CANCELED':
    case 'ABORTED':
      return AuditStatus.CANCELLED;
  }

  // Unknown / empty — fall back based on data shape
  return opts?.hasScores ? AuditStatus.COMPLETE : AuditStatus.RUNNING;
}
