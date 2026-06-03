#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
AGENT_SYSTEM_DIR="${PROJECT_ROOT}/agent-system"

EXECUTOR_1="${EXECUTOR_1:-executor1}"
EXECUTOR_2="${EXECUTOR_2:-executor2}"
COORDINATOR="${COORDINATOR:-coordinator}"
RUN_TIMEOUT_SECONDS="${RUN_TIMEOUT_SECONDS:-}"

AGENTS="${EXECUTOR_1}:com.diplomawork.agents.executor.ExecutorAgent;${EXECUTOR_2}:com.diplomawork.agents.executor.ExecutorAgent;${COORDINATOR}:com.diplomawork.agents.coordinator.CoordinatorAgent(${EXECUTOR_1},${EXECUTOR_2})"

cd "${AGENT_SYSTEM_DIR}"

COMMAND=(
    mvn
    exec:java
    -Dexec.mainClass=jade.Boot
    "-Dexec.args=-agents ${AGENTS}"
)

if [[ -n "${RUN_TIMEOUT_SECONDS}" ]]; then
    exec timeout "${RUN_TIMEOUT_SECONDS}" "${COMMAND[@]}"
fi

exec "${COMMAND[@]}"
