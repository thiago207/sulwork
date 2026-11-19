# syntax=docker/dockerfile:1

# Estagio 1: compila e empacota o jar com o Maven Wrapper
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B package -DskipTests

# Estagio 2: executa so com o JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV TZ=America/Recife

RUN addgroup -S sulwork && adduser -S sulwork -G sulwork
COPY --from=build /app/target/*.jar app.jar
USER sulwork

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
