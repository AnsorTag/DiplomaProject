package com.diplomawork.agents.model;

/**
 * Lifecycle states used by the multi-agent task scheduler.
 */
public enum TaskStatus {
    PENDING,
    ASSIGNED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
