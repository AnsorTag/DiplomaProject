import json
import sys


class TaskExecutionError(Exception):
    pass


def execute_python_echo(input_data: dict) -> str:
    message = input_data.get("message")
    if not isinstance(message, str):
        raise TaskExecutionError("PYTHON_ECHO task requires input_data.message as text")
    return message


def execute(task_type: str, input_data: dict) -> str:
    if task_type == "PYTHON_ECHO":
        return execute_python_echo(input_data)
    raise TaskExecutionError(f"Unsupported Python task type: {task_type}")


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: run_task.py <task_type> <input_json>", file=sys.stderr)
        return 2

    task_type = sys.argv[1]
    try:
        input_data = json.loads(sys.argv[2])
    except json.JSONDecodeError as error:
        print(f"Invalid input JSON: {error}", file=sys.stderr)
        return 2

    if not isinstance(input_data, dict):
        print("Input JSON must be an object", file=sys.stderr)
        return 2

    try:
        print(execute(task_type, input_data))
        return 0
    except TaskExecutionError as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
