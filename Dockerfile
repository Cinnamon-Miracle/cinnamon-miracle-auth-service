# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl auth-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /build/auth-service/target/auth-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Dnet.bytebuddy.experimental=true", "-jar", "app.jar"]
