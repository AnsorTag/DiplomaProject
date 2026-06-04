# JADE Executor Agent

The first executor agent receives assignment messages and performs task-type-based execution.

## File

- `agent-system/src/main/java/com/diplomawork/agents/executor/ExecutorAgent.java`

## Current Behavior

On startup, the executor:

1. prints its local JADE agent name;
2. waits for ACL messages;
3. handles messages with conversation id `task-assignment`;
4. marks the assigned task `RUNNING` if it is assigned to this executor;
5. executes supported task types;
6. marks the task `COMPLETED` with the execution result.

On failure, it schedules another `PENDING` attempt while attempts remain. At the
configured attempt limit, it marks the task `FAILED`.

## Supported Task Types

### ECHO

Input:

```json
{"message": "hello"}
```

Result:

```text
hello
```

### TEXT_SUMMARY

Input:

```json
{"text": "long text to summarize", "max_length": 120}
```

Result:

```text
The input text normalized and truncated to `max_length` characters.
```

`max_length` is optional and defaults to 120.

### PYTHON_ECHO

Input:

```json
{"message": "hello from Python"}
```

Result:

```text
hello from Python
```

This task is executed by the Java executor through `python_tasks/run_task.py`.

### TEXT_STATS

Input:

```json
{"text": "One line.\nSecond line."}
```

Result:

```json
{"character_count":22,"word_count":4,"line_count":2}
```

This task is executed by the Java executor through `python_tasks/run_task.py`.

### KEYWORD_COUNT

Input:

```json
{"text": "Java agents call Python agents", "keyword": "agents"}
```

Result:

```json
{"keyword":"agents","count":2}
```

This task is executed by the Java executor through `python_tasks/run_task.py`.

## Failure Path

Unsupported task types are marked `FAILED`.

Tasks created with `max_attempts` greater than `1` retry unsupported or invalid
execution until the attempt limit is reached.

Verified example:

```text
task_type: UNKNOWN_TASK
status: FAILED
error: Unsupported task type: UNKNOWN_TASK
```
