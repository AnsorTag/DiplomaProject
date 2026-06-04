# Demo Flow

This flow demonstrates the current multi-agent scheduler without creating a new database setup.

It assumes:

- PostgreSQL is already running.
- The existing `public.tasks` table is available.
- The Python virtual environment exists at `venv/`.
- Java and Maven are installed.

After pulling schema-related changes, run the idempotent migration:

```bash
./venv/bin/python scripts/migrate_tasks_multi_agent.py
```

## 1. Check the Current Queue

```bash
./venv/bin/python scripts/monitor_tasks.py --limit 10
```

Use this before a demo to see whether old pending or active tasks are still present.

## 2. Archive Legacy Placeholder Tasks

Dry run:

```bash
./venv/bin/python scripts/archive_legacy_tasks.py
```

Apply:

```bash
./venv/bin/python scripts/archive_legacy_tasks.py --apply
```

This marks pending legacy `GENERIC_TASK` rows as `CANCELLED`. It does not delete rows.

## 3. Reset Stale Active Tasks

Dry run:

```bash
./venv/bin/python scripts/reset_stale_tasks.py --minutes 30
```

Apply:

```bash
./venv/bin/python scripts/reset_stale_tasks.py --minutes 30 --apply
```

This resets stale `ASSIGNED` or `RUNNING` tasks back to `PENDING`. `ASSIGNED` age is
measured from `assigned_at`; `RUNNING` age is measured from `started_at`.

## 4. Create Demo Tasks

```bash
scripts/demo_create_tasks.sh
```

This creates one task for each supported task type:

- `ECHO`
- `TEXT_SUMMARY`
- `PYTHON_ECHO`
- `TEXT_STATS`
- `KEYWORD_COUNT`

## 5. Run the Agents

For a normal run:

```bash
scripts/run_agents.sh
```

For a bounded demo run:

```bash
RUN_TIMEOUT_SECONDS=30 scripts/run_agents.sh
```

The script starts:

- `executor1`
- `executor2`
- `coordinator`

The coordinator polls PostgreSQL, assigns pending tasks, and sends JADE assignment messages to executors.
It also performs one automatic stale-task recovery pass on startup using
`STALE_TASK_MINUTES`, which defaults to 30.

## 6. Monitor Results

One-shot view:

```bash
./venv/bin/python scripts/monitor_tasks.py --limit 10
```

Watch mode:

```bash
./venv/bin/python scripts/monitor_tasks.py --watch --interval 1 --limit 10
```

Expected result after a successful demo:

- demo tasks move from `PENDING` to `ASSIGNED`;
- executors mark them `RUNNING`;
- completed tasks end as `COMPLETED`;
- `result` contains the task output.

Inspect recent lifecycle events:

```bash
./venv/bin/python scripts/list_task_events.py --limit 20
```

A successful task should have `CREATED`, `ASSIGNED`, `STARTED`, and `COMPLETED`
events.

## Recovery Notes

If a JADE run is interrupted, some tasks may remain `ASSIGNED` or `RUNNING`.

Use:

```bash
./venv/bin/python scripts/reset_stale_tasks.py --minutes 1
```

Then apply only after reviewing the listed tasks:

```bash
./venv/bin/python scripts/reset_stale_tasks.py --minutes 1 --apply
```

If old prototype rows appear as pending `GENERIC_TASK`, archive them instead of deleting them:

```bash
./venv/bin/python scripts/archive_legacy_tasks.py --apply
```
