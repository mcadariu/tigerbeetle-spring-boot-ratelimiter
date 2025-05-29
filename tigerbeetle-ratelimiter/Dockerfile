FROM openjdk:24-slim

WORKDIR /app

COPY target/*.jar app.jar

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar .

RUN chmod 644 opentelemetry-javaagent.jar

EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", \
            "-javaagent:/app/opentelemetry-javaagent.jar", \
            "-Dotel.service.name=tigerbeetle-rate-limiting", \
            "-Dotel.traces.exporter=otlp", \
            "-Dotel.metrics.exporter=none", \
            "-Dotel.logs.exporter=none", \
            "-Dotel.exporter.otlp.endpoint=http://10.5.0.53:4318", \
            "-jar", \
            "/app/app.jar"]