# ---- build stage -----------------------------------------------------------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached unless pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B package -DskipTests

# ---- runtime stage ---------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
USER app
COPY --from=build /workspace/target/fraud-service.war app.war

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
