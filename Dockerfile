# ---- Etapa 1: build (Alpine + JDK 21) ----
FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-build-cache --no-daemon

# ---- Etapa 2: ejecución (Alpine + solo JRE) ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown app:app app.jar

USER app

EXPOSE 10000

# MaxRAMPercentage: usa como máximo el 70% de la RAM del contenedor
# UseSerialGC: GC con menor huella de memoria que G1
ENTRYPOINT ["java", \
            "-XX:MaxRAMPercentage=70.0", \
            "-XX:+UseSerialGC", \
            "-XX:+UseContainerSupport", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]