#!/bin/bash
set -e

# Hosts are parameterized so the same image works in docker-compose
# (mysql/redis) and on Railway (*.railway.internal). Defaults keep
# docker-compose behavior unchanged.
MYSQL_HOST="${MYSQL_HOST:-mysql}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
REDIS_HOST="${REDIS_HOST:-redis}"
REDIS_PORT="${REDIS_PORT:-6379}"

wait_for() {
    local host="$1" port="$2" i
    for i in $(seq 1 90); do
        if (exec 3<>"/dev/tcp/${host}/${port}") 2>/dev/null; then
            exec 3>&- 3<&-
            return 0
        fi
        sleep 2
    done
    echo "ERROR: timeout waiting for ${host}:${port}" >&2
    return 1
}

if [ "${SKIP_WAIT_MYSQL:-0}" != "1" ]; then
    echo "[entrypoint] waiting for mysql at ${MYSQL_HOST}:${MYSQL_PORT}..."
    wait_for "${MYSQL_HOST}" "${MYSQL_PORT}"
fi
if [ "${SKIP_WAIT_REDIS:-0}" != "1" ]; then
    echo "[entrypoint] waiting for redis at ${REDIS_HOST}:${REDIS_PORT}..."
    wait_for "${REDIS_HOST}" "${REDIS_PORT}"
fi
echo "[entrypoint] starting ruoyi-admin on port ${SERVER_PORT:-18080}"
exec java ${JVM_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar
