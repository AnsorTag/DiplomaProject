# Java Database Access

The Java agent layer now has a minimal JDBC connection check for the existing PostgreSQL server.

## Files

- `agent-system/pom.xml` - Maven build with PostgreSQL JDBC dependency.
- `agent-system/src/main/java/com/diplomawork/agents/db/DatabaseConfig.java` - Java database settings.
- `agent-system/src/main/java/com/diplomawork/agents/db/DatabaseConnectionCheck.java` - read-only connection check.

## Configuration

The Java defaults match the existing local PostgreSQL setup:

```text
jdbc:postgresql://localhost:5432/tasks
postgres / postgres
```

Optional environment variables:

```text
DB_JDBC_URL
DB_USER
DB_PASSWORD
```

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

## Task Repository

The Java repository currently supports read-only loading of pending tasks:

- `TaskRepository.findPendingTasks(limit)`
- `PendingTasksCheck` CLI for verification

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
