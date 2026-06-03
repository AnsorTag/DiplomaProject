import argparse
import json
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from db.database import SessionLocal
from db.task_repository import create_task


def main() -> None:
    parser = argparse.ArgumentParser(description="Create a multi-agent task in PostgreSQL.")
    parser.add_argument("task_type", help="Task type, for example TEXT_SUMMARY or TEXT_STATS.")
    parser.add_argument(
        "--input-json",
        default="{}",
        help="Task input as a JSON object. Defaults to an empty object.",
    )
    parser.add_argument("--priority", type=int, default=5, help="Task priority. Higher is more important.")
    args = parser.parse_args()

    input_data = json.loads(args.input_json)
    if not isinstance(input_data, dict):
        raise ValueError("--input-json must be a JSON object")

    db = SessionLocal()
    try:
        task = create_task(
            db,
            task_type=args.task_type,
            input_data=input_data,
            priority=args.priority,
        )
        print(f"Created task {task.id} with status {task.status}")
    finally:
        db.close()


if __name__ == "__main__":
    main()
