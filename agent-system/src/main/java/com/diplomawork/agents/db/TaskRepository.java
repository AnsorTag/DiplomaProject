package com.diplomawork.agents.db;

import com.diplomawork.agents.model.Task;
import com.diplomawork.agents.model.TaskStatus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for reading and assigning tasks in PostgreSQL.
 */
public class TaskRepository {
    public List<Task> findPendingTasks(int limit) throws SQLException {
        String sql = """
            select
                id,
                task_type,
                input_data::text as input_data_json,
                priority,
                attempt_count,
                max_attempts,
                status,
                assigned_agent,
                assigned_at,
                result,
                error,
                created_at,
                started_at,
                finished_at
            from public.tasks
            where status = ?
              and attempt_count < max_attempts
            order by priority desc, created_at asc
            limit ?
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TaskStatus.PENDING.name());
            statement.setInt(2, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Task> tasks = new ArrayList<>();
                while (resultSet.next()) {
                    tasks.add(mapTask(resultSet));
                }
                return tasks;
            }
        }
    }


    public int countActiveTasksForAgent(String agentName) throws SQLException {
        String sql = """
            select count(*)
            from public.tasks
            where assigned_agent = ?
              and status in (?, ?)
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, agentName);
            statement.setString(2, TaskStatus.ASSIGNED.name());
            statement.setString(3, TaskStatus.RUNNING.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public Optional<Task> assignPendingTask(long taskId, String agentName) throws SQLException {
        String sql = """
            update public.tasks
            set status = ?,
                assigned_agent = ?,
                assigned_at = now(),
                result = null,
                error = null,
                finished_at = null
            where id = ? and status = ? and attempt_count < max_attempts
            returning
                id,
                task_type,
                input_data::text as input_data_json,
                priority,
                attempt_count,
                max_attempts,
                status,
                assigned_agent,
                assigned_at,
                result,
                error,
                created_at,
                started_at,
                finished_at
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TaskStatus.ASSIGNED.name());
            statement.setString(2, agentName);
            statement.setLong(3, taskId);
            statement.setString(4, TaskStatus.PENDING.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapTask(resultSet));
            }
        }
    }

    public Optional<Task> markTaskRunning(long taskId, String agentName) throws SQLException {
        String sql = """
            update public.tasks
            set status = ?, started_at = now(), attempt_count = attempt_count + 1
            where id = ? and assigned_agent = ? and status = ? and attempt_count < max_attempts
            returning
                id,
                task_type,
                input_data::text as input_data_json,
                priority,
                attempt_count,
                max_attempts,
                status,
                assigned_agent,
                assigned_at,
                result,
                error,
                created_at,
                started_at,
                finished_at
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TaskStatus.RUNNING.name());
            statement.setLong(2, taskId);
            statement.setString(3, agentName);
            statement.setString(4, TaskStatus.ASSIGNED.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapTask(resultSet));
            }
        }
    }

    public Optional<Task> markTaskCompleted(long taskId, String agentName, String result) throws SQLException {
        String sql = """
            update public.tasks
            set status = ?, result = ?, finished_at = now()
            where id = ? and assigned_agent = ? and status = ?
            returning
                id,
                task_type,
                input_data::text as input_data_json,
                priority,
                attempt_count,
                max_attempts,
                status,
                assigned_agent,
                assigned_at,
                result,
                error,
                created_at,
                started_at,
                finished_at
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TaskStatus.COMPLETED.name());
            statement.setString(2, result);
            statement.setLong(3, taskId);
            statement.setString(4, agentName);
            statement.setString(5, TaskStatus.RUNNING.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapTask(resultSet));
            }
        }
    }

    public Optional<Task> handleTaskFailure(long taskId, String agentName, String error) throws SQLException {
        String sql = """
            with target as (
                select id, status = ? and attempt_count < max_attempts as should_retry
                from public.tasks
                where id = ? and assigned_agent = ? and status in (?, ?)
                for update
            )
            update public.tasks as task
            set status = case when target.should_retry then ? else ? end,
                assigned_agent = case when target.should_retry then null else task.assigned_agent end,
                assigned_at = case when target.should_retry then null else task.assigned_at end,
                started_at = case when target.should_retry then null else task.started_at end,
                finished_at = case when target.should_retry then null else now() end,
                result = null,
                error = ?
            from target
            where task.id = target.id
            returning
                task.id,
                task.task_type,
                task.input_data::text as input_data_json,
                task.priority,
                task.attempt_count,
                task.max_attempts,
                task.status,
                task.assigned_agent,
                task.assigned_at,
                task.result,
                task.error,
                task.created_at,
                task.started_at,
                task.finished_at
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TaskStatus.RUNNING.name());
            statement.setLong(2, taskId);
            statement.setString(3, agentName);
            statement.setString(4, TaskStatus.ASSIGNED.name());
            statement.setString(5, TaskStatus.RUNNING.name());
            statement.setString(6, TaskStatus.PENDING.name());
            statement.setString(7, TaskStatus.FAILED.name());
            statement.setString(8, error);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapTask(resultSet));
            }
        }
    }

    public int resolveStaleTasks(int staleMinutes) throws SQLException {
        String sql = """
            update public.tasks
            set status = case when attempt_count < max_attempts then ? else ? end,
                assigned_agent = case when attempt_count < max_attempts then null else assigned_agent end,
                assigned_at = case when attempt_count < max_attempts then null else assigned_at end,
                started_at = case when attempt_count < max_attempts then null else started_at end,
                finished_at = case when attempt_count < max_attempts then null else now() end,
                result = null,
                error = case
                    when attempt_count < max_attempts then null
                    else 'Stale task exhausted maximum attempts'
                end
            where (
                status = ?
                and coalesce(assigned_at, created_at) < now() - (? * interval '1 minute')
            ) or (
                status = ?
                and coalesce(started_at, assigned_at, created_at) < now() - (? * interval '1 minute')
            )
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TaskStatus.PENDING.name());
            statement.setString(2, TaskStatus.FAILED.name());
            statement.setString(3, TaskStatus.ASSIGNED.name());
            statement.setInt(4, staleMinutes);
            statement.setString(5, TaskStatus.RUNNING.name());
            statement.setInt(6, staleMinutes);
            return statement.executeUpdate();
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
            DatabaseConfig.jdbcUrl(),
            DatabaseConfig.user(),
            DatabaseConfig.password()
        );
    }

    private Task mapTask(ResultSet resultSet) throws SQLException {
        Task task = new Task();
        task.setId(resultSet.getLong("id"));
        task.setTaskType(resultSet.getString("task_type"));
        task.setInputDataJson(resultSet.getString("input_data_json"));
        task.setPriority(resultSet.getInt("priority"));
        task.setAttemptCount(resultSet.getInt("attempt_count"));
        task.setMaxAttempts(resultSet.getInt("max_attempts"));
        task.setStatus(TaskStatus.valueOf(resultSet.getString("status")));
        task.setAssignedAgent(resultSet.getString("assigned_agent"));
        task.setAssignedAt(toOffsetDateTime(resultSet.getTimestamp("assigned_at")));
        task.setResult(resultSet.getString("result"));
        task.setError(resultSet.getString("error"));
        task.setCreatedAt(toOffsetDateTime(resultSet.getTimestamp("created_at")));
        task.setStartedAt(toOffsetDateTime(resultSet.getTimestamp("started_at")));
        task.setFinishedAt(toOffsetDateTime(resultSet.getTimestamp("finished_at")));
        return task;
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
