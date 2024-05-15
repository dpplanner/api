FROM eclipse-temurin:17-jdk-alpine

ENV TZ=Asia/Seoul

RUN apk --no-cache add tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone \
    apk del tzdata

ARG JAR_FILE=../build/libs/dplanner-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} dplanner.jar

RUN mkdir ./log

ENTRYPOINT ["java","-jar", "-Dspring.profiles.active=production", "/dplanner.jar"]
