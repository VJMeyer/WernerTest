# Multi-stage build for Upload Service

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy parent pom and module poms
COPY pom.xml .
COPY messaging/pom.xml messaging/
COPY upload-service/pom.xml upload-service/

# Download dependencies
RUN mvn dependency:go-offline -B -pl upload-service -am

# Copy source code
COPY messaging/src messaging/src
COPY upload-service/src upload-service/src

# Build the application
RUN mvn clean package -DskipTests -pl upload-service -am

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create upload directory
RUN mkdir -p /app/uploads

# Copy the JAR from build stage
COPY --from=build /build/upload-service/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Set JVM options for containerized environment (Java 21 with Virtual Threads)
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
