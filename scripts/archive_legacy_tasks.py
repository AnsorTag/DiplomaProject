import argparse
import sys
from datetime import datetime, timezone
from pathlib import Path

from sqlalchemy import select

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from db.database import SessionLocal
from db.models import Task


LEGACY_TASK_TYPE = "GENERIC_TASK"
ARCHIVE_STATUS = "CANCELLED"
ARCHIVE_NOTE = "Archived legacy generic task"


def find_legacy_tasks(session):
    statement = (
        select(Task)
        .where(Task.task_type == LEGACY_TASK_TYPE)
        .where(Task.status == "PENDING")
        .order_by(Task.created_at.asc(), Task.id.asc())
    )
    return session.execute(statement).scalars().all()


def format_time(value) -> str:
    if value is None:
        return "-"
    return value.isoformat()


def print_tasks(tasks) -> None:
    if not tasks:
        print("No pending legacy GENERIC_TASK rows found.")
        return

    print("Pending legacy tasks:")
    print("ID    STATUS     TYPE            AGENT         CREATED")
    print("--------------------------------------------------------------------------")
    for task in tasks:
        print(
            f"{task.id:<5} "
            f"{task.status:<10} "
            f"{task.task_type:<15} "
            f"{str(task.assigned_agent or '-'):<13} "
            f"{format_time(task.created_at)}"
        )


def archive_tasks(session, tasks) -> None:
    now = datetime.now(timezone.utc)
    for task in tasks:
        task.status = ARCHIVE_STATUS
        task.assigned_agent = None
        task.started_at = None
        task.finished_at = now
        task.error = ARCHIVE_NOTE
    session.commit()


def main() -> None:
    parser = argparse.ArgumentParser(description="Archive pending legacy GENERIC_TASK rows.")
    parser.add_argument("--apply", action="store_true", help="Apply the archive. Without this flag, only prints matches.")
    args = parser.parse_args()

    with SessionLocal() as session:
        tasks = find_legacy_tasks(session)
        print_tasks(tasks)

        if not tasks:
            return

        if not args.apply:
            print()
            print("Dry run only. Re-run with --apply to mark these tasks CANCELLED.")
            return

        archive_tasks(session, tasks)
        print()
        print(f"Archived {len(tasks)} legacy task(s).")


if __name__ == "__main__":
    main()
