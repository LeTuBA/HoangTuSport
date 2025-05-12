# Stage 1: build
FROM gradle:8.13.0-jdk17 AS build
COPY --chown=gradle:gradle . /app
WORKDIR /app

# skip test
RUN gradle clean build -x test --no-daemon

# Stage 2: run application
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install curl for healthcheck
# RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# copy file jar
COPY --from=build /app/build/libs/*.jar app.jar

# open application port
EXPOSE 8080

# run application
ENTRYPOINT ["java", "-jar", "app.jar"] 