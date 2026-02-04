#!/bin/bash
# Start all HMS services for Conductor

cd "$(dirname "$0")/.." || exit 1

# Start PostgreSQL (idempotent - safe if already running)
echo "Starting PostgreSQL..."
docker compose up -d postgres

# Wait for PostgreSQL to be ready (checks port 5433 on localhost)
echo "Waiting for PostgreSQL to be ready..."
until pg_isready -h localhost -p 5433 -U admin -d hms_db > /dev/null 2>&1 || \
      docker compose exec -T postgres pg_isready -U admin -d hms_db > /dev/null 2>&1; do
    sleep 1
done
echo "PostgreSQL is ready!"

# Start API in background
echo "Starting Spring Boot API..."
(cd api && ./gradlew bootRun --args='--spring.profiles.active=dev') &
API_PID=$!

# Wait a bit for API to start initializing
sleep 5

# Start Vue frontend (foreground - Conductor will see this output)
echo "Starting Vue frontend..."
cd web && npm run dev

# If frontend exits, also stop the API
kill $API_PID 2>/dev/null
