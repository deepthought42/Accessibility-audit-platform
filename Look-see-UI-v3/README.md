# Look-see UI

Look-see is an AI-powered UX audit platform that helps businesses improve their website accessibility and user experience. The platform automatically analyzes websites for visual design, information architecture, and content quality issues while ensuring WCAG 2.1 compliance.

## What is Look-see?

LookSee is a comprehensive UX audit tool that:

- **Automatically audits websites** for UX issues using advanced AI
- **Ensures WCAG 2.1 compliance** for accessibility standards
- **Provides detailed reports** with actionable recommendations
- **Tracks usability scores** over time to measure improvements
- **Supports both single-page and full-site audits**

### Key Features

- **Visual Design Analysis**: Color palette, typography, whitespace, and branding evaluation
- **Information Architecture**: SEO optimization, menu analysis, and performance assessment
- **Content Quality**: Written content readability, imagery relevance, and media accessibility
- **Real-time Updates**: Live progress tracking via WebSocket connections
- **Export Capabilities**: Generate detailed reports in Excel format
- **Team Collaboration**: Share results with stakeholders and team members

## Third-Party Services Configuration

Look-see requires several third-party services to function properly. Configuration files are located in the `credentials/` directory.

### 1. Auth0 Authentication

**File**: `credentials/auth_config.json`

```json
{
  "domain": "your-auth0-domain.auth0.com",
  "clientId": "your-auth0-client-id",
  "authorizationParams": {
    "audience": "https://your-api-domain.com"
  },
  "apiUri": "https://your-api-domain.com",
  "appUri": "https://your-app-domain.com",
  "errorPath": "/error",
  "redirect_uri": "https://your-app-domain.com/audits"
}
```

**Setup Steps**:
1. Create an Auth0 account and application
2. Configure the application type as "Single Page Application"
3. Set up your callback URLs and allowed origins
4. Create an API in Auth0 and note the audience identifier
5. Copy the domain and client ID to your config file

### 2. Pusher WebSocket Service

**File**: `credentials/pusher_config.json`

```json
{
  "app_id": "your-pusher-app-id",
  "key": "your-pusher-key",
  "secret": "your-pusher-secret",
  "cluster": "your-cluster-region"
}
```

**Setup Steps**:
1. Create a Pusher account
2. Create a new app in your dashboard
3. Note the app ID, key, secret, and cluster
4. Update the config file with your credentials

### 3. Segment Analytics

**File**: `credentials/segment_config.json`

```json
{
  "key": "your-segment-write-key"
}
```

**Setup Steps**:
1. Create a Segment account
2. Create a new source (JavaScript)
3. Copy the write key to your config file

## Analytics with Segment

### Overview

Segment is used as a **Customer Data Platform (CDP)** for analytics and user behavior tracking. It serves as a central hub for collecting user interactions and funneling them to various analytics and marketing tools, providing comprehensive insights into user behavior and application usage.

### Implementation Details

#### Initialization & Setup
- **Script Loading**: Segment is loaded via the standard Segment snippet in `src/index.html`
- **Write Key**: Configured in `credentials/segment_config.json`
- **Environment**: Segment key accessible via `environment.segment_key`
- **User Identification**: Auth0 user IDs are sent to Segment for user tracking

#### User Identification
```typescript
// User identification in nav-bar.component.ts
window.analytics.identify(user?.sub, {
  // User properties can be added here
});
```

### Event Tracking Categories

#### 1. Audit-Related Events
- `'Click start single page UX audit'` - Page audit initiation
- `'Click start Full site UX audit'` - Domain audit initiation
- `'UX Audit Viewed'` - Audit results viewing
- `'View Quick-Audit Page'` - Quick audit page views
- `'Viewed UX Audit Form'` - Audit form views

#### 2. Report & Export Events
- `'Clicked Export Page Report button'` - Page report exports (with login status)
- `'Clicked Export Domain Report button'` - Domain report exports
- `'Request Report'` - Email report requests

