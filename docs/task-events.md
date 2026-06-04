# Task Event History

Structured lifecycle events are stored in `public.task_events`.

The PostgreSQL trigger `task_status_event_trigger` records events in the same
transaction as task creation or status changes. This covers Java agents, Python
maintenance scripts, and future tools that update `public.tasks`.

## Event Fields

- `id` - event id.
- `task_id` - related task id.
- `event_type` - normalized lifecycle event.
- `from_status` - previous task status, or `null` for creation.
- `to_status` - resulting task status.
- `agent_name` - executor associated with the transition when available.
- `details` - structured JSON metadata.
- `created_at` - event timestamp.

## Event Types

- `CREATED`
- `ASSIGNED`
- `STARTED`
- `COMPLETED`
- `FAILED`
- `CANCELLED`
- `RECOVERED`
- `STATUS_CHANGED` for an otherwise unrecognized status transition

Existing tasks are not backfilled. Events begin after the migration installs the
trigger.

## Inspect Events

List recent events:

```bash
./venv/bin/python scripts/list_task_events.py --limit 20
```

List one task's history:

```bash
./venv/bin/python scripts/list_task_events.py --task-id 1
```
