import sys
from pathlib import Path

from sqlalchemy import text

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from db.database import engine


MIGRATION_SQL = """
alter table public.tasks
    add column if not exists task_type varchar(100),
    add column if not exists input_data jsonb,
    add column if not exists priority integer,
    add column if not exists assigned_agent varchar(100),
    add column if not exists assigned_at timestamp with time zone;

update public.tasks
set task_type = 'LEGACY_RQ_TASK'
where task_type is null;

update public.tasks
set input_data = '{}'::jsonb
where input_data is null;

update public.tasks
set priority = 5
where priority is null;

update public.tasks
set assigned_at = coalesce(started_at, created_at)
where status in ('ASSIGNED', 'RUNNING')
  and assigned_at is null;

alter table public.tasks
    alter column task_type set not null,
    alter column task_type set default 'GENERIC_TASK',
    alter column input_data set not null,
    alter column input_data set default '{}'::jsonb,
    alter column priority set not null,
    alter column priority set default 5,
    alter column status set default 'PENDING',
    alter column created_at set default now();

create index if not exists idx_tasks_status
    on public.tasks (status);

create index if not exists idx_tasks_priority
    on public.tasks (priority desc);

create index if not exists idx_tasks_assigned_agent
    on public.tasks (assigned_agent);

create index if not exists idx_tasks_assigned_at
    on public.tasks (assigned_at);
"""


VERIFY_SQL = """
select column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and table_name = 'tasks'
  and column_name in (
      'task_type',
      'input_data',
      'priority',
      'assigned_agent',
      'assigned_at',
      'status',
      'created_at'
  )
order by ordinal_position
"""


def main() -> None:
    with engine.begin() as connection:
        connection.execute(text(MIGRATION_SQL))
        columns = connection.execute(text(VERIFY_SQL)).mappings().all()

    print("Migration completed: public.tasks multi-agent columns are present.")
    print("Verified columns:")
    for column in columns:
        print(
            f"- {column['column_name']}: {column['data_type']} "
            f"nullable={column['is_nullable']} default={column['column_default']}"
        )


if __name__ == "__main__":
    main()
