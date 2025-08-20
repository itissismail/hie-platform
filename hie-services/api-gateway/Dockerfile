#FROM eclipse-temurin:17-jdk
FROM amazoncorretto:17-alpine

WORKDIR /app

COPY target/hie-api-gateway.jar app.jar

# Expose config directory
VOLUME /config

ENTRYPOINT ["java","-Dlogging.config=/app/config/log4j.xml", "-jar", "app.jar", "-Dspring.config.location=/app/config/application.yml,file:/app/config/ACL.yml" ]


# Set working dir
#RUN mkdir /app
#
## Copy the jar (assumes built jar is in build/libs or target)
#COPY target/hie-api-gateway.jar /app/app.jar
#
## Expose config directory (external mounting point)
#VOLUME /config
##ADD ./config .
## Set entrypoint (application.yml + log4j2.xml are wired, ACL.yml will be read by your code)
##ENTRYPOINT ["java", \
##  "-Dspring.config.location=file:/config/application.yml", \
##  "-Dlogging.config=file:/config/log4j2.xml", \
##  "-jar", "app.jar"]
##ENTRYPOINT ["java", "-jar", "/app/app.jar"]
#ENTRYPOINT ["java","-Dlogging.config=/app/config/log4j.xml", "-jar", "/app/app.jar", "-Dspring.config.location=/app/config/application.yml,file:/app/config/ACL.yml" ]



