# Task Contract

This document defines the current task contract for the multi-agent scheduler.

Tasks are stored in the existing PostgreSQL `public.tasks` table. The JADE coordinator assigns pending tasks to executor agents, and executors update task status as work progresses.

## Task Fields

Active fields:

- `id` - database task id.
- `task_type` - task execution type.
- `input_data` - JSONB input payload.
- `priority` - higher values are scheduled first.
- `status` - lifecycle state.
- `assigned_agent` - executor agent currently responsible for the task.
- `result` - successful execution output.
- `error` - failure or archive reason.
- `created_at` - task creation timestamp.
- `started_at` - execution start timestamp.
- `finished_at` - terminal-state timestamp.

Legacy field:

- `job_id` - kept only so old prototype rows remain readable.

## Statuses

Valid statuses:

- `PENDING` - waiting for assignment.
- `ASSIGNED` - claimed by the coordinator for an executor.
- `RUNNING` - executor has started work.
- `COMPLETED` - task finished successfully.
- `FAILED` - task execution failed.
- `CANCELLED` - task was intentionally archived or skipped.

Terminal statuses:

- `COMPLETED`
- `FAILED`
- `CANCELLED`

## Lifecycle

Expected lifecycle:

```text
PENDING -> ASSIGNED -> RUNNING -> COMPLETED
```

Failure lifecycle:

```text
PENDING -> ASSIGNED -> RUNNING -> FAILED
```

Archive lifecycle:

```text
PENDING -> CANCELLED
```

The coordinator only assigns `PENDING` tasks. Executors should only execute tasks assigned to their own agent name.

## Scheduling Rules

The coordinator:

1. reads pending tasks from PostgreSQL;
2. reads configured executor agent names from JADE arguments;
3. chooses the least-loaded executor by active `ASSIGNED` and `RUNNING` task counts;
4. atomically assigns one pending task to the selected executor;
5. sends a JADE `REQUEST` message with conversation id `task-assignment`.

Pending tasks are ordered by priority before creation time.

## Supported Task Types

### ECHO

Executor:

- Java

Input:

```json
{"message": "hello"}
```

Rules:

- `input_data.message` is required.
- `message` must be text.

Result:

```text
hello
```

### TEXT_SUMMARY

Executor:

- Java

Input:

```json
{"text": "long text to summarize", "max_length": 120}
```

Rules:

- `input_data.text` is required.
- `text` must be text.
- `input_data.max_length` is optional.
- `max_length` must be a positive integer when provided.
- default `max_length` is `120`.

Result:

```text
The normalized text truncated to max_length characters.
```

### PYTHON_ECHO

Executor:

- Java executor through `python_tasks/run_task.py`

Input:

```json
{"message": "hello from Python"}
```

Rules:

- `input_data.message` is required.
- `message` must be text.

Result:

```text
hello from Python
```

### TEXT_STATS

Executor:

- Java executor through `python_tasks/run_task.py`

Input:

```json
{"text": "One line.\nSecond line."}
```

Rules:

- `input_data.text` is required.
- `text` must be text.

Result:

```json
{"character_count":22,"word_count":4,"line_count":2}
```

## Error Behavior

If input validation fails, the executor marks the task `FAILED` and stores the error message in `error`.

If the task type is unsupported, the executor marks the task `FAILED`.

Example:

```text
task_type: UNKNOWN_TASK
status: FAILED
error: Unsupported task type: UNKNOWN_TASK
```

## Maintenance Behavior

Development maintenance scripts use terminal states instead of deleting old rows:

- `scripts/reset_stale_tasks.py` resets stale `ASSIGNED` or `RUNNING` tasks back to `PENDING`.
- `scripts/archive_legacy_tasks.py` marks pending legacy `GENERIC_TASK` rows as `CANCELLED`.
