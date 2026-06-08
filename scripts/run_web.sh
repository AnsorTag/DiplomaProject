#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
PYTHON="${PYTHON:-${PROJECT_ROOT}/venv/bin/python}"
HOST="${WEB_HOST:-127.0.0.1}"
PORT="${WEB_PORT:-8000}"

if [[ -f "${ENV_FILE}" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "${ENV_FILE}"
    set +a
fi

cd "${PROJECT_ROOT}"
exec "${PYTHON}" -m uvicorn web.app:app --host "${HOST}" --port "${PORT}" --reload
