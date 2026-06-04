import argparse
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

from sqlalchemy import and_, func, or_, select

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from db.database import SessionLocal
from db.models import Task


def stale_cutoff(minutes: int) -> datetime:
    return datetime.now(timezone.utc) - timedelta(minutes=minutes)


def find_stale_tasks(session, cutoff: datetime):
    statement = (
        select(Task)
        .where(
            or_(
                and_(
                    Task.status == "ASSIGNED",
                    func.coalesce(Task.assigned_at, Task.created_at) < cutoff,
                ),
                and_(
                    Task.status == "RUNNING",
                    func.coalesce(Task.started_at, Task.assigned_at, Task.created_at) < cutoff,
                ),
            )
        )
        .order_by(Task.created_at.asc(), Task.id.asc())
    )
    return session.execute(statement).scalars().all()


def format_time(value) -> str:
    if value is None:
        return "-"
    return value.isoformat()


def print_tasks(tasks) -> None:
    if not tasks:
        print("No stale ASSIGNED/RUNNING tasks found.")
        return

    print("Stale tasks:")
    print("ID    STATUS     TYPE            AGENT         ASSIGNED                        STARTED")
    print("------------------------------------------------------------------------------------------")
    for task in tasks:
        print(
            f"{task.id:<5} "
            f"{task.status:<10} "
            f"{task.task_type:<15} "
            f"{str(task.assigned_agent or '-'):<13} "
            f"{format_time(task.assigned_at):<31} "
            f"{format_time(task.started_at)}"
        )


def reset_tasks(session, tasks) -> None:
    for task in tasks:
        task.status = "PENDING"
        task.assigned_agent = None
        task.assigned_at = None
        task.started_at = None
        task.finished_at = None
        task.result = None
        task.error = None
    session.commit()


def main() -> None:
    parser = argparse.ArgumentParser(description="Reset stale ASSIGNED/RUNNING tasks back to PENDING.")
    parser.add_argument("--minutes", type=int, default=30, help="Minimum age in minutes before a task is stale.")
    parser.add_argument("--apply", action="store_true", help="Apply the reset. Without this flag, only prints matches.")
    args = parser.parse_args()

    if args.minutes <= 0:
        raise SystemExit("--minutes must be positive")

    cutoff = stale_cutoff(args.minutes)
    with SessionLocal() as session:
        tasks = find_stale_tasks(session, cutoff)
        print(f"Cutoff: active timestamp before {cutoff.isoformat()}")
        print_tasks(tasks)

        if not tasks:
            return

        if not args.apply:
            print()
            print("Dry run only. Re-run with --apply to reset these tasks to PENDING.")
            return

        reset_tasks(session, tasks)
        print()
        print(f"Reset {len(tasks)} task(s) to PENDING.")


if __name__ == "__main__":
    main()
