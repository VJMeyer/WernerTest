# Multi-stage build for Batch Processor

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy parent pom and module poms
COPY pom.xml .
COPY messaging/pom.xml messaging/
COPY batch-processor/pom.xml batch-processor/

# Download dependencies
RUN mvn dependency:go-offline -B -pl batch-processor -am

# Copy source code
COPY messaging/src messaging/src
COPY batch-processor/src batch-processor/src

# Build the application
RUN mvn clean package -DskipTests -pl batch-processor -am

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create batch directory
RUN mkdir -p /app/batch /app/uploads

# Copy the JAR from build stage
COPY --from=build /build/batch-processor/target/*.jar app.jar

# Set JVM options for containerized environment (Java 21 with Virtual Threads)
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
