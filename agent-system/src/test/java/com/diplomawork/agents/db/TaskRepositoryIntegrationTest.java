package com.diplomawork.agents.db;

import com.diplomawork.agents.model.Task;
import com.diplomawork.agents.model.TaskStatus;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TaskRepositoryIntegrationTest {
    private static final String TEST_AGENT = "integration-test-executor";
    private static final int EXTREME_STALE_MINUTES = 100_000_000;

    private final TaskRepository repository = new TaskRepository();
    private final List<Long> taskIds = new ArrayList<>();

    @BeforeClass
    public static void requireExplicitIntegrationTestRun() {
        Assume.assumeTrue("true".equalsIgnoreCase(System.getenv("RUN_DB_INTEGRATION_TESTS")));
    }

    @After
    public void deleteTestTasks() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "delete from public.tasks where id = ?"
             )) {
            for (long taskId : taskIds) {
                statement.setLong(1, taskId);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    @Test
    public void completesTaskAndRecordsLifecycleEvents() throws SQLException {
        long taskId = createPendingTask(1);

        Task assigned = repository.assignPendingTask(taskId, TEST_AGENT).orElseThrow();
        Task running = repository.markTaskRunning(taskId, TEST_AGENT).orElseThrow();
        Task completed = repository.markTaskCompleted(taskId, TEST_AGENT, "done").orElseThrow();

        assertEquals(TaskStatus.ASSIGNED, assigned.getStatus());
        assertEquals(TaskStatus.RUNNING, running.getStatus());
        assertEquals(1, running.getAttemptCount());
        assertEquals(TaskStatus.COMPLETED, completed.getStatus());
        assertEquals("done", completed.getResult());
        assertNotNull(completed.getFinishedAt());
        assertEquals(
            List.of("CREATED", "ASSIGNED", "STARTED", "COMPLETED"),
            eventTypes(taskId)
        );
    }

    @Test
    public void retriesOnceThenRecordsTerminalFailure() throws SQLException {
        long taskId = createPendingTask(2);

        repository.assignPendingTask(taskId, TEST_AGENT).orElseThrow();
        repository.markTaskRunning(taskId, TEST_AGENT).orElseThrow();
        Task retry = repository.handleTaskFailure(taskId, TEST_AGENT, "first failure").orElseThrow();

        assertEquals(TaskStatus.PENDING, retry.getStatus());
        assertEquals(1, retry.getAttemptCount());
        assertNull(retry.getAssignedAgent());
        assertNull(retry.getFinishedAt());

        repository.assignPendingTask(taskId, TEST_AGENT).orElseThrow();
        repository.markTaskRunning(taskId, TEST_AGENT).orElseThrow();
        Task failed = repository.handleTaskFailure(taskId, TEST_AGENT, "second failure").orElseThrow();

        assertEquals(TaskStatus.FAILED, failed.getStatus());
        assertEquals(2, failed.getAttemptCount());
        assertEquals("second failure", failed.getError());
        assertNotNull(failed.getFinishedAt());
        assertEquals(
            List.of(
                "CREATED",
                "ASSIGNED",
                "STARTED",
                "RETRY_SCHEDULED",
                "ASSIGNED",
                "STARTED",
                "FAILED"
            ),
            eventTypes(taskId)
        );
    }

    @Test
    public void resolvesStaleTasksAccordingToRemainingAttempts() throws SQLException {
        long retryableTaskId = createAncientRunningTask(1, 2);
        long exhaustedTaskId = createAncientRunningTask(1, 1);

        int resolved = repository.resolveStaleTasks(EXTREME_STALE_MINUTES);

        assertEquals(2, resolved);

        Task retryable = readTask(retryableTaskId);
        assertEquals(TaskStatus.PENDING, retryable.getStatus());
        assertNull(retryable.getAssignedAgent());
        assertNull(retryable.getError());
        assertEquals(List.of("CREATED", "RECOVERED"), eventTypes(retryableTaskId));

        Task exhausted = readTask(exhaustedTaskId);
        assertEquals(TaskStatus.FAILED, exhausted.getStatus());
        assertEquals("Stale task exhausted maximum attempts", exhausted.getError());
        assertNotNull(exhausted.getFinishedAt());
        assertEquals(List.of("CREATED", "FAILED"), eventTypes(exhaustedTaskId));
    }

    private long createPendingTask(int maxAttempts) throws SQLException {
        String sql = """
            insert into public.tasks (task_type, input_data, priority, status, max_attempts)
            values ('INTEGRATION_TEST', '{}'::jsonb, -1000, 'PENDING', ?)
            returning id
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, maxAttempts);
            return recordTaskId(statement);
        }
    }

    private long createAncientRunningTask(int attemptCount, int maxAttempts) throws SQLException {
        String sql = """
            insert into public.tasks (
                task_type,
                input_data,
                priority,
                status,
                assigned_agent,
                assigned_at,
                started_at,
                attempt_count,
                max_attempts
            )
            values (
                'INTEGRATION_TEST',
                '{}'::jsonb,
                -1000,
                'RUNNING',
                ?,
                now() - interval '300 years',
                now() - interval '300 years',
                ?,
                ?
            )
            returning id
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TEST_AGENT);
            statement.setInt(2, attemptCount);
            statement.setInt(3, maxAttempts);
            return recordTaskId(statement);
        }
    }

    private long recordTaskId(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next());
            long taskId = resultSet.getLong("id");
            taskIds.add(taskId);
            return taskId;
        }
    }

    private Task readTask(long taskId) throws SQLException {
        String sql = """
            select
                status,
                assigned_agent,
                error,
                finished_at
            from public.tasks
            where id = ?
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                Task task = new Task();
                task.setStatus(TaskStatus.valueOf(resultSet.getString("status")));
                task.setAssignedAgent(resultSet.getString("assigned_agent"));
                task.setError(resultSet.getString("error"));
                if (resultSet.getTimestamp("finished_at") != null) {
                    task.setFinishedAt(
                        resultSet.getTimestamp("finished_at")
                            .toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toOffsetDateTime()
                    );
                }
                return task;
            }
        }
    }

    private List<String> eventTypes(long taskId) throws SQLException {
        String sql = """
            select event_type
            from public.task_events
            where task_id = ?
            order by id
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> eventTypes = new ArrayList<>();
                while (resultSet.next()) {
                    eventTypes.add(resultSet.getString("event_type"));
                }
                return eventTypes;
            }
        }
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
            DatabaseConfig.jdbcUrl(),
            DatabaseConfig.user(),
            DatabaseConfig.password()
        );
    }
}
