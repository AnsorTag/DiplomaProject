import sys
from pathlib import Path

from sqlalchemy import text

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from db.database import engine


SQL = """
update public.tasks
set task_type = 'GENERIC_TASK'
where task_type = 'LEGACY_RQ_TASK'
returning id
"""


def main() -> None:
    with engine.begin() as connection:
        rows = connection.execute(text(SQL)).mappings().all()

    print(f"Updated legacy task_type rows: {len(rows)}")
    if rows:
        ids = ", ".join(str(row["id"]) for row in rows)
        print(f"Task ids: {ids}")


if __name__ == "__main__":
    main()
