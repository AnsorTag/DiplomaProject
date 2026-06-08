# Web Dashboard

The web dashboard is an optional FastAPI interface for demonstrations,
screenshots, and manual task creation.

## Files

- `web/app.py` - FastAPI routes and database queries.
- `web/templates/` - Jinja templates.
- `web/static/styles.css` - dashboard styling.
- `scripts/run_web.sh` - local development launcher.

## Scope

The dashboard:

- shows task counts by status;
- shows active executor workloads;
- lists recent lifecycle events;
- lists and filters tasks;
- shows task input, result, error, and event timeline;
- creates supported task types;
- creates the standard demo task set.

The dashboard does not replace JADE scheduling or execution. It only observes
and inserts task rows in PostgreSQL.

## Run

Install dependencies:

```bash
./venv/bin/pip install -r requirements.txt
```

Start the dashboard:

```bash
scripts/run_web.sh
```

Open:

```text
http://127.0.0.1:8000
```

Use `WEB_HOST` and `WEB_PORT` to override the default bind address:

```bash
WEB_PORT=8080 scripts/run_web.sh
```
