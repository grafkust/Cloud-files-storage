FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/*.jar grafCloud.jar
ENTRYPOINT ["java","-jar","/app/grafCloud.jar"]
