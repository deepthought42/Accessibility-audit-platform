# Environment Variables Setup

This project uses environment variables to configure Auth0, Pusher, and other services. Environment variables can be loaded from:

1. **System environment variables** (highest priority)
2. **`.env` file** in the project root (if it exists)

## Quick Start

### 1. Create a `.env` file

Copy the example file and fill in your values:

```bash
cp env.example .env
```

Then edit `.env` with your actual configuration values.

### 2. Start the development server

```bash
npm start
```

The script will automatically:
- Load environment variables from `.env` file (if it exists)
- Load system environment variables (takes precedence)
- Generate `src/environments/environment.ts` with the actual values
- Start the Angular development server

## Available Environment Variables

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `AUTH0_DOMAIN` | Your Auth0 domain | Yes | `your-app.auth0.com` |
| `AUTH0_CLIENT_ID` | Your Auth0 client ID | Yes | `abc123def456` |
| `AUTH0_AUDIENCE` | Your API identifier | Optional | `https://api.yourdomain.com` |
| `AUTH0_API_URI` | Your API base URL | Yes | `https://api.yourdomain.com` |
| `AUTH0_APP_URI` | Your application URL | Yes | `https://app.yourdomain.com` |
| `AUTH0_REDIRECT_URI` | Auth0 redirect URI | Optional | `https://app.yourdomain.com/audits` |
| `AUTH0_ERROR_PATH` | Error page path | Optional | `/error` |
| `PUSHER_APP_ID` | Pusher app ID | Optional | `123456` |
| `PUSHER_KEY` | Pusher key | Optional | `abc123def456` |
| `PUSHER_SECRET` | Pusher secret | Optional | `secret123` |
| `PUSHER_CLUSTER` | Pusher cluster | Optional | `us2` |
| `SEGMENT_KEY` | Segment.io write key | Optional | `abc123` |
| `NODE_ENV` | Environment mode | Optional | `development` or `production` |

## How It Works

1. **Environment Loading**: The `scripts/load-env.js` script:
   - Reads `.env` file if it exists
   - Loads variables into `process.env`
   - System environment variables take precedence over `.env` file values

2. **Environment File Generation**: The script generates `src/environments/environment.ts` with:
   - Actual values from environment variables (not `process.env` references)
   - Proper TypeScript syntax
   - Fallback values for development

3. **Build Process**: 
   - `npm start` → Loads env vars → Generates environment.ts → Starts dev server
   - `npm run build` → Loads env vars → Generates environment.ts → Builds app

## NPM Scripts

- `npm start` - Start dev server with environment variables (recommended)
- `npm run start:direct` - Start dev server without loading env vars
- `npm run build` - Build for production with environment variables
- `npm run build:direct` - Build without loading env vars
- `npm run load-env` - Manually load environment variables and generate environment.ts

## Using System Environment Variables

You can also set environment variables directly on your system:

### Linux/macOS:
```bash
export AUTH0_DOMAIN=your-domain.auth0.com
export AUTH0_CLIENT_ID=your-client-id
npm start
```

### Windows (PowerShell):
```powershell
$env:AUTH0_DOMAIN="your-domain.auth0.com"
$env:AUTH0_CLIENT_ID="your-client-id"
npm start
```

### Windows (CMD):
```cmd
set AUTH0_DOMAIN=your-domain.auth0.com
set AUTH0_CLIENT_ID=your-client-id
npm start
```

System environment variables take precedence over `.env` file values.

## Production Deployment

For production deployments, set environment variables in your deployment platform:

- **Docker**: Use `--env-file .env` or `-e` flags
- **Kubernetes**: Use ConfigMaps or Secrets
- **Cloud Platforms**: Use their environment variable configuration
- **CI/CD**: Set environment variables in your pipeline

Example Docker:
```bash
docker run --env-file .env your-image
```

## Troubleshooting

### Environment variables not loading
- Check that your `.env` file is in the project root
- Verify variable names match exactly (case-sensitive)
- Check for syntax errors in `.env` file (no spaces around `=`)

### Generated environment.ts has empty values
- Ensure environment variables are set correctly
- Check that variable names match the expected format
- Run `npm run load-env` to see what values are being loaded

### Changes to .env not taking effect
- Restart the dev server after changing `.env`
- Run `npm run load-env` to regenerate environment.ts manually

## Security Notes

- **Never commit `.env` file** - It's already in `.gitignore`
- **Use different values** for development, staging, and production
- **Rotate credentials** regularly
- **Use secrets management** in production (e.g., Kubernetes Secrets, AWS Secrets Manager)

