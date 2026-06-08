import json
import sys
from datetime import date, datetime
from pathlib import Path
from typing import Any

from fastapi import FastAPI
from fastapi import Form
from fastapi import HTTPException
from fastapi import Request
from fastapi.responses import RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from sqlalchemy import desc
from sqlalchemy import func
from sqlalchemy import or_
from sqlalchemy import select

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from db.database import SessionLocal
from db.models import Task
from db.models import TaskEvent
from db.task_repository import create_task


BASE_DIR = Path(__file__).resolve().parent
STATUS_ORDER = ["PENDING", "ASSIGNED", "RUNNING", "COMPLETED", "FAILED", "CANCELLED"]
SUPPORTED_TASK_TYPES = ["ECHO", "TEXT_SUMMARY", "PYTHON_ECHO", "TEXT_STATS", "KEYWORD_COUNT"]
TERMINAL_STATUSES = {"COMPLETED", "FAILED", "CANCELLED"}
DEFAULT_DEMO_TASKS = [
    ("ECHO", {"message": "demo echo task"}, 40),
    (
        "TEXT_SUMMARY",
        {
            "text": (
                "This demo task shows Java-based text summarization through the executor agent. "
                "The input is normalized and shortened to a predictable result."
            ),
            "max_length": 80,
        },
        30,
    ),
    ("PYTHON_ECHO", {"message": "demo python bridge task"}, 20),
    ("TEXT_STATS", {"text": "One line.\nSecond line."}, 10),
    ("KEYWORD_COUNT", {"text": "Java agents call Python agents", "keyword": "agents"}, 5),
]


app = FastAPI(title="DiplomaWork Agent Dashboard")
app.mount("/static", StaticFiles(directory=BASE_DIR / "static"), name="static")
templates = Jinja2Templates(directory=BASE_DIR / "templates")


def format_datetime(value: datetime | date | None) -> str:
    if value is None:
        return "-"
    if isinstance(value, datetime):
        return value.strftime("%Y-%m-%d %H:%M:%S")
    return value.isoformat()


def format_json(value: Any) -> str:
    return json.dumps(value or {}, indent=2, ensure_ascii=True, sort_keys=True)


def short_text(value: Any, limit: int = 120) -> str:
    if value is None:
        return "-"
    text = str(value).replace("\n", " ")
    if len(text) <= limit:
        return text
    return text[: limit - 3] + "..."


templates.env.filters["datetime"] = format_datetime
templates.env.filters["json_pretty"] = format_json
templates.env.filters["short"] = short_text


def redirect_to(path: str) -> RedirectResponse:
    return RedirectResponse(path, status_code=303)


def parse_input_json(raw_value: str) -> dict[str, Any]:
    try:
        value = json.loads(raw_value or "{}")
    except json.JSONDecodeError as error:
        raise ValueError(f"Input JSON is invalid: {error.msg}") from error
    if not isinstance(value, dict):
        raise ValueError("Input JSON must be an object")
    return value


def status_counts(session) -> dict[str, int]:
    rows = session.execute(select(Task.status, func.count()).group_by(Task.status)).all()
    return {status: count for status, count in rows}


def workload_counts(session) -> list[dict[str, Any]]:
    rows = session.execute(
        select(Task.assigned_agent, Task.status, func.count())
        .where(Task.status.in_(["ASSIGNED", "RUNNING"]))
        .group_by(Task.assigned_agent, Task.status)
        .order_by(Task.assigned_agent.asc(), Task.status.asc())
    ).all()

    workloads: dict[str, dict[str, Any]] = {}
    for agent_name, status, count in rows:
        name = agent_name or "unassigned"
        entry = workloads.setdefault(name, {"agent_name": name, "ASSIGNED": 0, "RUNNING": 0, "total": 0})
        entry[status] = count
        entry["total"] += count
    return list(workloads.values())


