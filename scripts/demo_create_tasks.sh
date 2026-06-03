#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PYTHON="${PYTHON:-${PROJECT_ROOT}/venv/bin/python}"
CREATE_TASK="${PROJECT_ROOT}/scripts/create_task.py"

"${PYTHON}" "${CREATE_TASK}" ECHO \
    --priority 40 \
    --input-json '{"message":"demo echo task"}'

"${PYTHON}" "${CREATE_TASK}" TEXT_SUMMARY \
    --priority 30 \
    --input-json '{"text":"This demo task shows Java-based text summarization through the executor agent. The input is normalized and shortened to a predictable result.","max_length":80}'

"${PYTHON}" "${CREATE_TASK}" PYTHON_ECHO \
    --priority 20 \
    --input-json '{"message":"demo python bridge task"}'

"${PYTHON}" "${CREATE_TASK}" TEXT_STATS \
    --priority 10 \
    --input-json '{"text":"One line.\nSecond line."}'

echo "Created demo tasks. Start agents with: scripts/run_agents.sh"
