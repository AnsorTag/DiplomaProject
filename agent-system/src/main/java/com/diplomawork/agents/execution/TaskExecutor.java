package com.diplomawork.agents.execution;

import com.diplomawork.agents.model.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Executes supported task types.
 */
public class TaskExecutor {
    private static final String ECHO_TASK_TYPE = "ECHO";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String execute(Task task) throws TaskExecutionException {
        if (ECHO_TASK_TYPE.equals(task.getTaskType())) {
            return executeEcho(task);
        }

        throw new TaskExecutionException("Unsupported task type: " + task.getTaskType());
    }

    private String executeEcho(Task task) throws TaskExecutionException {
        JsonNode input = parseInput(task);
        JsonNode message = input.get("message");
        if (message == null || !message.isTextual()) {
            throw new TaskExecutionException("ECHO task requires input_data.message as text");
        }
        return message.asText();
    }

    private JsonNode parseInput(Task task) throws TaskExecutionException {
        try {
            return objectMapper.readTree(task.getInputDataJson());
        } catch (JsonProcessingException error) {
            throw new TaskExecutionException("Invalid input_data JSON", error);
        }
    }
}
