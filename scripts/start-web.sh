#!/bin/bash
# Start Vue frontend for HMS

cd "$(dirname "$0")/../web" || exit 1

echo "Starting Vue dev server..."
npm run dev
