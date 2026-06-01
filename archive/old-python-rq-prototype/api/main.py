from fastapi import FastAPI, HTTPException

from worker.queue import task_queue, redis_conn
from worker.tasks import example_task

from rq.job import Job
from rq.exceptions import NoSuchJobError

from db.database import SessionLocal
from db.models import Task


app = FastAPI()


@app.get("/")
def root():
    return{"status":"ok"}


@app.post("/tasks")
def create_task():
    db = SessionLocal()

    try:
        task = Task(status="PENDING")

        db.add(task)
        db.commit()
        db.refresh(task)

        job = task_queue.enqueue(example_task, task.id)

        task.job_id = job.id
        db.commit()
        db.refresh(task)

        return {
            "message": "Task submitted",
            "task_id": task.id,
            "job_id": job.id,
            "status": task.status

        }
    
    finally:
        db.close()
    


@app.get("/tasks/{job_id}")
def receive_id(job_id: str):
    try:
        job = Job.fetch(job_id, connection=redis_conn)
    except NoSuchJobError:
        raise HTTPException(status_code=404, detail="Job not found")

    return {
        "job_id": job.id,
        "status": job.get_status(),
        "result": job.result
    }
        