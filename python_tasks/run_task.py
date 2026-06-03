import json
import re
import sys


class TaskExecutionError(Exception):
    pass


def execute_python_echo(input_data: dict) -> str:
    message = input_data.get("message")
    if not isinstance(message, str):
        raise TaskExecutionError("PYTHON_ECHO task requires input_data.message as text")
    return message


def execute_text_stats(input_data: dict) -> str:
    text = input_data.get("text")
    if not isinstance(text, str):
        raise TaskExecutionError("TEXT_STATS task requires input_data.text as text")

    words = re.findall(r"\b\w+\b", text)
    stats = {
        "character_count": len(text),
        "word_count": len(words),
        "line_count": len(text.splitlines()) if text else 0,
    }
    return json.dumps(stats, separators=(",", ":"))


def execute_keyword_count(input_data: dict) -> str:
    text = input_data.get("text")
    keyword = input_data.get("keyword")
    if not isinstance(text, str):
        raise TaskExecutionError("KEYWORD_COUNT task requires input_data.text as text")
    if not isinstance(keyword, str) or not keyword:
        raise TaskExecutionError("KEYWORD_COUNT task requires input_data.keyword as non-empty text")

    count = len(re.findall(rf"\b{re.escape(keyword)}\b", text, flags=re.IGNORECASE))
    result = {
        "keyword": keyword,
        "count": count,
    }
    return json.dumps(result, separators=(",", ":"))


def execute(task_type: str, input_data: dict) -> str:
    if task_type == "PYTHON_ECHO":
        return execute_python_echo(input_data)
    if task_type == "TEXT_STATS":
        return execute_text_stats(input_data)
    if task_type == "KEYWORD_COUNT":
        return execute_keyword_count(input_data)
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
