#!/bin/sh

echo "🛠️ Running Flyway repair..."

flyway \
  -url=jdbc:h2:file:/app/data/mockwebhook \
  -user=sa \
  -password=password \
  -locations=filesystem:/app/db/migration \
  -baselineOnMigrate=true \
  repair

echo "🚀 Starting Spring Boot app..."
exec java $JAVA_OPTS -jar ./app.jar
