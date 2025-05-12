# Stage 1: build
FROM gradle:8.13.0-jdk17 AS build
COPY --chown=gradle:gradle . /app
WORKDIR /app

# skip test
RUN gradle clean build -x test --no-daemon

# Stage 2: run application
FROM eclipse-temurin:17-jre
WORKDIR /app

# copy file jar
COPY --from=build /app/build/libs/*.jar app.jar

# open application port - change to port 8888
EXPOSE 8080

# run application with docker profile
ENTRYPOINT ["java", "-jar", "app.jar"] 