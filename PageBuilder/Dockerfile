# Use an official Maven image to build the project
FROM maven:3.9.6-eclipse-temurin-17 AS build

ARG LOOKSEE_CORE_VERSION=0.3.16

# Set the working directory inside the container
WORKDIR /app

# Copy build metadata and helper scripts first for better layer caching
COPY pom.xml .
COPY scripts/download-core.sh ./scripts/download-core.sh
RUN chmod +x ./scripts/download-core.sh

# Download and install Looksee Core dependency used by this service
RUN bash ./scripts/download-core.sh "${LOOKSEE_CORE_VERSION}"
RUN mvn install:install-file     -Dfile="libs/core-${LOOKSEE_CORE_VERSION}.jar"     -DgroupId=com.looksee     -DartifactId=core     -Dversion="${LOOKSEE_CORE_VERSION}"     -Dpackaging=jar

# Copy source and build the application
COPY src ./src
RUN mvn clean install -DskipTests

# Use a smaller JRE image to run the app
FROM eclipse-temurin:17-jre

# Copy the built JAR file from the previous stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
EXPOSE 80
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xms512M", "-jar", "app.jar"]
