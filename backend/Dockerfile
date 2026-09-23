FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
# Tests already ran as a separate CI step before this image is built (see ../.github/workflows/ci.yml).
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/finance-backend-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# JVM heap per the 4GB droplet sizing plan (docs/08-devops/deployment.md).
ENTRYPOINT ["sh", "-c", "java -Xmx896m -jar app.jar"]
