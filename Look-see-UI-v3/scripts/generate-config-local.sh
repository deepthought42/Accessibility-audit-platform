#!/bin/bash
# This script generates config.json from environment variables for local development
# Usage: Run this before starting the dev server

set -e

# Determine output path based on build type
if [ -d "dist/look-see-ui-v3/browser" ]; then
  CONFIG_FILE="dist/look-see-ui-v3/browser/config.json"
elif [ -d "dist" ]; then
  CONFIG_FILE="dist/config.json"
else
  # For tests and dev server, create in src so it gets served by karma/ng serve
  mkdir -p src
  CONFIG_FILE="src/config.json"
fi

# Generate .env file first if it doesn't exist
if [ ! -f .env ]; then
  echo "ℹ️  .env file not found, generating from environment variables..."
  bash scripts/generate-env-local.sh
fi

# Load .env file
set -a
source .env
set +a

# Get values from .env file (already loaded above)
AUTH0_APP_URI="${AUTH0_APP_URI:-http://localhost:4200}"
AUTH0_API_URI="${AUTH0_API_URI:-http://localhost:3000}"
AUTH0_DOMAIN="${AUTH0_DOMAIN:-}"
AUTH0_CLIENT_ID="${AUTH0_CLIENT_ID:-}"
AUTH0_AUDIENCE="${AUTH0_AUDIENCE:-}"
AUTH0_REDIRECT_URI="${AUTH0_REDIRECT_URI:-${AUTH0_APP_URI}}"
AUTH0_ERROR_PATH="${AUTH0_ERROR_PATH:-/error}"
PUSHER_APP_ID="${PUSHER_APP_ID:-}"
PUSHER_KEY="${PUSHER_KEY:-}"
PUSHER_SECRET="${PUSHER_SECRET:-}"
PUSHER_CLUSTER="${PUSHER_CLUSTER:-}"
SEGMENT_KEY="${SEGMENT_KEY:-}"
NODE_ENV="${NODE_ENV:-development}"

# Handle audience - set to null if empty or placeholder
if [ -z "$AUTH0_AUDIENCE" ] || [ "$AUTH0_AUDIENCE" = "YOUR_API_IDENTIFIER" ] || [ "$AUTH0_AUDIENCE" = "YOUR_AUTH0_AUDIENCE" ]; then
  AUDIENCE_VALUE="null"
else
  AUDIENCE_VALUE="\"$AUTH0_AUDIENCE\""
fi

# Ensure directory exists
mkdir -p "$(dirname "$CONFIG_FILE")"

# Generate config.json
cat > "$CONFIG_FILE" <<EOF
{
  "production": $([ "$NODE_ENV" = "production" ] && echo "true" || echo "false"),
  "base_url": "$AUTH0_APP_URI",
  "api_url": "$AUTH0_API_URI",
  "auth": {
    "domain": "$AUTH0_DOMAIN",
    "clientId": "$AUTH0_CLIENT_ID",
    "authorizationParams": {
      "audience": $AUDIENCE_VALUE,
      "redirect_uri": "$AUTH0_REDIRECT_URI"
    },
    "errorPath": "$AUTH0_ERROR_PATH"
  },
  "httpInterceptor": {
    "allowedList": ["$AUTH0_API_URI/*"]
  },
  "pusher": {
    "app_id": "$PUSHER_APP_ID",
    "key": "$PUSHER_KEY",
    "secret": "$PUSHER_SECRET",
    "cluster": "$PUSHER_CLUSTER"
  },
  "segment_key": "$SEGMENT_KEY"
}
EOF

echo "✅ Generated config.json for local development"
echo "📄 Config file location: $CONFIG_FILE"
