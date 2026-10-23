FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/monitoring-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms64m", "-Xmx256m", "-jar", "/app/app.jar"]

