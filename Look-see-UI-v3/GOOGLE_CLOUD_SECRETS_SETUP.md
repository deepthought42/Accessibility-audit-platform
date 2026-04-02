# Google Cloud Secrets Setup for Auth0 Configuration

This guide explains how to use Google Cloud Secret Manager to securely populate your Auth0 configuration values.

## Prerequisites

1. **Google Cloud Project**: You need a Google Cloud project with Secret Manager API enabled
2. **Google Cloud CLI**: Already installed on your system
3. **Authentication**: You need to be authenticated with Google Cloud

## Step 1: Authenticate with Google Cloud

```bash
# Login to Google Cloud
gcloud auth login

# Set your project ID
gcloud config set project YOUR_PROJECT_ID

# Verify authentication
gcloud auth list
```

## Step 2: Enable Secret Manager API

```bash
# Enable the Secret Manager API
gcloud services enable secretmanager.googleapis.com
```

## Step 3: Create Secrets in Google Cloud

Create the following secrets in your Google Cloud project:

```bash
# Auth0 Domain
echo "your-auth0-domain.auth0.com" | gcloud secrets create auth0-domain --data-file=-

# Auth0 Client ID
echo "your-auth0-client-id" | gcloud secrets create auth0-client-id --data-file=-

# Auth0 Audience
echo "your-auth0-audience" | gcloud secrets create auth0-audience --data-file=-

# Auth0 API URI
echo "https://your-api-domain.com" | gcloud secrets create auth0-api-uri --data-file=-

# Auth0 App URI
echo "https://your-app-domain.com" | gcloud secrets create auth0-app-uri --data-file=-

# Auth0 Redirect URI
echo "https://your-app-domain.com/callback" | gcloud secrets create auth0-redirect-uri --data-file=-

# Auth0 Error Path
echo "/error" | gcloud secrets create auth0-error-path --data-file=-
```

## Step 4: Set Environment Variable

Set your Google Cloud project ID as an environment variable:

```bash
# Add to your shell profile (~/.bashrc, ~/.zshrc, etc.)
export GOOGLE_CLOUD_PROJECT="your-project-id"

# Or set it for the current session
export GOOGLE_CLOUD_PROJECT="your-project-id"
```

## Step 5: Update the Script

Edit `scripts/fetch-secrets.js` and replace `'your-project-id'` with your actual Google Cloud project ID:

```javascript
const PROJECT_ID = process.env.GOOGLE_CLOUD_PROJECT || 'your-actual-project-id';
```

## Step 6: Test the Setup

```bash
# Fetch secrets and update auth_config.json
npm run fetch-secrets

# Or run the script directly
node scripts/fetch-secrets.js
```

## Step 7: Verify the Configuration

Check that your `credentials/auth_config.json` file has been populated with the actual values from Google Cloud Secrets.

## Usage

### Development
- The secrets will be fetched automatically when you run `npm start` (due to the `prestart` script)
- You can manually fetch secrets anytime with `npm run fetch-secrets`

### Production
- The secrets will be fetched automatically when you run `npm run build` (due to the `prebuild` script)
- Ensure your deployment environment has the necessary Google Cloud authentication

## Security Best Practices

1. **Service Account**: For production, use a service account instead of user authentication
2. **IAM Permissions**: Grant minimal necessary permissions to the service account
3. **Secret Rotation**: Regularly rotate your secrets
4. **Environment Separation**: Use different secrets for different environments (dev, staging, prod)

## Troubleshooting

### Common Issues

1. **Authentication Error**: Make sure you're logged in with `gcloud auth login`
2. **Project Not Set**: Verify your project ID with `gcloud config get-value project`
3. **API Not Enabled**: Enable Secret Manager API with `gcloud services enable secretmanager.googleapis.com`
4. **Permission Denied**: Ensure your account has Secret Manager permissions

### Debug Commands

```bash
# Check authentication
gcloud auth list

# Check project
gcloud config get-value project

# List secrets
gcloud secrets list

# Test accessing a secret
gcloud secrets versions access latest --secret="auth0-domain"
```

## Alternative: Environment-Specific Configuration

You can also create environment-specific secret names:

```bash
# Development secrets
gcloud secrets create auth0-domain-dev --data-file=-

# Production secrets
gcloud secrets create auth0-domain-prod --data-file=-
```

Then modify the script to use environment-specific secret names based on `NODE_ENV`.

## Next Steps

1. Update your CI/CD pipeline to fetch secrets before building
2. Set up service account authentication for production
3. Implement secret rotation procedures
4. Add monitoring for secret access