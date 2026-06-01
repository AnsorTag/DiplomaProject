import time
from datetime import datetime, timezone

from db.database import SessionLocal
from db.models import Task

def utc_now():
    return datetime.now(timezone.utc)

def example_task(task_id: int):
    db = SessionLocal()

    task = None

    try:
        task = db.query(Task).filter(Task.id == task_id).first()

        if task is None:
            raise ValueError("Task not found")
        
        task.status = "RUNNING"
        task.started_at = utc_now()
        db.commit()

        print("Task started")
        time.sleep(5)
        print("Task finished")

        task.status = "COMPLETED"
        task.result = "Task completed"
        task.finished_at = utc_now()
        db.commit()

        return "Task completed"
    
    except Exception as e:
        task.status = "FAILED"
        task.error = str(e)
        task.finished_at = utc_now()
        db.commit()

        raise

    finally:
        db.close()