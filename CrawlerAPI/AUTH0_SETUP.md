# Auth0 Configuration

This project has Auth0 authentication support. The Auth0Service is a Spring singleton that loads once and provides user information retrieval capabilities.

## Architecture Overview

### Components

1. **Auth0Config** (`src/main/java/com/crawlerApi/config/Auth0Config.java`)
   - Java Bean that loads configuration from `auth0.properties`
   - Uses `@ConfigurationProperties(prefix = "auth0")` for automatic property binding
   - Singleton that loads once when the application starts

2. **Auth0Service** (`src/main/java/com/crawlerApi/service/Auth0Service.java`)
   - Singleton service bean that handles Auth0 API interactions
   - Provides methods to get user information and account details
   - Integrates with AccountService to retrieve user accounts

3. **SecurityConfig** (`src/main/java/com/crawlerApi/security/SecurityConfig.java`)
   - Configures JWT validation and security
   - Integrates with Auth0Service to provide user information methods
   - Handles Auth0 user ID extraction and account retrieval

## 1. Auth0 Application Setup

1. Create an Auth0 account at https://auth0.com
2. Create a new Application in your Auth0 dashboard
3. Configure the following settings:
   - **Application Type**: Single Page Application (SPA) or Regular Web Application
   - **Allowed Callback URLs**: Add your application's callback URL
   - **Allowed Logout URLs**: Add your application's logout URL
   - **Allowed Web Origins**: Add your application's domain

## 2. API Configuration

1. Create a new API in your Auth0 dashboard
2. Set the identifier to `https://api.your-domain.com` (or your preferred API identifier)
3. Enable RS256 signing algorithm
4. Note down the API identifier for configuration

## 3. Application Configuration

Update the `src/main/resources/auth0.properties` file with your Auth0 credentials:

```properties
auth0.domain=your-domain.auth0.com
auth0.issuer=https://your-domain.auth0.com/
auth0.apiAudience=https://api.your-domain.com

auth0.clientId=your-client-id
auth0.clientSecret=your-client-secret

auth0.audience:https://api.your-domain.com
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://your-domain.auth0.com/
spring.security.oauth2.client.registration.auth0.client-id=your-client-id
spring.security.oauth2.client.registration.auth0.client-secret=your-client-secret

auth0.securedRoute: /*
auth0.base64EncodedSecret: false
auth0.authorityStrategy: ROLES
auth0.defaultAuth0ApiSecurityEnabled: true
auth0.signingAlgorithm: RS256
```

## 4. Using the Auth0Service

### In Controllers

```java
@Autowired
private Auth0Service auth0Service;

// Get current user's account
Optional<Account> account = auth0Service.getCurrentUserAccount(principal);

// Get user information from Auth0
Optional<Map<String, Object>> userInfo = auth0Service.getUserInfo(accessToken);

// Get specific user attributes
Optional<String> username = auth0Service.getUsername(accessToken);
Optional<String> email = auth0Service.getEmail(accessToken);
Optional<String> nickname = auth0Service.getNickname(accessToken);

// Extract user ID from principal
String userId = auth0Service.extractUserId(principal.getName());
```

### In SecurityConfig

The SecurityConfig also provides the same methods:

```java
@Autowired
private SecurityConfig securityConfig;

// Get current user's account
Optional<Account> account = securityConfig.getCurrentUserAccount(principal);

// Get user information
Optional<String> username = securityConfig.getUsername(accessToken);
Optional<String> email = securityConfig.getEmail(accessToken);
```

## 5. Available Endpoints

The application provides several endpoints for user information:

- `GET /user/account` - Get current user's account information
- `GET /user/info` - Get user information from Auth0
- `GET /user/username` - Get username from Auth0
- `GET /user/email` - Get email from Auth0
- `GET /user/config/status` - Check Auth0 configuration status

## 6. Security Configuration

The application is configured to:
- Use JWT tokens for authentication
- Validate tokens against your Auth0 domain
- Require authentication for all endpoints except:
  - `/actuator/info`
  - `/actuator/health`
  - `/auditor/start-individual`
  - `/audits`
  - `/user/config/status`

## 7. User ID Handling

The application automatically handles Auth0 user IDs by removing the `auth0|` prefix from user IDs when looking up accounts in the database.

## 8. Testing

To test the Auth0 integration:

1. Start your application
2. Try to access a protected endpoint without a valid JWT token
3. You should receive a 401 Unauthorized response
4. Include a valid JWT token in the Authorization header: `Bearer <your-jwt-token>`
5. Test the user information endpoints with a valid token

## 9. Troubleshooting

- Ensure your Auth0 domain is correct
- Verify that your API identifier matches the audience in your JWT tokens
- Check that your application has the correct permissions in Auth0
- Verify that your JWT tokens are signed with RS256 algorithm
- Check the `/user/config/status` endpoint to verify configuration

## 10. Environment Variables

For production deployments, consider using environment variables instead of hardcoded values:

```properties
auth0.domain=${AUTH0_DOMAIN}
auth0.clientId=${AUTH0_CLIENT_ID}
auth0.clientSecret=${AUTH0_CLIENT_SECRET}
auth0.apiAudience=${AUTH0_API_AUDIENCE}
```

## 11. Singleton Benefits

The singleton architecture provides:
- **Single Instance**: Auth0Service loads once and is reused throughout the application
- **Configuration Management**: All Auth0 settings are centralized in Auth0Config
- **Type Safety**: Configuration values are strongly typed
- **Dependency Injection**: Clean separation of concerns
- **Easy Testing**: Configuration can be easily mocked or tested
- **Performance**: No repeated initialization of Auth0 API clients

## 12. Integrations configuration

The API exposes an integration layer (Jira, Slack, Github, etc.) that is configurable per account via REST. Integration config is stored per (account, integration type).

### Environment variables / application.properties

- **integrations.encryption.key** (optional): If set to a 16+ character string, integration config is encrypted at rest using AES-GCM. Omit or leave empty to store config as plain JSON.
- **integrations.product-board.private_key**: Used for Product Board JWT signing (existing).
- **integrations.\<type\>.\***: Per-integration app-level keys (e.g. `integrations.jira.private_key`, `integrations.slack.webhook_secret`) for signing or API defaults. See `application.properties` for the full list of placeholders.

### API endpoints

- `GET /v1/integrations` – list available integration types (metadata).
- `GET /v1/integrations/{type}` – metadata + current account config (masked).
- `GET /v1/integrations/{type}/config` – current account config (for backend use).
- `PUT /v1/integrations/{type}/config` – create/update config (body: JSON map; validated by provider).
- `DELETE /v1/integrations/{type}/config` – remove config.
- `POST /v1/integrations/{type}/test` – test connection using stored config.

All require an authenticated account (JWT). Other microservices can call these endpoints with the user’s JWT to read or update integration config.