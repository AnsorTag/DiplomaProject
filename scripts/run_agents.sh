#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
AGENT_SYSTEM_DIR="${PROJECT_ROOT}/agent-system"
ENV_FILE="${PROJECT_ROOT}/.env"

if [[ -f "${ENV_FILE}" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "${ENV_FILE}"
    set +a
fi

EXECUTOR_1="${EXECUTOR_1:-executor1}"
EXECUTOR_2="${EXECUTOR_2:-executor2}"
COORDINATOR="${COORDINATOR:-coordinator}"
RUN_TIMEOUT_SECONDS="${RUN_TIMEOUT_SECONDS:-}"
MAVEN_OFFLINE="${MAVEN_OFFLINE:-false}"

AGENTS="${EXECUTOR_1}:com.diplomawork.agents.agent.ExecutorAgent;${EXECUTOR_2}:com.diplomawork.agents.agent.ExecutorAgent;${COORDINATOR}:com.diplomawork.agents.agent.CoordinatorAgent(${EXECUTOR_1},${EXECUTOR_2})"

cd "${AGENT_SYSTEM_DIR}"

COMMAND=(
    mvn
)

if [[ "${MAVEN_OFFLINE}" == "true" ]]; then
    COMMAND+=(-o)
fi

COMMAND+=(
    exec:java
    -Dexec.mainClass=jade.Boot
    "-Dexec.args=-agents ${AGENTS}"
)

if [[ -n "${RUN_TIMEOUT_SECONDS}" ]]; then
    exec timeout "${RUN_TIMEOUT_SECONDS}" "${COMMAND[@]}"
fi

exec "${COMMAND[@]}"
