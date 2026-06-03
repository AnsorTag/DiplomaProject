FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven python3 python3-venv python3-pip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt ./
RUN python3 -m venv /app/venv \
    && /app/venv/bin/pip install --no-cache-dir --upgrade pip \
    && /app/venv/bin/pip install --no-cache-dir -r requirements.txt

COPY agent-system/pom.xml agent-system/pom.xml
RUN cd agent-system && mvn dependency:go-offline exec:help -Ddetail

COPY . .
RUN cd agent-system && mvn compile

ENV PYTHON_EXECUTABLE=/app/venv/bin/python
ENV PYTHON_TASK_RUNNER=/app/python_tasks/run_task.py
ENV MAVEN_OFFLINE=true

CMD ["scripts/run_agents.sh"]
