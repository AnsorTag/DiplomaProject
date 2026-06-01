# Agent System

This module is reserved for the Java + JADE multi-agent layer of the diploma project.

Current intended responsibility:

- accept task records from the backend/database layer;
- schedule tasks between agents;
- assign work to execution agents;
- track task lifecycle state;
- delegate selected execution work to Python modules when needed.

This folder is intentionally minimal for now. Build tooling, JADE dependencies, and database integration should be added in small steps after the existing PostgreSQL connection is reviewed.
