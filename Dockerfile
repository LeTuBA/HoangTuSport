FROM gradle:8.7-jdk17 AS build
COPY --chown=gradle:gradle . /app
WORKDIR /app

# Bỏ qua task test khi build
RUN gradle clean build -x test --no-daemon

# Stage 2: Chạy ứng dụng
FROM eclipse-temurin:17-jre
WORKDIR /app

# Tạo thư mục upload
RUN mkdir -p /app/upload

# Copy file JAR đã build từ stage build
COPY --from=build /app/build/libs/*.jar app.jar

# Khai báo volume cho thư mục upload
VOLUME /app/upload

# Thiết lập biến môi trường
ENV SPRING_PROFILES_ACTIVE=docker

# Mở cổng ứng dụng - Thay đổi sang cổng 8888
EXPOSE 8888

# Chạy ứng dụng với profile docker
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"] 