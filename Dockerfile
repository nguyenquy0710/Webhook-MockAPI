FROM openjdk:17-jdk-slim
COPY staging/app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
