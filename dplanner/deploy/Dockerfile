FROM eclipse-temurin:17-jre-jammy

WORKDIR /dplanner

ENV TZ=Asia/Seoul

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ARG JAR_FILE=./build/libs/dplanner-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} dplanner.jar

RUN mkdir ./log

ENTRYPOINT ["java","-jar", "-Dspring.profiles.active=production", "./dplanner.jar"]
