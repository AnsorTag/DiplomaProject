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
    private static final String TEXT_SUMMARY_TASK_TYPE = "TEXT_SUMMARY";
    private static final String PYTHON_ECHO_TASK_TYPE = "PYTHON_ECHO";
    private static final String TEXT_STATS_TASK_TYPE = "TEXT_STATS";
    private static final String KEYWORD_COUNT_TASK_TYPE = "KEYWORD_COUNT";
    private static final int DEFAULT_SUMMARY_MAX_LENGTH = 120;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PythonTaskRunner pythonTaskRunner = new PythonTaskRunner();

    public String execute(Task task) throws TaskExecutionException {
        if (ECHO_TASK_TYPE.equals(task.getTaskType())) {
            return executeEcho(task);
        }
        if (TEXT_SUMMARY_TASK_TYPE.equals(task.getTaskType())) {
            return executeTextSummary(task);
        }
        if (PYTHON_ECHO_TASK_TYPE.equals(task.getTaskType())
            || TEXT_STATS_TASK_TYPE.equals(task.getTaskType())
            || KEYWORD_COUNT_TASK_TYPE.equals(task.getTaskType())) {
            return pythonTaskRunner.run(task.getTaskType(), task.getInputDataJson());
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

    private String executeTextSummary(Task task) throws TaskExecutionException {
        JsonNode input = parseInput(task);
        JsonNode text = input.get("text");
        if (text == null || !text.isTextual()) {
            throw new TaskExecutionException("TEXT_SUMMARY task requires input_data.text as text");
        }

        int maxLength = readMaxLength(input);
        String normalizedText = normalizeWhitespace(text.asText());
        if (normalizedText.length() <= maxLength) {
            return normalizedText;
        }

        if (maxLength <= 3) {
            return ".".repeat(maxLength);
        }

        return normalizedText.substring(0, maxLength - 3).trim() + "...";
    }

    private int readMaxLength(JsonNode input) throws TaskExecutionException {
        JsonNode maxLength = input.get("max_length");
        if (maxLength == null || maxLength.isNull()) {
            return DEFAULT_SUMMARY_MAX_LENGTH;
        }
        if (!maxLength.isInt() || maxLength.asInt() <= 0) {
            throw new TaskExecutionException("TEXT_SUMMARY input_data.max_length must be a positive integer");
        }
        return maxLength.asInt();
    }

    private String normalizeWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private JsonNode parseInput(Task task) throws TaskExecutionException {
        try {
            return objectMapper.readTree(task.getInputDataJson());
        } catch (JsonProcessingException error) {
            throw new TaskExecutionException("Invalid input_data JSON", error);
        }
    }
}
