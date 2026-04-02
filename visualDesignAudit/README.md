# visualDesignAudit

A Spring Boot microservice that performs visual design audits on web pages, evaluating elements like color contrast, typography, imagery, and whitespace against WCAG 2.1 accessibility standards and design system guidelines.

## Overview

The Visual Design Audit service receives page audit requests via Google Cloud Pub/Sub and executes a suite of audit checks including:

- **Text Color Contrast** - Evaluates text-to-background color contrast against WCAG 2.1 AA/AAA standards
- **Non-Text Color Contrast** - Checks buttons and input elements for minimum 3:1 contrast ratio
- **Color Palette** - Validates that colors used on the page match the design system palette
- **Font Audit** - Checks font size consistency (minimum 12px) and header font weight consistency
- **Typefaces Audit** - Validates use of no more than 2 typefaces and typeface cascade consistency
- **Image Copyright** - Detects potential copyright issues by identifying stock/unlicensed images
- **Image Policy** - Checks images for policy violations (adult content, violence) per design system rules
- **Margin Audit** - Validates margins are multiples of 8 and not misused as padding
- **Padding Audit** - Validates padding values are multiples of 8 with proper units

## Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 2.6.13
- **Build Tool**: Maven 3.x
- **Cloud Platform**: Google Cloud Platform (Pub/Sub, Cloud Storage, Secret Manager)
- **Testing**: JUnit 4, Mockito, Spring Boot Test
- **API Documentation**: Swagger/OpenAPI 3.0
- **Code Generation**: Lombok

## Project Structure

```
src/
  main/
    java/com/looksee/visualDesignAudit/
      Application.java            # Spring Boot entry point
      AuditController.java        # REST controller for Pub/Sub messages
      audit/
        TextColorContrastAudit.java
        NonTextColorContrastAudit.java
        ColorPaletteAudit.java
        FontAudit.java
        TypefacesAudit.java
        ImageAudit.java
        ImagePolicyAudit.java
        MarginAudit.java
        PaddingAudit.java
    resources/
      application.properties      # Spring configuration
      application.yml
      logback.xml                  # Logging configuration
  test/
    java/com/looksee/visualDesignAudit/
      ApplicationTest.java
      AuditControllerTest.java
      audit/
        TextColorContrastAuditTest.java
        NonTextColorContrastAuditTest.java
        ColorPaletteAuditTest.java
        FontAuditTest.java
        TypefacesAuditTest.java
        ImageAuditTest.java
        ImagePolicyAuditTest.java
        MarginAuditTest.java
        PaddingAuditTest.java
    resources/
      test_messages/               # JSON fixtures for integration testing
```

## Prerequisites

- Java 17 (Eclipse Temurin recommended)
- Maven 3.x
- Google Cloud SDK (for deployment)
- Docker (for containerized deployment)

## Building

```bash
# Build the project
mvn clean package

# Run tests only
mvn test

# Skip tests during build
mvn clean package -DskipTests
```

## Running Locally

```bash
# Run with Maven
mvn spring-boot:run

# Run the JAR directly
java -jar target/visualDesignAudit-1.0.4.jar
```

The service starts on port 8080 by default (configurable in `application.properties`).

## Testing

The project uses JUnit 4 with Mockito for unit testing. Tests are organized to mirror the source code structure.

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=MarginAuditTest

