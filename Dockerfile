# Verwende Maven mit Java 22 für den Build
FROM maven:3.9.4-eclipse-temurin AS builder

WORKDIR /app

# Kopiere die Maven-Projektdateien und lade Abhängigkeiten herunter
COPY pom.xml ./
RUN mvn dependency:go-offline

# Kopiere den Quellcode und baue das Projekt
COPY src ./src
RUN mvn package -DskipTests

# Verwende ein leichtgewichtiges JRE-Image für die Laufzeit
FROM eclipse-temurin:22-jre

WORKDIR /app

# JMX Exporter herunterladen
ARG JMX_EXPORTER_VERSION=0.17.2
ADD https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${JMX_EXPORTER_VERSION}/jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar \
    /jmx_exporter.jar

# Konfigurationsdatei für JMX Exporter
COPY jmx_exporter_config.yml /app/jmx_exporter_config.yml

# Anwendung kopieren
COPY --from=builder /app/target/java-mem-thief-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

# Exponiere Port für Prometheus
EXPOSE 2112

# Starte die JVM mit JMX Exporter
ENTRYPOINT ["java", "-javaagent:/jmx_exporter.jar=2112:/app/jmx_exporter_config.yml", "-jar", "app.jar"]
