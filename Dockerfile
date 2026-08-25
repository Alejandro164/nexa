# Etapa de compilación
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -Dmaven.test.skip=true

# Etapa de ejecución
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# LibreOffice: requerido en runtime por LibreOfficeConversionService (conversión a PDF).
# curl: usado por el HEALTHCHECK.
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libreoffice-writer \
    libreoffice-calc \
    libreoffice-impress \
    curl \
    && apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd -r nexa && useradd -r -g nexa -d /app nexa
RUN mkdir -p /home/datos-nexa && chown -R nexa:nexa /home/datos-nexa /app

COPY --from=build --chown=nexa:nexa /app/target/*.jar app.jar

USER nexa
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/login || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]


