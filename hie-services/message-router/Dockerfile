FROM amazoncorretto:17-alpine

WORKDIR /app

# Copy the built jar
COPY target/hie-message-router.jar app.jar

# Expose config directory (external mount)
VOLUME /config

# Run with external config and logging
ENTRYPOINT ["java", "-Dlogging.config=/app/config/log4j.xml", "-Dspring.config.location=/app/config/application.yml", "-jar", "app.jar"]
