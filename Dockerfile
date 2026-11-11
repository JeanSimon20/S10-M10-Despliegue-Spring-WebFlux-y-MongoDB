# Etapa 1: Compilación con Maven y JDK 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: Imagen ligera de ejecución con JRE 17
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S springgroup && adduser -S springuser -G springgroup
USER springuser:springgroup

COPY --from=builder /workspace/target/app-webflux-mongo.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
