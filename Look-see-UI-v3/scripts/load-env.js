const fs = require('fs');
const path = require('path');

// Load environment variables from .env file if it exists
function loadEnvFile() {
  const envPath = path.join(__dirname, '..', '.env');
  
  if (fs.existsSync(envPath)) {
    const envContent = fs.readFileSync(envPath, 'utf8');
    const lines = envContent.split('\n');
    
    lines.forEach(line => {
      // Skip comments and empty lines
      const trimmedLine = line.trim();
      if (!trimmedLine || trimmedLine.startsWith('#')) {
        return;
      }
      
      const match = trimmedLine.match(/^([^=]+)=(.*)$/);
      if (match) {
        const key = match[1].trim();
        let value = match[2].trim();
        
        // Remove quotes if present
        if ((value.startsWith('"') && value.endsWith('"')) || 
            (value.startsWith("'") && value.endsWith("'"))) {
          value = value.slice(1, -1);
        }
        
        // Only set if not already in process.env (system env vars take precedence)
        if (!process.env[key]) {
          process.env[key] = value;
        }
      }
    });
    
    console.log('✅ Loaded environment variables from .env file');
  } else {
    console.log('ℹ️  No .env file found, using system environment variables only');
  }
}

// Helper function to safely quote strings for TypeScript
function quoteValue(value) {
  if (value === undefined || value === null || value === '') {
    return "''";
  }
  // Escape single quotes and wrap in single quotes
  return `'${String(value).replace(/'/g, "\\'")}'`;
}

// Helper function to handle optional values
function optionalValue(value, defaultValue = '') {
  if (value === undefined || value === null || value === '') {
    return defaultValue ? quoteValue(defaultValue) : "undefined";
  }
  return quoteValue(value);
}

// Generate environment.ts file from environment variables
function generateEnvironmentFile() {
  const isProduction = process.env.NODE_ENV === 'production';
  
  // Get values with fallbacks
  const auth0AppUri = process.env.AUTH0_APP_URI || 'http://localhost:4200';
  const auth0ApiUri = process.env.AUTH0_API_URI || 'http://localhost:3000';
  const auth0Domain = process.env.AUTH0_DOMAIN || '';
  const auth0ClientId = process.env.AUTH0_CLIENT_ID || '';
  const auth0Audience = process.env.AUTH0_AUDIENCE || '';
  const auth0RedirectUri = process.env.AUTH0_REDIRECT_URI || auth0AppUri;
  const auth0ErrorPath = process.env.AUTH0_ERROR_PATH || '/error';
  const pusherAppId = process.env.PUSHER_APP_ID || '';
  const pusherKey = process.env.PUSHER_KEY || '';
  const pusherSecret = process.env.PUSHER_SECRET || '';
  const pusherCluster = process.env.PUSHER_CLUSTER || '';
  const segmentKey = process.env.SEGMENT_KEY || '';
  
  // Handle audience - set to undefined if empty or placeholder
  const audienceValue = (auth0Audience && auth0Audience !== 'YOUR_API_IDENTIFIER' && auth0Audience !== 'YOUR_AUTH0_AUDIENCE') 
    ? quoteValue(auth0Audience) 
    : 'undefined';
  
  const envTemplate = `// This file can be replaced during build by using the \`fileReplacements\` array.
// \`ng build --prod\` replaces \`environment.ts\` with \`environment.prod.ts\`.
// The list of file replacements can be found in \`angular.json\`.
// This file is auto-generated from environment variables. Do not edit manually.

export const environment = {
  production: ${isProduction},
  base_url: ${quoteValue(auth0AppUri)},
  api_url: ${quoteValue(auth0ApiUri)},
  auth: {
    domain: ${quoteValue(auth0Domain)},
    clientId: ${quoteValue(auth0ClientId)},
    authorizationParams: {
      audience: ${audienceValue},
      redirect_uri: ${quoteValue(auth0RedirectUri)},
    },
    errorPath: ${quoteValue(auth0ErrorPath)},
  },
  httpInterceptor: {
    allowedList: [${quoteValue(auth0ApiUri)} + "/*"],
    /*
          // Attach access tokens to any calls to '/api' (exact match)
          {
            uri: '"/audits',
            tokenOptions: {
              authorizationParams: {
                audience: authConfig.authorizationParams.audience,
              }
            },
          },
          {
            uri: '/audits/start-individual',
            tokenOptions: {
              authorizationParams: {
                audience: authConfig.authorizationParams.audience,
              }
            },
          },
          {
            uri: '/auditor/start-individual',
            tokenOptions: {
              authorizationParams: {
                audience: authConfig.authorizationParams.audience
              }
            },
          },
          {
            uri: 'https://api-stage.look-see.com"/audits/*',
            tokenOptions: {
              authorizationParams: {
                audience: authConfig.authorizationParams.audience,
              }
            },
          },
        ],
        */
  },
  pusher: {
    app_id: ${quoteValue(pusherAppId)},
    key: ${quoteValue(pusherKey)},
    secret: ${quoteValue(pusherSecret)},
    cluster: ${quoteValue(pusherCluster)}
  },
  segment_key: ${quoteValue(segmentKey)}
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as \`zone.run\`, \`zoneDelegate.invokeTask\`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
`;

  const envFilePath = path.join(__dirname, '..', 'src', 'environments', 'environment.ts');
  
  try {
    fs.writeFileSync(envFilePath, envTemplate);
    console.log('✅ Generated environment.ts from environment variables');
  } catch (error) {
    console.error('❌ Error writing environment.ts:', error);
    process.exit(1);
  }
}

// Main execution
loadEnvFile();
generateEnvironmentFile();

// Export for use in other scripts
module.exports = { loadEnvFile, generateEnvironmentFile };

