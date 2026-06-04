import argparse
import shutil
import sys
import time
from datetime import date, datetime
from pathlib import Path

from sqlalchemy import desc, func, select

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from db.database import SessionLocal
from db.models import Task


STATUS_ORDER = ["PENDING", "ASSIGNED", "RUNNING", "COMPLETED", "FAILED", "CANCELLED"]


def format_time(value: datetime | date | None) -> str:
    if value is None:
        return "-"
    if isinstance(value, datetime):
        return value.strftime("%Y-%m-%d %H:%M:%S")
    return value.isoformat()


def truncate(value: object, width: int) -> str:
    text = "-" if value is None else str(value)
    if len(text) <= width:
        return text
    return text[: max(0, width - 3)] + "..."


def count_by_status(session) -> dict[str, int]:
    rows = session.execute(select(Task.status, func.count()).group_by(Task.status)).all()
    return {status: count for status, count in rows}


def recent_tasks(session, limit: int):
    return session.execute(select(Task).order_by(desc(Task.created_at), desc(Task.id)).limit(limit)).scalars().all()


def print_summary(counts: dict[str, int]) -> None:
    known_parts = [f"{status}:{counts.get(status, 0)}" for status in STATUS_ORDER if counts.get(status, 0)]
    unknown_parts = [
        f"{status}:{count}"
        for status, count in sorted(counts.items())
        if status not in STATUS_ORDER
    ]
    parts = known_parts + unknown_parts
    print("Status counts: " + (" | ".join(parts) if parts else "no tasks"))


def print_table(tasks) -> None:
    columns = [
        ("ID", 5),
        ("TYPE", 14),
        ("STATUS", 10),
        ("PRIO", 4),
        ("TRY", 5),
        ("AGENT", 12),
        ("CREATED", 19),
        ("FINISHED", 19),
        ("RESULT/ERROR", 48),
    ]
    header = "  ".join(label.ljust(width) for label, width in columns)
    print(header)
    print("-" * len(header))

    for task in tasks:
        outcome = task.error if task.error else task.result
        values = [
            task.id,
            task.task_type,
            task.status,
            task.priority,
            f"{task.attempt_count}/{task.max_attempts}",
            task.assigned_agent,
            format_time(task.created_at),
            format_time(task.finished_at),
            outcome,
        ]
        print("  ".join(truncate(value, width).ljust(width) for value, (_, width) in zip(values, columns)))


def render(limit: int) -> None:
    with SessionLocal() as session:
        print_summary(count_by_status(session))
        print()
        print_table(recent_tasks(session, limit))


def clear_screen() -> None:
    print("\033[2J\033[H", end="")


def main() -> None:
    parser = argparse.ArgumentParser(description="Monitor tasks in the existing PostgreSQL database.")
    parser.add_argument("--limit", type=int, default=15, help="Number of recent tasks to display.")
    parser.add_argument("--watch", action="store_true", help="Refresh the monitor until interrupted.")
    parser.add_argument("--interval", type=float, default=2.0, help="Refresh interval in seconds for --watch.")
    args = parser.parse_args()

    if args.limit <= 0:
        raise SystemExit("--limit must be positive")
    if args.interval <= 0:
        raise SystemExit("--interval must be positive")

    if not args.watch:
        render(args.limit)
        return

    while True:
        if shutil.get_terminal_size(fallback=(80, 24)).columns > 0:
            clear_screen()
        render(args.limit)
        sys.stdout.flush()
        time.sleep(args.interval)


if __name__ == "__main__":
    main()
