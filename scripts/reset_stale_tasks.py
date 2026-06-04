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


def find_stale_tasks(session, cutoff: datetime, lock: bool = False):
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
    if lock:
        statement = statement.with_for_update(skip_locked=True)
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
    print("ID    STATUS     TYPE            ATTEMPT  AGENT         ASSIGNED                        STARTED")
    print("---------------------------------------------------------------------------------------------------")
    for task in tasks:
        print(
            f"{task.id:<5} "
            f"{task.status:<10} "
            f"{task.task_type:<15} "
            f"{f'{task.attempt_count}/{task.max_attempts}':<8} "
            f"{str(task.assigned_agent or '-'):<13} "
            f"{format_time(task.assigned_at):<31} "
            f"{format_time(task.started_at)}"
        )


def resolve_tasks(session, tasks) -> tuple[int, int]:
    recovered = 0
    failed = 0
    for task in tasks:
        if task.attempt_count < task.max_attempts:
            task.status = "PENDING"
            task.assigned_agent = None
            task.assigned_at = None
            task.started_at = None
            task.finished_at = None
            task.result = None
            task.error = None
            recovered += 1
        else:
            task.status = "FAILED"
            task.finished_at = datetime.now(timezone.utc)
            task.error = "Stale task exhausted maximum attempts"
            failed += 1
    session.commit()
    return recovered, failed


def main() -> None:
    parser = argparse.ArgumentParser(description="Resolve stale ASSIGNED/RUNNING tasks.")
    parser.add_argument("--minutes", type=int, default=30, help="Minimum age in minutes before a task is stale.")
    parser.add_argument("--apply", action="store_true", help="Apply resolution. Without this flag, only prints matches.")
    args = parser.parse_args()

    if args.minutes <= 0:
        raise SystemExit("--minutes must be positive")

    cutoff = stale_cutoff(args.minutes)
    with SessionLocal() as session:
        tasks = find_stale_tasks(session, cutoff, lock=args.apply)
        print(f"Cutoff: active timestamp before {cutoff.isoformat()}")
        print_tasks(tasks)

        if not tasks:
            return

        if not args.apply:
            print()
            print("Dry run only. Re-run with --apply to recover or fail these stale tasks.")
            return

        recovered, failed = resolve_tasks(session, tasks)
        print()
        print(f"Recovered {recovered} task(s) to PENDING.")
        print(f"Marked {failed} exhausted task(s) FAILED.")


if __name__ == "__main__":
    main()
