# syntax=docker/dockerfile:1

# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy files needed to resolve/build dependencies first for better layer caching
COPY pom.xml ./
COPY scripts ./scripts
RUN chmod +x ./scripts/download-core.sh

# Install Looksee core jar used by this service
RUN bash ./scripts/download-core.sh && \
    mvn -B install:install-file \
      -Dfile=libs/core-0.3.21.jar \
      -DgroupId=com.looksee \
      -DartifactId=core \
      -Dversion=0.3.21 \
      -Dpackaging=jar

# Copy source and package app
COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/journeyExpander-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xms256M", "-ea", "-jar", "/app/app.jar"]