#### 3. User Management Events
- `'Clicked login button'` - Login button interactions
- `'Clicked logout button'` - Logout button interactions
- `'Added Domain'` - New domain additions
- `'Request access to [feature]'` - Feature access requests

#### 4. Content Management Events
- `'Added recommendation'` - Recommendation additions
- `'Deleted Recommendation'` - Recommendation deletions
- `'Added observation'` - Observation additions

#### 5. Competitive Analysis Events
- `'Clicked competitive analysis'` - Competitive analysis feature usage
- `'Clicked analyze button in competitive analysis'` - Analysis button interactions

### Technical Implementation

#### SegmentIOService (`segmentio.service.ts`)
The centralized service for all Segment tracking operations:

```typescript
// Example tracking methods
sendUxAuditStartedMessage(page_url: string) {
  window.analytics.track('Click start single page UX audit', {
    url: page_url
  });
}

trackExportReportAuthenticatedClick(page_url: string, page_key: string) {
  window.analytics.track('Clicked Export Page Report button', {
    url: page_url,
    page_key: page_key,
    is_logged_in: true
  });
}

sendRecommendationAddedMessage(issue_key: string, recommendation: string) {
  window.analytics.track('Added recommendation', {
    issue_key: issue_key,
    recommendation: recommendation
  });
}
```

#### Integration Points
**Components using Segment tracking**:
- `audit-form.component.ts` - Audit initiation tracking
- `audit-dashboard.component.ts` - Audit viewing and interaction tracking
- `nav-bar.component.ts` - User identification and export tracking
- `page-audit-review.component.ts` - Page review and export tracking
- `audit.service.ts` - Report request tracking
- `domain.service.ts` - Domain-related action tracking

### Data Flow

1. **User Action**: User performs an action (clicks button, starts audit, etc.)
2. **Event Trigger**: Component calls appropriate SegmentIOService method
3. **Data Collection**: Service sends event with relevant context data to Segment
4. **Data Distribution**: Segment forwards data to configured destinations (analytics tools, marketing platforms, etc.)

### Event Data Structure

Each tracked event includes relevant context data:

```typescript
// Example event structure
{
  event: 'Click start single page UX audit',
  properties: {
    url: 'https://example.com',
    timestamp: '2024-01-01T00:00:00Z',
    user_id: 'auth0|123456789'
  }
}
```

### Benefits

1. **Centralized Analytics**: Single source of truth for user behavior data
2. **Multi-destination Support**: Data can be sent to multiple analytics and marketing tools
3. **User Journey Tracking**: Complete visibility into user interactions and progression
4. **Feature Usage Analytics**: Understanding which features are most/least used
5. **Conversion Tracking**: Monitor user progression through the application funnel
6. **A/B Testing Support**: Data feeds into testing and optimization tools
7. **Privacy Compliance**: Built-in support for GDPR and privacy regulations

### Privacy & Compliance

- **Privacy Policy**: Segment usage is documented in `src/static-pages/privacy.html`
- **Third-party Integration**: Listed alongside other analytics tools (Heap Analytics, Google Analytics, Facebook Advertising)
- **Cookie Usage**: Uses first-party and third-party cookies for tracking
- **User Consent**: Respects user privacy preferences and consent management

### Configuration Requirements

- **Segment Account**: Active Segment account with JavaScript source
- **Write Key**: Valid write key configured in `credentials/segment_config.json`
- **Environment Setup**: Segment key properly configured in `src/environments/environment.ts`
- **Destination Configuration**: Analytics and marketing tools configured in Segment dashboard

### 4. Stripe Payment Processing

Stripe is used for subscription billing and payment processing. Configuration is handled through the backend API.

**Setup Steps**:
1. Create a Stripe account
2. Configure webhook endpoints in your Stripe dashboard
3. Set up subscription products and pricing plans
4. Configure the backend API with your Stripe keys

## Development Setup

### Prerequisites

