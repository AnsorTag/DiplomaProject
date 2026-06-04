package com.diplomawork.agents.execution;

import com.diplomawork.agents.model.Task;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TaskExecutorTest {
    private final TaskExecutor executor = new TaskExecutor();

    @Test
    public void executesEchoTask() throws TaskExecutionException {
        String result = executor.execute(task("ECHO", "{\"message\":\"hello\"}"));

        assertEquals("hello", result);
    }

    @Test
    public void rejectsEchoWithoutTextMessage() {
        TaskExecutionException error = assertThrows(
            TaskExecutionException.class,
            () -> executor.execute(task("ECHO", "{\"message\":42}"))
        );

        assertEquals("ECHO task requires input_data.message as text", error.getMessage());
    }

    @Test
    public void normalizesAndTruncatesTextSummary() throws TaskExecutionException {
        String result = executor.execute(task(
            "TEXT_SUMMARY",
            "{\"text\":\"  one   two\\nthree four  \",\"max_length\":13}"
        ));

        assertEquals("one two th...", result);
        assertEquals(13, result.length());
    }

    @Test
    public void keepsShortSummaryWithinMaxLength() throws TaskExecutionException {
        String result = executor.execute(task(
            "TEXT_SUMMARY",
            "{\"text\":\"long text\",\"max_length\":2}"
        ));

        assertEquals("..", result);
    }

    @Test
    public void rejectsInvalidSummaryMaxLength() {
        TaskExecutionException error = assertThrows(
            TaskExecutionException.class,
            () -> executor.execute(task("TEXT_SUMMARY", "{\"text\":\"hello\",\"max_length\":0}"))
        );

        assertEquals(
            "TEXT_SUMMARY input_data.max_length must be a positive integer",
            error.getMessage()
        );
    }

    @Test
    public void rejectsMalformedInputJson() {
        TaskExecutionException error = assertThrows(
            TaskExecutionException.class,
            () -> executor.execute(task("ECHO", "{invalid"))
        );

        assertEquals("Invalid input_data JSON", error.getMessage());
        assertTrue(error.getCause() != null);
    }

    @Test
    public void rejectsUnsupportedTaskType() {
        TaskExecutionException error = assertThrows(
            TaskExecutionException.class,
            () -> executor.execute(task("UNKNOWN_TASK", "{}"))
        );

        assertEquals("Unsupported task type: UNKNOWN_TASK", error.getMessage());
    }

    private Task task(String taskType, String inputDataJson) {
        Task task = new Task();
        task.setTaskType(taskType);
        task.setInputDataJson(inputDataJson);
        return task;
    }
}
