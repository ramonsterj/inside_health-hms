# Stage 1: Build
FROM --platform=linux/amd64 eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache Gradle wrapper
COPY gradlew ./
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew --version

# Cache dependencies
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon || true

# Build application
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
FROM --platform=linux/amd64 eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd -r hms && useradd -r -g hms hms
RUN mkdir -p /var/data/hms/files && chown -R hms:hms /var/data/hms

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown hms:hms app.jar

USER hms

EXPOSE 8081

ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}
ENTRYPOINT ["java", "-jar", "app.jar"]