- **Node.js**: Version 18.19.1 or higher
- **npm**: Version 10.2.4 or higher
- **Angular CLI**: Version 18.1.4 or higher
- **TypeScript**: Version 5.5.4 or higher

### Installation

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd Look-see-UI-v3
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Configure third-party services**:
   - Copy `credentials/auth_config_template.json` to `credentials/auth_config.json`
   - Update all configuration files with your service credentials
   - Ensure `src/environments/environment.ts` is properly configured

4. **Start the development server**:
   ```bash
   ng serve
   ```

5. **Navigate to the application**:
   Open your browser and go to `http://localhost:4200/`

### Development Commands

- **Start development server**: `ng serve`
- **Build for production**: `ng build`
- **Run unit tests**: `ng test`
- **Run end-to-end tests**: `ng e2e`
- **Lint code**: `ng lint`

## Deployment Requirements

### Frontend Server Requirements

#### Production Environment

- **Web Server**: Nginx (latest version)
- **SSL Certificate**: Valid SSL certificate for your domain
- **Domain**: Configured domain name with DNS pointing to your server
- **Ports**: 80 (HTTP redirect) and 443 (HTTPS)

#### Server Specifications

- **CPU**: Minimum 1 vCPU, recommended 2+ vCPU
- **RAM**: Minimum 2GB, recommended 4GB+
- **Storage**: Minimum 10GB, recommended 20GB+
- **OS**: Linux (Ubuntu 20.04+ recommended)

#### Docker Deployment

The application includes a multi-stage Dockerfile for containerized deployment:

```bash
# Build the Docker image
docker build --no-cache -t your-registry/look-see-ui:version .

# Push to registry
docker push your-registry/look-see-ui:version
```

#### Manual Deployment

1. **Build the application**:
   ```bash
   npm run build
   ```

2. **Configure Nginx**:
   - Copy the appropriate nginx config file (`nginx.prod.conf`, `nginx.stage.conf`, or `nginx.demo.conf`)
   - Update the server_name and SSL certificate paths
   - Place SSL certificates in `/etc/ssl/`

3. **Deploy files**:
   - Copy built files from `dist/look-see-ui-v3/browser/` to your web server directory
   - Ensure proper file permissions

### Backend API Requirements

The frontend requires a backend API service with the following endpoints:

- **Authentication**: `/accounts`
- **Audits**: `/audits`, `/auditrecords`
- **Domains**: `/domains`
- **Observations**: `/observations`
- **Reports**: `/audits/{id}/excel`

### Environment Variables

Ensure the following environment variables are set:

- `NODE_ENV`: Set to `production` for production builds
- API endpoints configured in `src/environments/environment.ts`
- All third-party service credentials properly configured

## Architecture Overview

### Frontend Architecture

- **Framework**: Angular 17.3.2
- **UI Library**: Angular Material + Tailwind CSS
- **State Management**: RxJS observables and services
- **Authentication**: Auth0 Angular SDK
- **Real-time Updates**: Pusher WebSocket service
- **Analytics**: Segment.io integration

### Key Components

- **Audit Dashboard**: Displays audit results and statistics
- **Audit List**: Shows all user audits with status
- **Page Review**: Detailed view of individual page audits
- **Domain Management**: Add and manage website domains
- **User Profile**: Account settings and subscription management

### Data Flow

1. **User Authentication**: Auth0 handles user login and JWT token management
2. **Audit Initiation**: User submits URL for audit via the frontend
3. **Backend Processing**: API processes the audit request
4. **Real-time Updates**: Pusher provides live progress updates
5. **Results Display**: Frontend renders audit results and recommendations
6. **Report Generation**: Users can export detailed reports

## Real-time Events with Pusher

### Overview

Pusher is used as a real-time WebSocket service to provide live updates during website audits. It enables the frontend to receive real-time notifications from the backend about audit progress and status changes without requiring page refreshes.

### Real-time Events

