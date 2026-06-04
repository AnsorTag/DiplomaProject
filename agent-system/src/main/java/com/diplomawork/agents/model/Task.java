package com.diplomawork.agents.model;

import java.time.OffsetDateTime;

/**
 * Java-side representation of a row in the PostgreSQL public.tasks table.
 *
 * JSON fields are kept as strings for now so the initial Java contract does not
 * depend on a specific JSON library before build tooling is chosen.
 */
public class Task {
    private Long id;
    private String taskType;
    private String inputDataJson;
    private int priority;
    private int attemptCount;
    private int maxAttempts;
    private TaskStatus status;
    private String assignedAgent;
    private String result;
    private String error;
    private OffsetDateTime createdAt;
    private OffsetDateTime assignedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;

    public Task() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getInputDataJson() {
        return inputDataJson;
    }

    public void setInputDataJson(String inputDataJson) {
        this.inputDataJson = inputDataJson;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getAssignedAgent() {
        return assignedAgent;
    }

    public void setAssignedAgent(String assignedAgent) {
        this.assignedAgent = assignedAgent;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    @Override
    public String toString() {
        return "Task{" +
            "id=" + id +
            ", taskType='" + taskType + "'" +
            ", priority=" + priority +
            ", attempts=" + attemptCount + "/" + maxAttempts +
            ", status=" + status +
            ", assignedAgent='" + assignedAgent + "'" +
            "}";
    }
}
