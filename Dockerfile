# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Korean fonts for PDF rendering.
RUN apt-get update \
    && apt-get install -y --no-install-recommends fonts-nanum \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar /app/app.jar

ENV TZ=Asia/Seoul
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
