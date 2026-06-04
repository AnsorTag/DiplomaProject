# Java Database Access

The Java agent layer uses JDBC to schedule, execute, and recover tasks in the existing PostgreSQL server.

## Files

- `agent-system/pom.xml` - Maven build with PostgreSQL JDBC dependency.
- `agent-system/src/main/java/com/diplomawork/agents/db/DatabaseConfig.java` - Java database settings.
- `agent-system/src/main/java/com/diplomawork/agents/db/DatabaseConnectionCheck.java` - read-only connection check.

## Configuration

The Java default connection target is:

```text
jdbc:postgresql://localhost:5432/tasks
```

Configure credentials through environment variables or the ignored root `.env` file:

```text
DB_JDBC_URL
DB_USER
DB_PASSWORD
```

The coordinator recovery threshold is configured separately:

```text
STALE_TASK_MINUTES
```

It defaults to `30`; set it to `0` to disable automatic stale-task recovery.

## Commands

Compile:

```bash
cd agent-system
mvn compile
```

Run the read-only database check:

```bash
cd agent-system
mvn exec:java -Dexec.mainClass=com.diplomawork.agents.db.DatabaseConnectionCheck
```

Apply the idempotent task schema migration after pulling schema-related changes:

```bash
./venv/bin/python scripts/migrate_tasks_multi_agent.py
```

## Task Repository

The Java repository supports:

- loading pending tasks;
- atomically assigning pending tasks and recording `assigned_at`;
- marking tasks `RUNNING`, `COMPLETED`, or `FAILED`;
- counting active tasks per executor;
- resetting stale `ASSIGNED` and `RUNNING` tasks to `PENDING`.

Run the pending task check:

```bash
cd agent-system
mvn exec:java -Dexec.mainClass=com.diplomawork.agents.db.PendingTasksCheck -Dexec.args="10"
```

Assign a pending task to an agent:

```bash
cd agent-system
mvn exec:java -Dexec.mainClass=com.diplomawork.agents.db.AssignTaskCheck -Dexec.args="1 coordinator-agent-1"
```

The assignment update only succeeds when the task is still `PENDING`.