# Run tests with verbose output
mvn test -Dsurefire.useFile=false
```

### Test Coverage

Tests cover all 11 source classes across the project:

| Class | Test File | Coverage Areas |
|-------|-----------|----------------|
| `Application` | `ApplicationTest` | Bootstrap configuration, annotations |
| `AuditController` | `AuditControllerTest` | Message handling, audit orchestration, duplicate detection |
| `TextColorContrastAudit` | `TextColorContrastAuditTest` | WCAG compliance levels, large/small text, contrast thresholds |
| `NonTextColorContrastAudit` | `NonTextColorContrastAuditTest` | Button/input contrast, border detection, WCAG levels |
| `ColorPaletteAudit` | `ColorPaletteAuditTest` | Palette matching, scoring |
| `FontAudit` | `FontAuditTest` | Font size consistency, header analysis, mobile readability |
| `TypefacesAudit` | `TypefacesAuditTest` | Typeface counting, cascade consistency |
| `ImageAudit` | `ImageAuditTest` | Copyright detection scoring |
| `ImagePolicyAudit` | `ImagePolicyAuditTest` | Adult/violent content detection, policy compliance |
| `MarginAudit` | `MarginAuditTest` | Multiple-of-8 validation, unit cleaning, margin-as-padding detection |
| `PaddingAudit` | `PaddingAuditTest` | Multiple-of-8 validation, spacing consistency |

## API

The service exposes a single POST endpoint at `/` that accepts Pub/Sub messages containing `PageAuditMessage` payloads.

### POST /

Receives a Pub/Sub message and executes the visual design audit.

**Request Body**: Pub/Sub message wrapper containing a Base64-encoded `PageAuditMessage`

**Responses**:
- `200 OK` - Audit completed successfully
- `400 Bad Request` - Invalid message format
- `500 Internal Server Error` - Audit execution failure

## Deploying to GCP

### Prerequisites

Before deploying, make sure you have the necessary credentials:

#### Retrieving GCP Project Credentials JSON

1. Go to the Google Cloud Console (https://console.cloud.google.com/).
2. Select your project.
3. Navigate to "IAM & Admin" > "Service Accounts".
4. Click on "Create Service Account" or select an existing one.
5. If creating a new account, give it a name and grant it the necessary permissions.
6. On the service account details page, go to the "Keys" tab.
7. Click "Add Key" > "Create new key".
8. Choose JSON as the key type and click "Create".
9. The JSON file will be downloaded to your computer. Keep it secure.

#### Retrieving Gmail Credentials JSON

1. Go to the Google Cloud Console (https://console.cloud.google.com/).
2. Select your project.
3. Navigate to "APIs & Services" > "Credentials".
4. Click "Create Credentials" > "OAuth client ID".
5. Select "Desktop app" as the application type.
6. Click "Create" and download the JSON file.
7. Rename the file to `credentials.json` and place it in your project directory.

### Docker Deployment

```bash
gcloud auth print-access-token | sudo docker login -u oauth2accesstoken --password-stdin https://us-central1-docker.pkg.dev

sudo docker build --no-cache -t us-central1-docker.pkg.dev/cosmic-envoy-280619/visual-design-audit/0.0.2 .

sudo docker push us-central1-docker.pkg.dev/cosmic-envoy-280619/visual-design-audit/0.0.2
```

### Adding Secrets to GCP Secret Manager

1. Go to the Google Cloud Console (https://console.cloud.google.com/).
2. Select your project.
3. Navigate to "Security" > "Secret Manager".
4. Click "Create Secret" for each of the following secrets:

   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `GCP_PROJECT_ID`
   - `GCP_BUCKET_NAME`

5. For each secret, provide a name and the corresponding value from your `application.properties` file.

### Required Values

Set the following values in your secrets:

1. From `application.properties`:
   - `SPRING_DATASOURCE_URL`: Your database URL (e.g., `jdbc:postgresql://[HOST]:[PORT]/[DATABASE]`)
   - `SPRING_DATASOURCE_USERNAME`: Your database username
   - `SPRING_DATASOURCE_PASSWORD`: Your database password
   - `GCP_PROJECT_ID`: Your Google Cloud Project ID
   - `GCP_BUCKET_NAME`: The name of your Google Cloud Storage bucket

## Configuration

Key configuration properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | HTTP server port |
| `management.server.port` | `80` | Management endpoint port |
| `management.health.pubsub.enabled` | `false` | Pub/Sub health check |
| `logging.file` | `look-see.log` | Log file path |

## License

ISC License - see [LICENSE](LICENSE) for details.
