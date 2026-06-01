# Task Repository

The Python task repository is a small compatibility layer around the existing PostgreSQL `public.tasks` table.

## Files

- `db/task_repository.py` - reusable task data access functions.
- `scripts/list_tasks.py` - read-only task listing CLI.
- `scripts/create_task.py` - task creation CLI.

## Current Contract

New tasks use these multi-agent fields:

- `task_type`
- `input_data`
- `priority`
- `status`
- `assigned_agent`
- `result`
- `error`
- lifecycle timestamps

The legacy `job_id` field is still present in the table and model so old prototype rows remain readable.

## Examples

List recent tasks:

```bash
./venv/bin/python scripts/list_tasks.py --limit 10
```

List pending tasks:

```bash
./venv/bin/python scripts/list_tasks.py --status PENDING
```

Create a task:

```bash
./venv/bin/python scripts/create_task.py TEXT_SUMMARY --input-json '{"text":"example"}' --priority 5
```
