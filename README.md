# Multi-Agent Task Scheduling and Execution System

This diploma project implements a task scheduler using Java, JADE agents, Python
task modules, and PostgreSQL.

A coordinator agent reads pending tasks from PostgreSQL and assigns them to the
least-loaded configured executor. Executor agents run Java or Python-backed task
types, persist results, and apply a finite retry policy. PostgreSQL records task
lifecycle events through a trigger.

## Architecture

```text
PostgreSQL
    |
    v
JADE Coordinator -> JADE Executor Agents -> Java task execution
                                      |
                                      +-> Python task modules
```

- `agent-system/` - Java 17, Maven, and JADE agent layer.
- `db/` - Python SQLAlchemy models and task repository helpers.
- `python_tasks/` - Python-backed task execution modules.
- `scripts/` - migration, monitoring, demo, maintenance, and test commands.
- `web/` - optional FastAPI web dashboard for demos and screenshots.
- `docs/` - detailed contracts and component documentation.
- `archive/` - archived files from the previous project direction.

## Task Lifecycle

Normal execution:

```text
PENDING -> ASSIGNED -> RUNNING -> COMPLETED
```

Failed executions return to `PENDING` while attempts remain. A failure at the
configured attempt limit becomes terminal `FAILED`.

PostgreSQL records lifecycle events such as `CREATED`, `ASSIGNED`, `STARTED`,
`COMPLETED`, `RETRY_SCHEDULED`, `RECOVERED`, and `FAILED`.

## Current Platform Support

The documented development and demo workflow currently supports Linux.

- Commands use Bash scripts and Linux virtual-environment paths.
- Environment variables are loaded with `source .env`.
- Docker Compose uses host networking so containers can reach PostgreSQL at
  `localhost`.

The Java, Python, and PostgreSQL application code is mostly portable, but the
repository does not yet include Windows PowerShell scripts or Windows-specific
Docker networking instructions.

## Prerequisites

- Java 17 JDK
- Maven
- Python 3
- PostgreSQL
- Docker and Docker Compose, only for the optional containerized agent runtime

Verify Java and Maven:

```bash
java -version
javac -version
mvn -version
```

## PostgreSQL Setup

The application uses a PostgreSQL database named `tasks`. Connection properties
in `.env` only tell the application how to connect; they do not install
PostgreSQL or create the database.

### Initialize a Fresh Local Database

The following is an Ubuntu/Debian Linux example. Installation commands differ
between Linux distributions.

Install and start PostgreSQL:

```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl enable --now postgresql
```

Open PostgreSQL as its administrative user:

```bash
sudo -u postgres psql
```

Create a project user and database:

```sql
create role diploma_user login password 'choose-a-password';
create database tasks owner diploma_user;
\quit
```

Configure `.env` with the same user and password, then run the migration
described below. The migration creates and completes the current schema.

## Local Setup

Create the Python virtual environment and install dependencies:

```bash
python3 -m venv venv
./venv/bin/pip install -r requirements.txt
```

If `.env` does not already exist, create a local ignored copy from
`.env.example` and configure the existing PostgreSQL connection:

```bash
cp .env.example .env
```

Relevant settings:

```text
DATABASE_URL=postgresql://diploma_user:YOUR_PASSWORD@localhost:5432/tasks
DB_JDBC_URL=jdbc:postgresql://localhost:5432/tasks
DB_USER=diploma_user
DB_PASSWORD=YOUR_PASSWORD
STALE_TASK_MINUTES
PYTHON_EXECUTABLE
PYTHON_TASK_RUNNER
```

Do not commit `.env`.

Apply the idempotent schema migration:

```bash
./venv/bin/python scripts/migrate_tasks_multi_agent.py
```

This creates or updates the task schema and event trigger. It does not install
PostgreSQL or create the `tasks` database itself.

Check Python and Java database access:

```bash
./venv/bin/python scripts/check_db_connection.py

set -a
source .env
set +a
cd agent-system
mvn exec:java -Dexec.mainClass=com.diplomawork.agents.db.DatabaseConnectionCheck
cd ..
```

Python tools load the root `.env` automatically. Agent and integration-test
scripts source it before starting Java. Direct Maven database commands require
the environment export shown above.

## Run the System

Create one task for each supported task type:

```bash
scripts/demo_create_tasks.sh
```

Start two executors and one coordinator:

```bash
scripts/run_agents.sh
```

Run a bounded demo:

```bash
RUN_TIMEOUT_SECONDS=30 scripts/run_agents.sh
```

Monitor tasks and inspect lifecycle events:

```bash
./venv/bin/python scripts/monitor_tasks.py --watch --interval 1 --limit 10
./venv/bin/python scripts/list_task_events.py --limit 20
```

Run the optional web dashboard:

```bash
scripts/run_web.sh
```

Open `http://127.0.0.1:8000`. The dashboard reads the same PostgreSQL task
schema, creates new tasks, and shows lifecycle events. It does not schedule or
execute tasks; JADE agents remain responsible for that.

## Supported Task Types

| Task type | Runtime | Purpose |
| --- | --- | --- |
| `ECHO` | Java | Returns `input_data.message`. |
| `TEXT_SUMMARY` | Java | Normalizes and truncates input text. |
| `PYTHON_ECHO` | Python | Returns `input_data.message` through the Python bridge. |
| `TEXT_STATS` | Python | Counts characters, words, and lines. |
| `KEYWORD_COUNT` | Python | Counts case-insensitive whole-word matches. |

Create a task manually:

```bash
./venv/bin/python scripts/create_task.py TEXT_SUMMARY \
  --input-json '{"text":"Example task text","max_length":120}' \
  --priority 5 \
  --max-attempts 2
```

## Tests

Run Java unit tests:

```bash
cd agent-system
mvn test
cd ..
```

The PostgreSQL lifecycle integration suite is opt-in and cleans up its own test
rows:

```bash
scripts/run_integration_tests.sh
```

Run Python task-module tests:

```bash
./venv/bin/python -m unittest discover -s tests -v
```

## Docker

Docker packages the agent runtime but continues to use the existing host
PostgreSQL server.

```bash
docker compose build
docker compose up agents
```

For a bounded Docker demo:

```bash
RUN_TIMEOUT_SECONDS=30 docker compose up agents
```

The Compose configuration uses host networking for the local Linux demo.

## Maintenance Commands

Preview stale active tasks:

```bash
./venv/bin/python scripts/reset_stale_tasks.py --minutes 30
```

Resolve them after reviewing the preview:

```bash
./venv/bin/python scripts/reset_stale_tasks.py --minutes 30 --apply
```

Preview or archive legacy pending placeholder tasks:

```bash
./venv/bin/python scripts/archive_legacy_tasks.py
./venv/bin/python scripts/archive_legacy_tasks.py --apply
```

These commands preserve task history instead of deleting normal project data.

## Documentation

- [Demo flow](docs/demo-flow.md)
- [Task contract](docs/task-contract.md)
- [Task event history](docs/task-events.md)
- [JADE coordinator](docs/jade-coordinator.md)
- [JADE executor](docs/jade-executor.md)
- [Java database access](docs/java-database.md)
- [Python task modules](docs/python-task-modules.md)
- [Web dashboard](docs/web-dashboard.md)
- [Docker setup](docs/docker.md)
