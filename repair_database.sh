#!/bin/bash

# Script to repair Flyway database migrations
# Use this script if you encounter Flyway validation errors

echo "🔧 Database Migration Repair Tool"
echo "=================================="
echo ""

# Check if data directory exists
if [ ! -d "data" ]; then
    echo "ℹ️  No database found. This script is only needed if you have migration issues."
    echo "   The application will create a fresh database on first start."
    exit 0
fi

echo "⚠️  This will attempt to repair failed database migrations."
echo "   Make sure the application is not running before proceeding."
echo ""
read -p "Do you want to continue? (y/N): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Operation cancelled."
    exit 1
fi

echo ""
echo "🏥 Attempting to repair database migrations..."

# Try to run Flyway repair through Maven
if command -v ./mvnw &> /dev/null; then
    echo "📦 Using Maven to repair migrations..."
    ./mvnw flyway:repair -Dflyway.configFiles=src/main/resources/application.properties
elif command -v mvn &> /dev/null; then
    echo "📦 Using Maven to repair migrations..."
    mvn flyway:repair -Dflyway.configFiles=src/main/resources/application.properties
else
    echo "❌ Maven not found. Please install Maven or use the Maven wrapper (./mvnw)."
    exit 1
fi

echo ""
echo "✅ Repair completed. Try starting the application again."
echo "   If issues persist, you may need to delete the data/ directory"
echo "   to start with a fresh database."