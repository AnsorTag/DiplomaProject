# JADE Executor Agent

The first executor agent only receives task assignment messages.

## File

- `agent-system/src/main/java/com/diplomawork/agents/executor/ExecutorAgent.java`

## Current Behavior

On startup, the executor:

1. prints its local JADE agent name;
2. waits for ACL messages;
3. handles messages with conversation id `task-assignment`;
4. prints the assigned task id and sender.

It does not mark tasks as `RUNNING` or execute work yet.
