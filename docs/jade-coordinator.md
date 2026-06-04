# JADE Coordinator Agent

The JADE coordinator periodically schedules pending tasks and notifies the selected executor agent.

## File

- `agent-system/src/main/java/com/diplomawork/agents/coordinator/CoordinatorAgent.java`

## Current Behavior

On startup, the agent:

1. prints its local JADE agent name;
2. resolves stale `ASSIGNED` and `RUNNING` tasks based on remaining attempts;
3. performs one immediate scheduling attempt;
4. starts a `TickerBehaviour` that repeats scheduling every 2 seconds.

Startup recovery defaults to tasks older than 30 minutes. Configure the threshold with
`STALE_TASK_MINUTES`, or set it to `0` to disable automatic recovery.
Tasks with attempts remaining return to `PENDING`; exhausted tasks become `FAILED`.

On each scheduling attempt, the agent:

1. uses `TaskRepository.findPendingTasks(10)`;
2. reads configured executor names from JADE agent arguments;
3. selects the least-loaded executor by counting active `ASSIGNED` and `RUNNING` tasks;
4. selects the first pending task by repository ordering;
5. calls `TaskRepository.assignPendingTask(taskId, selectedExecutor)`;
6. updates the task only if it is still `PENDING`;
7. sends a JADE `REQUEST` message to the selected executor with the task id.

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
timeout 30s mvn exec:java -Dexec.mainClass=jade.Boot -Dexec.args="-agents executor1:com.diplomawork.agents.executor.ExecutorAgent;executor2:com.diplomawork.agents.executor.ExecutorAgent;coordinator:com.diplomawork.agents.coordinator.CoordinatorAgent(executor1,executor2)"
```

Verified behavior: the coordinator starts, repeatedly reads pending tasks from PostgreSQL, chooses a configured executor by load, atomically assigns pending tasks, and sends the selected executor a task assignment message.
