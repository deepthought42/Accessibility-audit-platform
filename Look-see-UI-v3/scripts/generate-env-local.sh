#!/bin/bash
# This script generates .env file from environment variables for local development
# Usage: Run this before starting the dev server or running tests

set -e

ENV_FILE=".env"

# Load existing .env if it exists (to preserve any manual edits)
if [ -f "$ENV_FILE" ]; then
  echo "ℹ️  Existing .env file found, loading current values..."
  set -a
  source "$ENV_FILE"
  set +a
fi

# When LOCAL_STACK=1 we are running inside the docker-compose dev stack and
# should default the API URL to the CrawlerAPI port on the host that the
# browser sees. Browser-side requests go through the host bridge.
if [ "${LOCAL_STACK:-0}" = "1" ]; then
  AUTH0_API_URI="${AUTH0_API_URI:-http://localhost:9080}"
  AUTH0_APP_URI="${AUTH0_APP_URI:-http://localhost:4200}"
fi

# Generate .env file from environment variables with defaults for local dev
cat > "$ENV_FILE" <<EOF
# This file is auto-generated from environment variables
# You can edit this file manually or set environment variables before running the script
# For local development, defaults are provided

NODE_ENV=${NODE_ENV:-development}
AUTH0_APP_URI=${AUTH0_APP_URI:-http://localhost:4200}
AUTH0_API_URI=${AUTH0_API_URI:-http://localhost:3000}
AUTH0_DOMAIN=${AUTH0_DOMAIN:-}
AUTH0_CLIENT_ID=${AUTH0_CLIENT_ID:-}
AUTH0_AUDIENCE=${AUTH0_AUDIENCE:-}
AUTH0_REDIRECT_URI=${AUTH0_REDIRECT_URI:-${AUTH0_APP_URI}}
AUTH0_ERROR_PATH=${AUTH0_ERROR_PATH:-/error}
PUSHER_APP_ID=${PUSHER_APP_ID:-}
PUSHER_KEY=${PUSHER_KEY:-}
PUSHER_SECRET=${PUSHER_SECRET:-}
PUSHER_CLUSTER=${PUSHER_CLUSTER:-}
SEGMENT_KEY=${SEGMENT_KEY:-}
EOF

echo "✅ Generated .env file for local development"
echo "📄 .env file location: $ENV_FILE"
