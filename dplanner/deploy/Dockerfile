FROM eclipse-temurin:17-jdk-alpine

ARG JAR_FILE=../build/libs/dplanner-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} dplanner.jar

ENTRYPOINT ["java","-jar", "-Dspring.profiles.active=production", "/dplanner.jar"]