#### 1. Audit Progress Updates (`auditUpdate`)
- **Purpose**: Sends live updates about audit progress and status changes
- **Trigger**: Backend sends progress updates during audit processing
- **Data**: JSON payload containing audit statistics and progress information
- **Usage**: Updates the audit dashboard with real-time progress indicators

#### 2. New Page Discovery (`pageFound`)
- **Purpose**: Notifies the frontend when new pages are discovered during domain audits
- **Trigger**: Backend crawler finds new pages during website analysis
- **Data**: JSON payload containing page information and audit data
- **Usage**: Dynamically adds new pages to the audit list

### Channel Management

#### User-Specific Channels
- **Channel Naming**: Uses the user's Auth0 ID (with `|` character removed) as the channel name
- **Isolation**: Each user receives updates only for their own audits
- **Security**: Private channels ensure data privacy and user isolation

#### Channel Lifecycle
1. **Subscription**: User subscribes to their personal channel upon login
2. **Event Reception**: Real-time events are received and processed
3. **Cleanup**: Channels are unsubscribed when user logs out or navigates away

### Technical Implementation

#### WebSocket Service (`web-socket.service.ts`)
```typescript
// Subscribe to user-specific channel
this.webSocketService.listenChannel(userChannelName, 'auditUpdate', (response) => {
  const auditMsg = JSON.parse(response);
  // Update UI with real-time data
});

// Listen for new page discoveries
this.webSocketService.listenChannel(userChannelName, 'pageFound', (response) => {
  const pageMsg = JSON.parse(response);
  // Add new page to audit list
});
```

#### Configuration Requirements
- **Pusher Credentials**: Configured in `credentials/pusher_config.json`
- **Environment Setup**: Pusher settings in `src/environments/environment.ts`
- **Dependencies**: `pusher-js` library (version 7.6.0)

### Benefits

1. **Enhanced User Experience**: Real-time updates provide immediate feedback
2. **Efficient Communication**: WebSocket connection is more efficient than polling
3. **Scalable Infrastructure**: Pusher handles WebSocket scaling and management
4. **User Isolation**: Private channels ensure data security and privacy
5. **Reduced Server Load**: Eliminates need for frequent API polling

### Event Flow Example

1. **Audit Initiation**: User starts a domain audit
2. **Backend Processing**: Backend begins crawling and analyzing the website
3. **Progress Updates**: Backend sends `auditUpdate` events with progress information
4. **Page Discovery**: When new pages are found, `pageFound` events are sent
5. **UI Updates**: Frontend receives events and updates the interface in real-time
6. **Completion**: User sees live progress without manual page refreshes

### Error Handling

- **Connection Failures**: Automatic reconnection attempts
- **Event Processing**: Graceful handling of malformed JSON data
- **Channel Cleanup**: Proper unsubscription to prevent memory leaks
- **Fallback**: Graceful degradation if WebSocket connection fails

## Security Considerations

- **Authentication**: JWT tokens managed by Auth0
- **HTTPS**: All production traffic must use HTTPS
- **CORS**: Configured to allow only authorized domains
- **API Security**: All API calls require valid authentication tokens
- **SSL Certificates**: Valid SSL certificates required for production

## Support and Contact

For technical support or questions about Look-see:

- **Email**: support@Look-see.com
- **Phone**: (617) 453-8134
- **Address**: 59 Fountain St. #526, Framingham, MA 01702

## License

This project is proprietary software owned by Look-see, Inc. All rights reserved.


##Deploy

docker build --no-cache -t gcr.io/cosmic-envoy-280619/look-see-ui:vx.x.x .
docker push gcr.io/cosmic-envoy-280619/look-see-ui:vx.x.x

    ```bash
    gcloud auth print-access-token | sudo docker login -u oauth2accesstoken --password-stdin https://us-central1-docker.pkg.dev
	sudo docker build --no-cache -t us-central1-docker.pkg.dev/cosmic-envoy-280619/user-interface/#.#.# .
	sudo docker push us-central1-docker.pkg.dev/cosmic-envoy-280619/user-interface/#.#.#
    ```

//Configuring Auth0


