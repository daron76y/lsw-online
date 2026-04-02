#!/usr/bin/env bash
set -euo pipefail

# Poll health endpoints so system tests fail fast with a clear reason.
wait_for_health() {
  local url="$1"
  local timeout_seconds="${2:-240}"
  local interval_seconds=5
  local deadline=$((SECONDS + timeout_seconds))

  echo "Checking ${url}"
  while (( SECONDS < deadline )); do
    if curl -fsS "${url}" >/dev/null; then
      echo "Healthy: ${url}"
      return 0
    fi
    sleep "${interval_seconds}"
  done

  echo "Timed out waiting for ${url}" >&2
  return 1
}

wait_for_health "http://localhost:8081/actuator/health"
wait_for_health "http://localhost:8082/actuator/health"
wait_for_health "http://localhost:8083/actuator/health"
wait_for_health "http://localhost:8084/actuator/health"
wait_for_health "http://localhost:8085/actuator/health"
wait_for_health "http://localhost:8080/actuator/health"

echo "All service health checks passed."
