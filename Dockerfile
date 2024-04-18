FROM openjdk:17-jdk-slim-buster
VOLUME /tmp
EXPOSE 8081
ARG JAR_FILE=build/libs/spring-keycloak-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
