#!/bin/bash

APP_NAME="dplanner"
DEFAULT_CONF_PATH="/etc/nginx/sites-enabled"
DEFAULT_CONF="default"
MAX_RETRIES=10

# 컨테이너 스위칭
switch_container() {
  IS_BLUE=$(docker-compose -p "${APP_NAME}-blue" -f docker-compose.blue.yml ps | grep Up)
  if [ -z "$IS_BLUE" ]; then
      echo "### GREEN => BLUE ###"
      docker-compose -p "${APP_NAME}-blue" -f docker-compose.blue.yml up -d
      BEFORE_COMPOSE_COLOR="green"
      AFTER_COMPOSE_COLOR="blue"

      sleep 30

      health_check "http://127.0.0.1:9001/actuator/health"
  else
      echo "### BLUE => GREEN ###"
      docker-compose -p "${APP_NAME}-green" -f docker-compose.green.yml up -d
      BEFORE_COMPOSE_COLOR="blue"
      AFTER_COMPOSE_COLOR="green"

      sleep 30

      health_check "http://127.0.0.1:9000/actuator/health"
  fi
}

# 컨테이너 상태 체크
health_check() {
    local RETRIES=0
    local URL=$1
    while [ $RETRIES -lt $MAX_RETRIES ]; do
      echo "Checking service at $URL... (attempt: $((RETRIES+1)))"
      sleep 3

      RESPONSE=$(curl -s "$URL")
      if [ -n "$RESPONSE" ]; then
        STATUS=$(echo "$RESPONSE" | jq -r '.status')
        if [ "$STATUS" = "UP" ]; then
          echo "health check success"
          return 0
        fi
      fi

      RETRIES=$((RETRIES+1))
    done;

    echo "Failed to check service after $MAX_RETRIES attempts."
    docker-compose -p "${APP_NAME}-${AFTER_COMPOSE_COLOR}" -f "docker-compose.${AFTER_COMPOSE_COLOR}.yml" down
    echo "### DEPLOY FAILED ###"
    exit 1

}

switch_conf() {
    cp "${DEFAULT_CONF_PATH}/${AFTER_COMPOSE_COLOR}" "${DEFAULT_CONF_PATH}/${DEFAULT_CONF}"
    nginx -s reload
}

down_container() {
  # 이전 컨테이너 종료
    docker-compose -p "${APP_NAME}-${BEFORE_COMPOSE_COLOR}" -f "docker-compose.${BEFORE_COMPOSE_COLOR}.yml" down
    echo "### $BEFORE_COMPOSE_COLOR DOWN ###"
}

switch_container
switch_conf
down_container