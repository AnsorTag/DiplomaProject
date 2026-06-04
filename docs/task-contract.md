# Task Contract

This document defines the current task contract for the multi-agent scheduler.

Tasks are stored in the existing PostgreSQL `public.tasks` table. The JADE coordinator assigns pending tasks to executor agents, and executors update task status as work progresses.

Lifecycle transitions are recorded in `public.task_events`.

## Task Fields

Active fields:

- `id` - database task id.
- `task_type` - task execution type.
- `input_data` - JSONB input payload.
- `priority` - higher values are scheduled first.
- `status` - lifecycle state.
- `assigned_agent` - executor agent currently responsible for the task.
- `attempt_count` - execution attempts already started.
- `max_attempts` - maximum execution attempts allowed.
- `result` - successful execution output.
- `error` - failure or archive reason.
- `created_at` - task creation timestamp.
- `assigned_at` - most recent assignment timestamp.
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

Retry lifecycle:

```text
PENDING -> ASSIGNED -> RUNNING -> PENDING
```

Archive lifecycle:

```text
PENDING -> CANCELLED
```

The coordinator only assigns `PENDING` tasks. Executors should only execute tasks assigned to their own agent name.

## Retry Rules

- `attempt_count` increments when an executor marks a task `RUNNING`.
- `max_attempts` defaults to `1`, preserving single-attempt behavior.
- an execution failure returns the task to `PENDING` while `attempt_count < max_attempts`;
- a failure at the attempt limit marks the task terminal `FAILED`;
- exhausted `PENDING` tasks are not eligible for assignment.

Create a retryable task with:

```bash
./venv/bin/python scripts/create_task.py UNKNOWN_TASK --max-attempts 2
```

## Event History

PostgreSQL records task creation and every status transition in `public.task_events`
within the same transaction as the task change.

Expected successful lifecycle events:

```text
CREATED -> ASSIGNED -> STARTED -> COMPLETED
```

Recovery records `RECOVERED` when an `ASSIGNED` or `RUNNING` task returns to
`PENDING`. See `docs/task-events.md` for the complete event contract and inspection
commands.

Execution failure records `RETRY_SCHEDULED` when another attempt remains.

## Scheduling Rules

The coordinator:

1. reads pending tasks from PostgreSQL;
2. reads configured executor agent names from JADE arguments;
3. chooses the least-loaded executor by active `ASSIGNED` and `RUNNING` task counts;
4. atomically assigns one pending task to the selected executor;
5. sends a JADE `REQUEST` message with conversation id `task-assignment`.

Pending tasks are ordered by priority before creation time.

On startup, the coordinator resolves stale `ASSIGNED` and `RUNNING` tasks.
`ASSIGNED` age is measured from `assigned_at`; `RUNNING` age is measured from
`started_at`. Legacy rows fall back to their available assignment or creation timestamp.
Tasks with attempts remaining return to `PENDING`; exhausted tasks become `FAILED`.
The threshold defaults to 30 minutes through `STALE_TASK_MINUTES`; set it to `0` to
disable automatic recovery.

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
The normalized text truncated to at most max_length characters, including any
trailing ellipsis.
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

### KEYWORD_COUNT

Executor:

- Java executor through `python_tasks/run_task.py`

Input:

```json
{"text": "Java agents call Python agents", "keyword": "agents"}
```

Rules:

- `input_data.text` is required.
- `text` must be text.
- `input_data.keyword` is required.
- `keyword` must be non-empty text.
- matching is case-insensitive and counts whole-word matches.

Result:

```json
{"keyword":"agents","count":2}
```

## Error Behavior

If input validation fails, the executor stores the error message in `error`. The task
is retried when attempts remain, otherwise it becomes `FAILED`.

Unsupported task types follow the same retry policy.

Example:

```text
task_type: UNKNOWN_TASK
status: FAILED
error: Unsupported task type: UNKNOWN_TASK
```

## Maintenance Behavior

Development maintenance scripts use terminal states instead of deleting old rows:

- `scripts/reset_stale_tasks.py` previews or resolves stale `ASSIGNED` or `RUNNING` tasks.
- `scripts/archive_legacy_tasks.py` marks pending legacy `GENERIC_TASK` rows as `CANCELLED`.
