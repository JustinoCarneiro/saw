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
# /app/data é onde AudioStorageService grava os áudios de mentoria (sawhub.audio.storage-dir) —
# precisa existir e já pertencer ao sawhub antes do USER trocar, senão o boot falha com
# AccessDeniedException (WORKDIR é criado como root). Pré-criar aqui também garante que, se
# /app/data virar um volume Docker nomeado em produção, o volume herda o dono certo na primeira
# montagem (Docker copia o conteúdo/ownership da imagem pro volume vazio nesse momento).
RUN mkdir -p /app/data/audios && chown -R sawhub:sawhub /app
USER sawhub

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
