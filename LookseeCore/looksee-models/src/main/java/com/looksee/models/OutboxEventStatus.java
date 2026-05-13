package com.looksee.models;

/**
 * Constants for the {@link OutboxEvent#getStatus()} field.
 *
 * <p>The entity continues to store status as a {@code String} (preserving the
 * LookseeCore 1.0.0 public API). Call sites use these constants via
 * {@code OutboxEventStatus.PENDING.name()} so the on-disk values cannot drift
 * from typos.</p>
 */
public enum OutboxEventStatus {
    PENDING,
    PROCESSED,
    FAILED
}
