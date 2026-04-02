#!/bin/bash
# This script generates .env file from system environment variables
# It runs at container startup before generating config.json

set -e

ENV_FILE="/usr/share/nginx/html/.env"

# Get values from system environment variables
cat > "$ENV_FILE" <<EOF
# This file is auto-generated from system environment variables at container startup
# Do not edit manually

NODE_ENV=${NODE_ENV:-production}
AUTH0_APP_URI=${AUTH0_APP_URI:-}
AUTH0_API_URI=${AUTH0_API_URI:-}
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

echo "✅ Generated .env file from system environment variables"
echo "📄 .env file location: $ENV_FILE"
