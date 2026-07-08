# syntax=docker/dockerfile:1

# ---- build ---------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Camada de dependências separada da de código: só reprocessa `mvn dependency:go-offline`
# quando o pom.xml muda, não a cada alteração de código-fonte.
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -B package -DskipTests

# ---- runtime ---------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S sawhub && adduser -S sawhub -G sawhub
COPY --from=build /app/target/*.jar app.jar
RUN chown sawhub:sawhub app.jar
USER sawhub

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
