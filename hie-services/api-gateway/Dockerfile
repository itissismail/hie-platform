FROM eclipse-temurin:17-jdk-alpine

# Set working dir
WORKDIR /app

# Copy the jar (assumes built jar is in build/libs or target)
COPY target/hie-api-gateway.jar app.jar

# Expose config directory
VOLUME /config

# Set entrypoint
ENTRYPOINT ["java", "-Dspring.config.location=file:/config/", "-Dlogging.config=file:/config/log4j2.xml", "-jar", "app.jar"]
