# Project Structure

The project is transitioning from a Python/FastAPI + Redis/RQ worker prototype to a Java + JADE multi-agent task scheduling and execution system.

## Active Areas

- `agent-system/` - future Java + JADE multi-agent layer.
- `db/` - existing SQLAlchemy/PostgreSQL prototype configuration and task model.
- `docs/` - project documentation and orientation notes.

## Archived Areas

- `archive/old-python-rq-prototype/` - previous FastAPI + Redis/RQ queue prototype.

## Pending Decisions

- Whether Java build tooling should use Maven or Gradle.
- How the JADE agent layer should connect to the existing PostgreSQL server.
- Which Python task execution modules are still needed.
- Whether the current SQLAlchemy `Task` model should remain the database source of truth or become only a Python-side helper.
