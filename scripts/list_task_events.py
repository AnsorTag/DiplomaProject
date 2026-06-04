import argparse
import json
import sys
from datetime import date, datetime
from pathlib import Path

from sqlalchemy import desc, select

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from db.database import SessionLocal
from db.models import TaskEvent


def normalize(value):
    if isinstance(value, (datetime, date)):
        return value.isoformat()
    return value


def event_to_dict(event: TaskEvent) -> dict:
    return {
        "id": event.id,
        "task_id": event.task_id,
        "event_type": event.event_type,
        "from_status": event.from_status,
        "to_status": event.to_status,
        "agent_name": event.agent_name,
        "details": event.details,
        "created_at": normalize(event.created_at),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="List structured task lifecycle events.")
    parser.add_argument("--task-id", type=int, help="Only show events for one task.")
    parser.add_argument("--limit", type=int, default=50, help="Maximum number of events to show.")
    args = parser.parse_args()

    if args.task_id is not None and args.task_id <= 0:
        raise SystemExit("--task-id must be positive")
    if args.limit <= 0:
        raise SystemExit("--limit must be positive")

    statement = select(TaskEvent).order_by(desc(TaskEvent.created_at), desc(TaskEvent.id)).limit(args.limit)
    if args.task_id is not None:
        statement = statement.where(TaskEvent.task_id == args.task_id)

    with SessionLocal() as session:
        events = session.execute(statement).scalars().all()

    for event in events:
        print(json.dumps(event_to_dict(event), ensure_ascii=True, sort_keys=True))


if __name__ == "__main__":
    main()
