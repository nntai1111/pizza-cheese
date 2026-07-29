# ==============================================================================
# STAGE 1: Build JAR application bằng Maven & JDK 17
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml trước để tận dụng Docker Caching layer cho dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy toàn bộ mã nguồn Java và tiến hành build package (bỏ qua tests khi build container)
COPY src ./src
RUN mvn clean package -DskipTests

# ==============================================================================
# STAGE 2: Môi trường Runtime siêu nhẹ với JRE 17 Alpine
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy file jar đã build thành công từ STAGE 1
COPY --from=builder /app/target/*.jar app.jar

# Mở port 8080 cho Spring Boot
EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
