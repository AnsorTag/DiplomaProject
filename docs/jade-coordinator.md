# JADE Coordinator Agent

The JADE coordinator performs one controlled scheduling action and notifies the selected executor agent.

## File

- `agent-system/src/main/java/com/diplomawork/agents/coordinator/CoordinatorAgent.java`

## Current Behavior

On startup, the agent:

1. prints its local JADE agent name;
2. uses `TaskRepository.findPendingTasks(10)`;
3. reads configured executor names from JADE agent arguments;
4. selects the least-loaded executor by counting active `ASSIGNED` and `RUNNING` tasks;
5. selects the first pending task by repository ordering;
6. calls `TaskRepository.assignPendingTask(taskId, selectedExecutor)`;
7. updates the task only if it is still `PENDING`;
8. sends a JADE `REQUEST` message to the selected executor with the task id.

## Dependency Note

The older `com.tilab.jade:jade` artifact is hosted outside Maven Central. The project uses the Maven-compatible JADE GitLab/JitPack coordinates documented by the JADE project site.

## Run

From `agent-system/`:

```bash
mvn exec:java -Dexec.mainClass=jade.Boot -Dexec.args="-agents executor1:com.diplomawork.agents.executor.ExecutorAgent;executor2:com.diplomawork.agents.executor.ExecutorAgent;coordinator:com.diplomawork.agents.coordinator.CoordinatorAgent(executor1,executor2)"
```

This starts a JADE platform and launches one coordinator agent.

For a short verification run without leaving the platform active:

```bash
timeout 10s mvn exec:java -Dexec.mainClass=jade.Boot -Dexec.args="-agents executor1:com.diplomawork.agents.executor.ExecutorAgent;executor2:com.diplomawork.agents.executor.ExecutorAgent;coordinator:com.diplomawork.agents.coordinator.CoordinatorAgent(executor1,executor2)"
```

Verified behavior: the coordinator starts, reads pending tasks from PostgreSQL, chooses a configured executor by load, atomically assigns one pending task, and sends the selected executor a task assignment message.
