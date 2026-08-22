FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21.0.8_9-jre-alpine

RUN addgroup -S application && adduser -S application -G application
WORKDIR /app
COPY --from=build --chown=application:application /workspace/target/*.jar application.jar

USER application
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
