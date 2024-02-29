kill_process() {
    local pid=$(pgrep -f dplanner)
    if [ -n "${pid}" ]; then
        kill -15 ${pid}
        sleep 5
        echo "> kill process ${pid}"
    else
        echo "> no process"
    fi
}

deploy_application() {
    echo "> jar file start to deploy"
    nohup java -jar ./dplanner/dplanner-0.0.1-SNAPSHOT.jar --spring.profiles.active=production >> application.log 2> /dev/null &
    sleep 20
}

health_check() {
    echo "> health check"
    for retry_count in {1..10}; do
        local response=$(curl -s http://localhost:8080/auth/health)
        local up_count=$(echo $response | grep 'UP' | wc -l)

        if [ $up_count -ge 1 ]; then
            echo "> Health check 성공"
            exit 0
        else
            echo "> Health check의 응답을 알 수 없거나 혹은 status가 UP이 아닙니다."
            echo "> Health check: ${response}"
        fi

        if [ $retry_count -eq 10 ]; then
            echo "> Health check 실패. "
        fi

        sleep 10
    done
}

# 첫 번째 배포 시도
kill_process
chmod +x ./dplanner/dplanner-0.0.1-SNAPSHOT.jar
deploy_application
health_check

# 배포가 실패했을 경우 다시 시도
kill_process
chmod +x ./dplanner/dplanner-0.0.1-SNAPSHOT.jar
deploy_application
health_check
exit 1