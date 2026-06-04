# Java Task Contract

The initial Java model mirrors the current PostgreSQL `public.tasks` table after the additive multi-agent migration.

## Files

- `agent-system/src/main/java/com/diplomawork/agents/model/Task.java`
- `agent-system/src/main/java/com/diplomawork/agents/model/TaskStatus.java`

## Notes

- `TaskStatus` defines the scheduler lifecycle: `PENDING`, `ASSIGNED`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`.
- `Task.inputDataJson` stores the PostgreSQL `input_data` JSONB value as text for now.
- `Task.attemptCount` and `Task.maxAttempts` define the finite retry policy.

## Build

The `agent-system` module uses Maven and targets Java 17.

Compile it with:

```bash
cd agent-system
mvn compile
```
