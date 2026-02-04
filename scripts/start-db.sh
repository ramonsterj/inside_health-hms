#!/bin/bash
# Start PostgreSQL container for HMS

cd "$(dirname "$0")/.." || exit 1

echo "Starting PostgreSQL container..."
docker compose up postgres