def task_list_statement(status: str | None, query: str | None):
    statement = select(Task).order_by(desc(Task.created_at), desc(Task.id))
    if status:
        statement = statement.where(Task.status == status)
    if query:
        like_value = f"%{query}%"
        statement = statement.where(
            or_(
                Task.task_type.ilike(like_value),
                Task.assigned_agent.ilike(like_value),
                Task.result.ilike(like_value),
                Task.error.ilike(like_value),
            )
        )
    return statement


@app.get("/")
def dashboard(request: Request):
    with SessionLocal() as session:
        counts = status_counts(session)
        recent_tasks = session.execute(
            select(Task).order_by(desc(Task.created_at), desc(Task.id)).limit(8)
        ).scalars().all()
        recent_events = session.execute(
            select(TaskEvent).order_by(desc(TaskEvent.created_at), desc(TaskEvent.id)).limit(10)
        ).scalars().all()
        active_workloads = workload_counts(session)

    return templates.TemplateResponse(
        "dashboard.html",
        {
            "request": request,
            "active_page": "dashboard",
            "status_order": STATUS_ORDER,
            "counts": counts,
            "recent_tasks": recent_tasks,
            "recent_events": recent_events,
            "active_workloads": active_workloads,
        },
    )


@app.get("/tasks")
def tasks(request: Request, status: str | None = None, q: str | None = None):
    with SessionLocal() as session:
        task_rows = session.execute(task_list_statement(status, q).limit(100)).scalars().all()
        counts = status_counts(session)

    return templates.TemplateResponse(
        "tasks.html",
        {
            "request": request,
            "active_page": "tasks",
            "status_order": STATUS_ORDER,
            "counts": counts,
            "tasks": task_rows,
            "selected_status": status or "",
            "query": q or "",
        },
    )


@app.get("/tasks/new")
def new_task(request: Request):
    return templates.TemplateResponse(
        "new_task.html",
        {
            "request": request,
            "active_page": "new_task",
            "task_types": SUPPORTED_TASK_TYPES,
            "selected_type": "TEXT_SUMMARY",
            "input_json": format_json(
                {"text": "Example task text for the executor agent.", "max_length": 120}
            ),
            "priority": 5,
            "max_attempts": 1,
            "error": None,
        },
    )


@app.post("/tasks")
def create_task_route(
    request: Request,
    task_type: str = Form(...),
    input_json: str = Form("{}"),
    priority: int = Form(5),
    max_attempts: int = Form(1),
):
    try:
        input_data = parse_input_json(input_json)
        if task_type not in SUPPORTED_TASK_TYPES:
            raise ValueError("Unsupported task type")
        if max_attempts <= 0:
            raise ValueError("Max attempts must be positive")
    except ValueError as error:
        return templates.TemplateResponse(
            "new_task.html",
            {
                "request": request,
                "active_page": "new_task",
                "task_types": SUPPORTED_TASK_TYPES,
                "selected_type": task_type,
                "input_json": input_json,
                "priority": priority,
                "max_attempts": max_attempts,
                "error": str(error),
            },
            status_code=400,
        )

    with SessionLocal() as session:
        task = create_task(session, task_type, input_data, priority, max_attempts)
        task_id = task.id
    return redirect_to(f"/tasks/{task_id}")


@app.post("/demo-tasks")
def create_demo_tasks():
    with SessionLocal() as session:
        created_ids = [
            create_task(session, task_type, input_data, priority=priority, max_attempts=1).id
            for task_type, input_data, priority in DEFAULT_DEMO_TASKS
        ]
    return redirect_to(f"/tasks?demo={','.join(str(task_id) for task_id in created_ids)}")


@app.get("/tasks/{task_id}")
def task_detail(request: Request, task_id: int):
    with SessionLocal() as session:
        task = session.get(Task, task_id)
        if task is None:
            raise HTTPException(status_code=404, detail="Task not found")
        events = session.execute(
            select(TaskEvent)
            .where(TaskEvent.task_id == task_id)
            .order_by(TaskEvent.id.asc())
        ).scalars().all()

    return templates.TemplateResponse(
        "task_detail.html",
        {
            "request": request,
            "active_page": "tasks",
            "task": task,
            "events": events,
            "terminal_statuses": TERMINAL_STATUSES,
        },
    )
