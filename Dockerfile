# ==============================================================================
# ETAPA 1: Construcción (Build Stage) con Maven y JDK 17
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace

# Copiar archivo de definición de dependencias primero para aprovechar la caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y empaquetar el JAR ejecutable
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# ETAPA 2: Ejecución (Runtime Stage) ligera y segura con JRE 17 Alpine
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Buenas prácticas DevSecOps: Ejecutar el contenedor con un usuario sin privilegios (non-root)
RUN addgroup -S springgroup && adduser -S springuser -G springgroup
USER springuser:springgroup

# Copiar únicamente el artefacto compilado desde la etapa builder
COPY --from=builder /workspace/target/app-webflux-mongo.jar /app/app.jar

# Puerto por defecto expuesto por Netty (configurable vía ConfigMap APP_PORT)
EXPOSE 8080

# Optimización de memoria JVM para contenedores en Kubernetes
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

