# syntax=docker/dockerfile:1.6
# LookseeCore version is supplied at build time via --build-arg from the
# repo-root LOOKSEE_CORE_VERSION file. See scripts/download-core.sh.
FROM maven:3.9.6-eclipse-temurin-17 AS build

ARG LOOKSEE_CORE_VERSION
RUN test -n "${LOOKSEE_CORE_VERSION}" || (echo "LOOKSEE_CORE_VERSION build-arg is required" >&2; exit 1)

WORKDIR /app

COPY pom.xml .

RUN curl -fsSL "https://github.com/deepthought42/LookseeCore/releases/download/v${LOOKSEE_CORE_VERSION}/looksee-core-${LOOKSEE_CORE_VERSION}.jar" \
        -o "/tmp/core-${LOOKSEE_CORE_VERSION}.jar" \
 && mvn -q install:install-file \
        -Dfile="/tmp/core-${LOOKSEE_CORE_VERSION}.jar" \
        -DgroupId=com.looksee \
        -DartifactId=core \
        -Dversion="${LOOKSEE_CORE_VERSION}" \
        -Dpackaging=jar

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-ea","-jar","/app/app.jar"]
