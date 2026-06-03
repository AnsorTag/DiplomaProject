# Python Task Modules

Python-backed task execution is exposed through a small command-line bridge:

- `python_tasks/run_task.py`

The Java executor calls this script through `PythonTaskRunner`.

## Environment Variables

Defaults are relative to the `agent-system/` directory because Maven runs from there:

```text
PYTHON_EXECUTABLE=../venv/bin/python
PYTHON_TASK_RUNNER=../python_tasks/run_task.py
```

## Supported Python Task Types

### PYTHON_ECHO

Input:

```json
{"message": "hello from Python"}
```

Result:

```text
hello from Python
```
