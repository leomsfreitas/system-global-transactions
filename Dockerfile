# syntax=docker/dockerfile:1.7
# Imagem unica para api e worker: docker build --build-arg MODULO=api|worker .

FROM eclipse-temurin:21.0.12_8-jdk AS build
ARG MODULO
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY modulos/dominio/pom.xml modulos/dominio/pom.xml
COPY modulos/conectores/pom.xml modulos/conectores/pom.xml
COPY modulos/api/pom.xml modulos/api/pom.xml
COPY modulos/worker/pom.xml modulos/worker/pom.xml
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline -pl modulos/${MODULO} -am || true
COPY db db
COPY api-spec api-spec
COPY modulos modulos
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -B package -pl modulos/${MODULO} -am -DskipTests -Dspotless.check.skip -Djacoco.skip \
    && cp modulos/${MODULO}/target/${MODULO}.jar /app.jar

FROM eclipse-temurin:21.0.12_8-jre-alpine
ARG MODULO
LABEL org.opencontainers.image.title="transacoes-${MODULO}"
RUN addgroup -S app && adduser -S app -G app \
    && apk add --no-cache curl \
    && curl -fsSL https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem -o /etc/ssl/certs/rds-global-bundle.pem \
    && apk del curl
WORKDIR /app
COPY --from=build /app.jar app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
