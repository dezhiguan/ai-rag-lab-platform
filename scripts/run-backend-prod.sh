#!/usr/bin/env bash
# 加载 .env.prod 并以 prod profile 启动后端 jar
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env.prod"
JAR="${ROOT_DIR}/backend/target/backend-0.0.1-SNAPSHOT.jar"

if [[ ! -f "$JAR" ]]; then
  echo "Error: jar not found at $JAR"
  echo "Run: cd backend && mvn -DskipTests package"
  exit 1
fi

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "$ENV_FILE"
  set +a
  echo "Loaded environment from $ENV_FILE"
else
  echo "Warning: $ENV_FILE not found, using existing shell environment variables"
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"

exec java ${JAVA_OPTS:-} -jar "$JAR"
