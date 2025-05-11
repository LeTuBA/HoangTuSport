FROM gradle:8.7-jdk17-alpine AS build
COPY --chown=gradle:gradle . /app
WORKDIR /app

# Bỏ qua task test khi build
RUN gradle clean build -x test --no-daemon

# Stage 2: Chạy ứng dụng
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy file JAR đã build từ stage build
COPY --from=build /app/build/libs/*.jar app.jar

# Mở cổng ứng dụng - Thay đổi sang cổng 8888
EXPOSE 8888

# Chạy ứng dụng với profile docker
ENTRYPOINT ["java", "-jar", "app.jar"] 