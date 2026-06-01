from collections.abc import Sequence
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from db.models import Task


DEFAULT_TASK_STATUS = "PENDING"
ASSIGNED_TASK_STATUS = "ASSIGNED"


def create_task(
    db: Session,
    task_type: str,
    input_data: dict[str, Any] | None = None,
    priority: int = 5,
) -> Task:
    task = Task(
        task_type=task_type,
        input_data=input_data or {},
        priority=priority,
        status=DEFAULT_TASK_STATUS,
    )
    db.add(task)
    db.commit()
    db.refresh(task)
    return task


def get_task(db: Session, task_id: int) -> Task | None:
    return db.get(Task, task_id)


def list_tasks(
    db: Session,
    status: str | None = None,
    limit: int = 20,
) -> Sequence[Task]:
    statement = select(Task).order_by(Task.priority.desc(), Task.created_at.asc()).limit(limit)
    if status is not None:
        statement = statement.where(Task.status == status)
    return db.execute(statement).scalars().all()


def assign_task(db: Session, task_id: int, agent_name: str) -> Task | None:
    task = get_task(db, task_id)
    if task is None:
        return None

    task.assigned_agent = agent_name
    task.status = ASSIGNED_TASK_STATUS
    db.commit()
    db.refresh(task)
    return task
