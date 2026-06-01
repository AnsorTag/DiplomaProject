# AGENTS.md

## Project Context

This repository is for my diploma project:

**Design and Implementation of a Multi-Agent Task Scheduling and Execution System**

The project has recently changed direction. It was previously closer to a Python/Docker distributed worker system. The current version is a multi-agent task scheduling and execution system using Java + JADE for the agent layer, while Python is still used for selected task execution modules.

The goal is to build the project carefully, module by module, not all at once.

Do not generate the entire project in one response or one large commit.

## Current Development Rule

Work incrementally.

For every requested change:

1. Inspect the existing repository first.
2. Explain what already exists.
3. Identify what is relevant, obsolete, or unclear.
4. Propose a small next step.
5. Modify only the requested module.
6. Do not rewrite unrelated parts of the project.
7. Do not delete files unless explicitly confirmed.

The project should stay understandable for a final-year CS student.

## Existing PostgreSQL

A PostgreSQL server already exists and is running.

Do not create a completely new database setup unless asked.

The first backend task will be to reconnect the project to the existing PostgreSQL server.

Before changing database code:

1. Look for existing database configuration files.
2. Look for existing `.env`, config, or connection settings.
3. Look for existing models, migrations, or SQL scripts.
4. Identify whether they can be reused.
5. Ask before replacing the database structure.

Expected task table fields may include:

```text
id
task_type
input_data
priority
status
assigned_agent
result
error
created_at
started_at
finished_at
