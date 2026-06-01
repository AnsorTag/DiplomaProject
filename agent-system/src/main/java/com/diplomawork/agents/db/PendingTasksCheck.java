package com.diplomawork.agents.db;

import com.diplomawork.agents.model.Task;

import java.sql.SQLException;
import java.util.List;

/**
 * Read-only check for loading pending tasks through the Java repository.
 */
public class PendingTasksCheck {
    private static final int DEFAULT_LIMIT = 10;

    public static void main(String[] args) throws SQLException {
        int limit = parseLimit(args);
        TaskRepository repository = new TaskRepository();
        List<Task> tasks = repository.findPendingTasks(limit);

        System.out.println("Pending tasks found: " + tasks.size());
        for (Task task : tasks) {
            System.out.println(task);
        }
    }

    private static int parseLimit(String[] args) {
        if (args.length == 0) {
            return DEFAULT_LIMIT;
        }
        return Integer.parseInt(args[0]);
    }
}
