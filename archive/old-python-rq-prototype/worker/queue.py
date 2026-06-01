from redis import Redis
from rq import Queue

redis_conn = Redis(host="localhost", port=6379)

task_queue = Queue("tasks", connection=redis_conn)