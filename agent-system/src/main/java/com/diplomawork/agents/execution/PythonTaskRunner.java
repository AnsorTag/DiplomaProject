package com.diplomawork.agents.execution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Runs Python-backed task modules through a small command-line bridge.
 */
public class PythonTaskRunner {
    private static final String DEFAULT_PYTHON_EXECUTABLE = "../venv/bin/python";
    private static final String DEFAULT_TASK_RUNNER = "../python_tasks/run_task.py";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public String run(String taskType, String inputDataJson) throws TaskExecutionException {
        ProcessBuilder processBuilder = new ProcessBuilder(
            pythonExecutable(),
            taskRunnerPath(),
            taskType,
            inputDataJson
        );

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TaskExecutionException("Python task timed out: " + taskType);
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();

            if (process.exitValue() != 0) {
                String message = stderr.isBlank() ? stdout : stderr;
                throw new TaskExecutionException("Python task failed: " + message);
            }

            return stdout;
        } catch (IOException error) {
            throw new TaskExecutionException("Could not start Python task runner", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new TaskExecutionException("Python task interrupted", error);
        }
    }

    private String pythonExecutable() {
        return valueOrDefault("PYTHON_EXECUTABLE", DEFAULT_PYTHON_EXECUTABLE);
    }

    private String taskRunnerPath() {
        return Path.of(valueOrDefault("PYTHON_TASK_RUNNER", DEFAULT_TASK_RUNNER)).toString();
    }

    private String valueOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
