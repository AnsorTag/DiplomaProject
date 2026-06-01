package com.diplomawork.agents.db;

import com.diplomawork.agents.model.Task;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Assigns one pending task to an agent for repository verification.
 */
public class AssignTaskCheck {
    public static void main(String[] args) throws SQLException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: AssignTaskCheck <taskId> <agentName>");
        }

        long taskId = Long.parseLong(args[0]);
        String agentName = args[1];

        TaskRepository repository = new TaskRepository();
        Optional<Task> task = repository.assignPendingTask(taskId, agentName);

        if (task.isEmpty()) {
            System.out.println("Task was not assigned. It may not exist or may no longer be PENDING.");
            return;
        }

        System.out.println("Assigned task:");
        System.out.println(task.get());
    }
}
