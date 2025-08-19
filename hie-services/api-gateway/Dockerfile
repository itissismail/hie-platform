FROM eclipse-temurin:17-jdk-alpine

# Set working dir
WORKDIR /app

# Copy the jar (assumes built jar is in build/libs or target)
COPY target/hie-api-gateway.jar app.jar

# Expose config directory (external mounting point)
VOLUME /config

# Set entrypoint (application.yml + log4j2.xml are wired, ACL.yml will be read by your code)
ENTRYPOINT ["java", \
  "-Dspring.config.location=file:/config/application.yml", \
  "-Dlogging.config=file:/config/log4j2.xml", \
  "-jar", "app.jar"]
