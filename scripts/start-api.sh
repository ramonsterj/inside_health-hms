#!/bin/bash
# Start Spring Boot API for HMS

cd "$(dirname "$0")/../api" || exit 1

echo "Starting Spring Boot API (dev profile)..."
./gradlew bootRun --args='--spring.profiles.active=dev'
