# Use an official Maven image to build the project
FROM maven:3.9.6-eclipse-temurin-17 as build

# Set the working directory inside the container
# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies first for faster builds
COPY pom.xml .

# Copy and run the download script to get the LookseeCore JAR
COPY scripts/download-core.sh ./scripts/download-core.sh
RUN chmod +x ./scripts/download-core.sh
RUN bash ./scripts/download-core.sh
RUN mvn install:install-file -Dfile=libs/core-0.3.19.jar -DgroupId=com.looksee -DartifactId=core -Dversion=0.3.19 -Dpackaging=jar

# Copy the rest of the project source code
COPY src ./src

# Build the application
RUN mvn clean install -DskipTests

# Use a smaller JDK image to run the app
FROM eclipse-temurin:17-jre

# Copy the built JAR file from the previous stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 443
EXPOSE 80
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Xms512M", "-ea","-jar", "app.jar"]
