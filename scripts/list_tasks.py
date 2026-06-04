import argparse
import json
import sys
from datetime import date, datetime
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from db.database import SessionLocal
from db.task_repository import list_tasks


def normalize(value):
    if isinstance(value, (datetime, date)):
        return value.isoformat()
    return value


def task_to_dict(task):
    return {
        "id": task.id,
        "task_type": task.task_type,
        "input_data": task.input_data,
        "priority": task.priority,
        "status": task.status,
        "assigned_agent": task.assigned_agent,
        "result": task.result,
        "error": task.error,
        "created_at": normalize(task.created_at),
        "assigned_at": normalize(task.assigned_at),
        "started_at": normalize(task.started_at),
        "finished_at": normalize(task.finished_at),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="List tasks from the existing PostgreSQL database.")
    parser.add_argument("--status", help="Optional status filter, for example PENDING or COMPLETED.")
    parser.add_argument("--limit", type=int, default=20, help="Maximum number of tasks to show.")
    args = parser.parse_args()

    db = SessionLocal()
    try:
        tasks = list_tasks(db, status=args.status, limit=args.limit)
        for task in tasks:
            print(json.dumps(task_to_dict(task), ensure_ascii=True, sort_keys=True))
    finally:
        db.close()


if __name__ == "__main__":
    main()
