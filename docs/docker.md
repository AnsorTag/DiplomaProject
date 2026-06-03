# Docker Setup

Docker packaging is optional. It packages the Java JADE agents and Python task modules, but it does not create a new PostgreSQL database.

The container is expected to connect to the existing PostgreSQL server through environment variables.

## Files

- `Dockerfile` - builds the Java/Python agent runtime.
- `docker-compose.yml` - starts the agent runtime as one service.
- `.dockerignore` - excludes local virtualenvs, build output, Git data, and old archived prototype files from the image context.

## PostgreSQL Connection

The compose file uses host networking for local Linux demos and defaults to connecting to PostgreSQL on the host machine:

```text
DB_JDBC_URL=jdbc:postgresql://localhost:5432/tasks
DATABASE_URL=postgresql://DB_USER:DB_PASSWORD@localhost:5432/tasks
DB_USER=postgres
DB_PASSWORD=your_db_password
```

This matches the existing local development defaults and works when PostgreSQL is already available on the host at `localhost:5432`.

If your PostgreSQL server uses different credentials, create a local `.env` file and override the values there. Do not commit `.env`.

## Build

```bash
docker compose build
```

## Run Agents

Normal run:

```bash
docker compose up agents
```

Bounded demo run:

```bash
RUN_TIMEOUT_SECONDS=30 docker compose up agents
```

The container runs:

```bash
scripts/run_agents.sh
```

That starts:

- `executor1`
- `executor2`
- `coordinator`

## Notes

- PostgreSQL is not containerized here.
- The existing `public.tasks` table is reused.
- The compose service uses `network_mode: host` so the container can reach the host PostgreSQL service through `localhost`.
- Python task modules run inside the container through `/app/venv/bin/python`.
- Java reads PostgreSQL through `DB_JDBC_URL`, `DB_USER`, and `DB_PASSWORD`.
