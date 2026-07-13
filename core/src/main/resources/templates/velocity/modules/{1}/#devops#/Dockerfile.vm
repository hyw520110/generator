FROM openjdk:${r"${java_version:8}"}-jre-alpine
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY ${r"${JAR_FILE}"} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
