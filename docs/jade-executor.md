# JADE Executor Agent

The first executor agent receives assignment messages and performs a placeholder execution lifecycle.

## File

- `agent-system/src/main/java/com/diplomawork/agents/executor/ExecutorAgent.java`

## Current Behavior

On startup, the executor:

1. prints its local JADE agent name;
2. waits for ACL messages;
3. handles messages with conversation id `task-assignment`;
4. marks the assigned task `RUNNING` if it is assigned to this executor;
5. simulates work with a short sleep;
6. marks the task `COMPLETED` with a placeholder result.

On failure, it attempts to mark the task `FAILED`.
