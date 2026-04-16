# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:25-ea-17-jdk

LABEL maintainer="Nguyen Quy <quyit.job@gmail.com>" \
  org.opencontainers.image.authors="Nguyen Quy <quyit.job@gmail.com>" \
  org.opencontainers.image.description="A powerful and user-friendly webhook testing tool that allows developers to create, manage, and test webhook APIs with ease."

USER root

ENV TZ=Asia/Ho_Chi_Minh \
  LANG=en_US.UTF-8 \
  LANGUAGE=en_US.UTF-8

ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true" \
  JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/WebHookMock-0.0.1-SNAPSHOT.jar ./app.jar
# Copy the entrypoint script and make it executable
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
COPY repair_database.sh ./repair_database.sh
RUN chmod +x ./repair_database.sh
COPY validate_migration.sh ./validate_migration.sh
RUN chmod +x ./validate_migration.sh

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar ./app.jar"]
