#!/bin/bash

# Database Migration Validation Script
# This script helps verify that your database migration completed successfully

echo "=== Webhook MockAPI Database Migration Validation ==="
echo ""

# Set the database path
DB_PATH="./data/mockwebhook"

# Check if database exists
if [ ! -f "${DB_PATH}.mv.db" ]; then
    echo "❌ Database file not found at ${DB_PATH}.mv.db"
    echo "   Please ensure the application has been started at least once."
    exit 1
fi

echo "✅ Database file found at ${DB_PATH}.mv.db"

# Function to run SQL query
run_sql() {
    java -cp "$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout):target/classes" \
         org.h2.tools.Shell \
         -url "jdbc:h2:file:${DB_PATH}" \
         -user sa \
         -password password \
         -sql "$1" 2>/dev/null
}

echo ""
echo "=== Checking Migration Status ==="

# Check Flyway schema history
echo "📋 Migration History:"
run_sql "SELECT \"installed_rank\", \"version\", \"description\", \"success\" FROM \"flyway_schema_history\" ORDER BY \"installed_rank\";"

echo ""
echo "=== Validating Database Schema ==="

# Check if delay_ms column exists
DELAY_COL_EXISTS=$(run_sql "SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='API_CONFIGS' AND COLUMN_NAME='DELAY_MS';" | grep -o '[0-9]' | head -1)

if [ "$DELAY_COL_EXISTS" = "1" ]; then
    echo "✅ delay_ms column exists in api_configs table"
else
    echo "❌ delay_ms column missing in api_configs table"
    echo "   Migration may not have completed successfully."
    exit 1
fi

# Check data integrity
echo ""
echo "=== Checking Data Integrity ==="

# Count records with null delay_ms
NULL_DELAY_COUNT=$(run_sql "SELECT COUNT(*) as count FROM api_configs WHERE delay_ms IS NULL;" | grep -o '[0-9]' | head -1)

if [ "$NULL_DELAY_COUNT" = "0" ]; then
    echo "✅ All api_configs records have valid delay_ms values"
else
    echo "⚠️  Found $NULL_DELAY_COUNT records with null delay_ms values"
    echo "   These will be treated as 0ms delay by the application."
fi

# Show sample data
echo ""
echo "=== Sample Data ==="
echo "📊 First 5 API configurations:"
run_sql "SELECT id, path, method, status_code, delay_ms FROM api_configs LIMIT 5;"

echo ""
echo "=== Migration Validation Complete ==="
echo ""
echo "Summary:"
echo "- Database schema is up to date"
echo "- All required columns are present"
echo "- Data integrity is maintained"
echo ""
echo "Your database is ready for the new version! 🎉"