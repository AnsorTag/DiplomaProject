# JADE Coordinator Agent

The JADE coordinator now performs one controlled scheduling action and notifies an executor agent.

## File

- `agent-system/src/main/java/com/diplomawork/agents/coordinator/CoordinatorAgent.java`

## Current Behavior

On startup, the agent:

1. prints its local JADE agent name;
2. uses `TaskRepository.findPendingTasks(10)`;
3. selects the first pending task by repository ordering;
4. calls `TaskRepository.assignPendingTask(taskId, "executor")`;
5. updates the task only if it is still `PENDING`;
6. sends a JADE `REQUEST` message to the `executor` agent with the task id.

## Dependency Note

The older `com.tilab.jade:jade` artifact is hosted outside Maven Central. The project uses the Maven-compatible JADE GitLab/JitPack coordinates documented by the JADE project site.

## Run

From `agent-system/`:

```bash
mvn exec:java -Dexec.mainClass=jade.Boot -Dexec.args="-agents executor:com.diplomawork.agents.executor.ExecutorAgent;coordinator:com.diplomawork.agents.coordinator.CoordinatorAgent"
```

This starts a JADE platform and launches one coordinator agent.

For a short verification run without leaving the platform active:

```bash
timeout 10s mvn exec:java -Dexec.mainClass=jade.Boot -Dexec.args="-agents executor:com.diplomawork.agents.executor.ExecutorAgent;coordinator:com.diplomawork.agents.coordinator.CoordinatorAgent"
```

Verified behavior: the coordinator starts, reads pending tasks from PostgreSQL, atomically assigns one pending task to `executor`, and sends the executor a task assignment message.
