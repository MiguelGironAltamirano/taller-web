FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
# The evidences directory must exist and belong to the runtime user so a fresh named volume
# mounted there inherits that ownership; otherwise Docker creates it root-owned and uploads fail.
RUN useradd --system --uid 10001 app \
    && mkdir -p /app/evidences \
    && chown app /app/evidences
COPY --from=build /app/target/servicios-*.jar app.jar
COPY start.sh /app/start.sh
EXPOSE 8080
# Readiness probe via Spring Boot Actuator. The JRE image ships neither curl nor wget, so the
# probe speaks HTTP through bash's built-in /dev/tcp instead of adding packages to the image.
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=6 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health/readiness HTTP/1.0\\r\\nHost: localhost\\r\\n\\r\\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]
# Railway volumes are mounted root-owned. Fix the mount directory, then run Java as UID 10001.
ENTRYPOINT ["bash", "/app/start.sh"]
