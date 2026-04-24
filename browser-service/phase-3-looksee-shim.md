# Phase 3 — LookseeCore Browsing-Client Shim

> **Goal:** LookseeCore ships a new `looksee-browsing-client` Maven module and a thin `RemoteBrowser` in `looksee-core` that lets every downstream service (PageBuilder, element-enrichment, journeyExecutor, audits, and friends) drive browsing either **locally** (current in-process behavior, byte-identical default) or **remotely** over HTTP to `brandonkindred/browser-service`, chosen by a single config flag.
>
> **Where to work:** locally in the Look-see monorepo, against the `LookseeCore/` subtree. Suggest a feature branch: `git checkout -b phase-3/looksee-shim`.
>
> **Scope guard:** this phase adds the shim and wires **page-level** ops (navigate, screenshot, source, close, capture). Element-handle ops (`findElement`, `extractAttributes`, journey step execution) stay local-only — they're phase **3b**, and every `RemoteBrowser` method that would need them throws `UnsupportedOperationException` with a pointer to the phase-3b task list. See [§14 Open items](#14-open-items-flagged-for-reviewer).
>
> **Sibling reference:** this doc mirrors [`phase-1a-engine-upgrade.md`](./phase-1a-engine-upgrade.md) in structure (step-by-step, per-step commits, definition-of-done). Cross-links appear where the phase-1a engine work constrains what the client can assume.

## Why this phase exists now

Phase 0 locked the OpenAPI contract for `brandonkindred/browser-service`. Phase 1a is upgrading the engine to Selenium 4 / Appium 9 / Java 21 behind that contract. Neither of those lets any Look-see consumer *call* the new service. Without a client shim, every consumer (PageBuilder, element-enrichment, journeyExecutor, audits, and ~6 others) remains hardwired to the in-tree `LookseeCore/looksee-browser` engine and can't be cut over once browser-service deploys.

Shipping the shim as a config-flagged default-local change means:

- Every consumer picks it up for free by bumping LookseeCore 0.5.0 → 0.6.0; no code edit required.
- Cutover becomes a flag flip (`looksee.browsing.mode=remote` + `service-url`) per-consumer on its own timeline.
- Local mode keeps backing everything until every consumer has been validated on remote. The `looksee-browser` engine module stays untouched in this phase.

## Scope

| In scope | Out of scope |
|---|---|
| New Maven module `LookseeCore/looksee-browsing-client/` | Any change to `LookseeCore/looksee-browser/` source |
| `openapi-generator-maven-plugin` wired with `<library>native</library>` (JDK `HttpClient`) | Forcing Spring / OkHttp / Jersey onto consumers |
| Hand-written `BrowsingClient` facade over the generated `*Api` classes | Auto-generated service-layer glue beyond the DTO surface |
| `LookseeBrowsingProperties` + `@ConditionalOnProperty` bean wiring | Per-consumer `application.yml` edits (phase 4) |
| `RemoteBrowser extends Browser` — page-level overrides | Element-handle remote ops (phase 3b) |
| Fork `BrowserService.getConnection()` and `TestService.getConnection()` on the mode flag | Touching any other caller of `BrowserConnectionHelper.getConnection` outside looksee-core |
| New `BrowserService.capturePage(URL, BrowserType, long)` one-shot helper + new `PageStateAdapter.toPageState(byte[], String, long, String)` overload | Rewriting existing multi-step audit flows to use `capturePage` (opt-in migration, phase 4) |
| Unit tests for `BrowsingClient`, `RemoteBrowser`, and mode-fork in `BrowserService` | Live integration tests against a running browser-service (flagged as optional manual verification) |
| `A11yParent` 0.5.0 → 0.6.0 + CHANGELOG entry | Publishing to a remote Maven repo (handled by existing release pipeline) |

If something looks refactor-tempting in `Browser.java`, `BrowserService.java`, or `PageStateAdapter.java` — resist. The goal is the shim plus two surgical call-site forks, nothing else.

## Prerequisites

```bash
java -version     # 17.x (LookseeCore is still on Java 17; do not bump)
mvn -v            # 3.9+
git status        # clean working tree
```

Network access during build is required once: `openapi-generator-maven-plugin` resolves its generator deps from Maven Central on first run, then caches in `~/.m2`.

Spring Boot note: LookseeCore is on Spring Boot 2.6.x. `@ConfigurationProperties` and `@ConditionalOnProperty` work identically in 2.x and 3.x — no friction, no migration pressure from this phase.

## Step 0 — Branch + sanity

```bash
cd /path/to/Look-see
git checkout -b phase-3/looksee-shim
```

Confirm the starting build is green on the branch point (so any later regression is yours, not baseline):

```bash
cd LookseeCore
mvn -q -pl looksee-core -am verify
```

Expect: `BUILD SUCCESS`. If not, stop — fix main first or rebase.

## Step 1 — Create `looksee-browsing-client` module

### 1.1 Directory scaffold

```
LookseeCore/looksee-browsing-client/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/looksee/browsing/client/      (hand-written facade)
    │   │   ├── BrowsingClient.java
    │   │   └── BrowsingClientConfig.java
    │   └── resources/
    │       └── openapi.yaml                        (copied in Step 2)
    └── test/
        └── java/com/looksee/browsing/client/
            └── BrowsingClientTest.java
```

### 1.2 Module `pom.xml`

Create `LookseeCore/looksee-browsing-client/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.looksee</groupId>
        <artifactId>A11yParent</artifactId>
        <version>0.6.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>looksee-browsing-client</artifactId>
    <name>LookseeCore Browsing Client</name>
    <description>HTTP client for brandonkindred/browser-service. Generated DTOs + hand-written facade.</description>

    <properties>
        <openapi-generator.version>7.10.0</openapi-generator.version>
        <jackson.version>2.15.4</jackson.version>
        <build-helper.version>3.6.0</build-helper.version>
    </properties>

    <dependencies>
        <!-- DTO serialization -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <!-- Reused Look-see enums (BrowserType, BrowserEnvironment, ...) -->
        <dependency>
            <groupId>com.looksee</groupId>
            <artifactId>looksee-browser</artifactId>
            <version>${project.version}</version>
            <exclusions>
                <!-- Client must not drag Selenium onto consumers in remote-only deployments -->
                <exclusion>
                    <groupId>org.seleniumhq.selenium</groupId>
                    <artifactId>*</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.openapitools</groupId>
                <artifactId>openapi-generator-maven-plugin</artifactId>
                <version>${openapi-generator.version}</version>
                <executions>
                    <execution>
                        <id>generate-browsing-client</id>
                        <phase>generate-sources</phase>
                        <goals><goal>generate</goal></goals>
                        <configuration>
                            <inputSpec>${project.basedir}/src/main/resources/openapi.yaml</inputSpec>
                            <generatorName>java</generatorName>
                            <library>native</library>
                            <apiPackage>com.looksee.browsing.generated.api</apiPackage>
                            <modelPackage>com.looksee.browsing.generated.model</modelPackage>
                            <invokerPackage>com.looksee.browsing.generated</invokerPackage>
                            <generateApiTests>false</generateApiTests>
                            <generateModelTests>false</generateModelTests>
                            <generateApiDocumentation>false</generateApiDocumentation>
                            <generateModelDocumentation>false</generateModelDocumentation>
                            <configOptions>
                                <sourceFolder>src/gen/java/main</sourceFolder>
                                <dateLibrary>java8</dateLibrary>
                                <useJakartaEe>false</useJakartaEe>
                                <hideGenerationTimestamp>true</hideGenerationTimestamp>
                                <openApiNullable>false</openApiNullable>
                            </configOptions>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <version>${build-helper.version}</version>
                <executions>
                    <execution>
                        <id>add-generated-sources</id>
                        <phase>generate-sources</phase>
                        <goals><goal>add-source</goal></goals>
                        <configuration>
                            <sources>
                                <source>${project.build.directory}/generated-sources/openapi/src/gen/java/main</source>
                            </sources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

Notes on choices:

- **`<library>native</library>`** — uses the JDK 11+ `HttpClient`. Zero dependency on Spring / OkHttp / Jersey, which matters because some consumers (notably `element-enrichment`) run on trimmed Spring profiles and we don't want the client shim to force transitive upgrades.
- **Jackson `2.15.4`** — pinned to match the version already dragged in by the Spring Boot 2.6.x BOM used across looksee-core; overriding avoids a classpath-split between generated DTOs and existing code.
- **Excluding Selenium from `looksee-browser`** — we only want the enums (`BrowserType`, `BrowserEnvironment`, `Action`, `AlertChoice`); the Selenium transitive deps stay out of remote-only consumer pods. `RemoteBrowser` lives in `looksee-core` (Step 5) and pulls Selenium in through the regular `looksee-browser` dep there.

### 1.3 Register module in `LookseeCore/pom.xml`

Edit `LookseeCore/pom.xml` — bump the parent version (Step 9 does this formally; stage the version bump here or in Step 9 depending on taste — the doc recommends Step 9 to keep this commit tight) and add the module entry:

```diff
 <modules>
     <module>looksee-core</module>
     <module>looksee-browser</module>
+    <module>looksee-browsing-client</module>
 </modules>
```

### 1.4 Commit

```bash
git add LookseeCore/pom.xml LookseeCore/looksee-browsing-client/pom.xml
git commit -m "chore(core): add looksee-browsing-client module scaffold"
```

## Step 2 — Import the OpenAPI spec

Single source of truth today: `brandonkindred/browser-service:openapi.yaml` (the spec produced in phase 0, driving the engine work in phase 1a). Copy it wholesale:

```bash
cp /path/to/browser-service/openapi.yaml \
   LookseeCore/looksee-browsing-client/src/main/resources/openapi.yaml
```

Generate + compile:

```bash
cd LookseeCore
mvn -pl looksee-browsing-client -am -DskipTests compile
```

Expect: sources generated under `looksee-browsing-client/target/generated-sources/openapi/src/gen/java/main/com/looksee/browsing/generated/` (api + model packages) and a clean compile. Spot-check by listing the expected top-level APIs:

```bash
find LookseeCore/looksee-browsing-client/target/generated-sources -name "*Api.java"
# Expect at minimum: SessionsApi.java, NavigationApi.java, ScreenshotsApi.java,
# CaptureApi.java, HealthApi.java (names mirror the tags in openapi.yaml)
```

Commit the spec (generated sources stay out of git — they're in `target/`):

```bash
git add LookseeCore/looksee-browsing-client/src/main/resources/openapi.yaml
git commit -m "feat(browsing-client): import OpenAPI spec and generate client"
```

> **Drift control.** The spec is copied, not referenced. Until browser-service publishes itself as a Maven artifact (deferred), resync by re-copying whenever the service cuts a contract-affecting release. See [§14](#14-open-items-flagged-for-reviewer).

## Step 3 — Write the `BrowsingClient` facade

Hand-written facade at `LookseeCore/looksee-browsing-client/src/main/java/com/looksee/browsing/client/BrowsingClient.java`. This is the **only** class `looksee-core` touches from the generated package — everything else stays internal to the module, so DTO shape changes are absorbed here, not across the 10+ consumer services.

### 3.1 Config object

`BrowsingClientConfig.java`:

```java
package com.looksee.browsing.client;

import java.time.Duration;

public final class BrowsingClientConfig {
    private final String serviceUrl;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public BrowsingClientConfig(String serviceUrl, Duration connectTimeout, Duration readTimeout) {
        if (serviceUrl == null || serviceUrl.isBlank()) {
            throw new IllegalArgumentException("serviceUrl must be set when remote browsing is enabled");
        }
        this.serviceUrl = serviceUrl.endsWith("/") ? serviceUrl.substring(0, serviceUrl.length() - 1) : serviceUrl;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public String getServiceUrl() { return serviceUrl; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
}
```

### 3.2 Facade surface

`BrowsingClient.java` — public methods (signatures; bodies wrap the corresponding generated `*Api` classes and translate DTO enums into Look-see's existing `com.looksee.browser.enums.*`):

```java
public final class BrowsingClient {

    public BrowsingClient(BrowsingClientConfig config) { ... }

    // Session lifecycle
    public Session createSession(BrowserType type, BrowserEnvironment env);
    public SessionState getSession(String id);
    public void deleteSession(String id);

    // Navigation + state
    public void navigate(String id, String url);
    public PageStatus getStatus(String id);
    public ViewportState getViewport(String id);
    public String getSource(String id);

    // Screenshots
    public byte[] screenshot(String id, ScreenshotStrategy strategy);

    // One-shot capture (navigate + screenshot + source, single round-trip)
    public CaptureResponse capture(CaptureRequest req);
}
```

Where:

- `Session`, `SessionState`, `PageStatus`, `ViewportState`, `CaptureRequest`, `CaptureResponse` are **public facade types** in `com.looksee.browsing.client` — small POJOs that shield consumers from the generated model package. Any rename in the OpenAPI spec only ripples to the facade's translation code.
- `ScreenshotStrategy` is an enum in the facade mapping to the generated enum values (viewport / full-page / ashot / shutterbug) and to Look-see's existing naming.
- `BrowserType` / `BrowserEnvironment` are the existing enums from `com.looksee.browser.enums.*` — reuse, do not duplicate.

The constructor builds the shared `ApiClient` (from the generated invoker package) once, configures it with `config.getServiceUrl()` + the two timeouts, and hands it to each `*Api` instance. Retries + backoff: not added in this phase; document as a phase-3b candidate if service deploy surfaces transient failures.

### 3.3 Unit test

`BrowsingClientTest.java` — pure Mockito, no HTTP:

```java
@ExtendWith(MockitoExtension.class)
class BrowsingClientTest {
    @Mock SessionsApi sessionsApi;
    @Mock NavigationApi navigationApi;
    @Mock ScreenshotsApi screenshotsApi;
    @Mock CaptureApi captureApi;

    // Construct BrowsingClient with a test-only constructor that accepts the
    // four API stubs directly (package-private — see TestSupport section below).
    // Assert each facade method forwards arguments correctly and maps DTO enums
    // → Look-see enums on the return path.
}
```

To make this possible, add a **package-private** secondary constructor on `BrowsingClient` taking the four `*Api` instances for tests. The public constructor keeps taking `BrowsingClientConfig`. This is the same pattern used elsewhere in looksee-core for testability (grep for "package-private test constructor" in `BrowserService`).

### 3.4 Commit

```bash
git add LookseeCore/looksee-browsing-client/src/
git commit -m "feat(browsing-client): add BrowsingClient facade"
```

## Step 4 — Config properties in looksee-core

New file `LookseeCore/looksee-core/src/main/java/com/looksee/config/LookseeBrowsingProperties.java`:

```java
package com.looksee.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("looksee.browsing")
public class LookseeBrowsingProperties {
    public enum Mode { LOCAL, REMOTE }

    private Mode mode = Mode.LOCAL;
    private String serviceUrl;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(120);

    // standard getters + setters
}
```

New file `LookseeCore/looksee-core/src/main/java/com/looksee/config/BrowsingClientConfiguration.java`:

```java
package com.looksee.config;

import com.looksee.browsing.client.BrowsingClient;
import com.looksee.browsing.client.BrowsingClientConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LookseeBrowsingProperties.class)
public class BrowsingClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "looksee.browsing.mode", havingValue = "remote")
    public BrowsingClient browsingClient(LookseeBrowsingProperties props) {
        return new BrowsingClient(new BrowsingClientConfig(
            props.getServiceUrl(),
            props.getConnectTimeout(),
            props.getReadTimeout()));
    }
}
```

In local mode (the default), `BrowsingClient` is not instantiated at all — consumers carry zero runtime overhead until they opt in.

Add the `looksee-browsing-client` dep to `LookseeCore/looksee-core/pom.xml`:

```xml
<dependency>
    <groupId>com.looksee</groupId>
    <artifactId>looksee-browsing-client</artifactId>
    <version>${project.version}</version>
</dependency>
```

Commit:

```bash
git add LookseeCore/looksee-core/pom.xml \
        LookseeCore/looksee-core/src/main/java/com/looksee/config/LookseeBrowsingProperties.java \
        LookseeCore/looksee-core/src/main/java/com/looksee/config/BrowsingClientConfiguration.java
git commit -m "feat(core): add LookseeBrowsingProperties + remote-mode bean wiring"
```

## Step 5 — `RemoteBrowser extends Browser`

New file `LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/RemoteBrowser.java`. Lives in **looksee-core** (not looksee-browser) so the engine module stays pure-local — in remote-only deployments, looksee-browser's Selenium transitive deps still load, but no local driver is ever constructed.

### 5.1 Constructor + state

```java
public class RemoteBrowser extends Browser {
    private final BrowsingClient client;
    private final String sessionId;
    private final String browserName;

    public RemoteBrowser(BrowsingClient client, String sessionId, String browserName) {
        super(null); // the driver-taking super constructor; we override every method that reads it
        this.client = client;
        this.sessionId = sessionId;
        this.browserName = browserName;
    }
}
```

`Browser.java` currently exposes a driver-taking constructor; if no null-accepting variant exists, add a protected no-arg constructor in `Browser` that leaves the driver field null **only** if the review finds the null-passing super call problematic. Prefer overriding every getter-of-driver downstream in `RemoteBrowser` over mutating `Browser`. Revisit in review.

### 5.2 Overrides — forward via `BrowsingClient`

Override surface derives directly from the public methods on `LookseeCore/looksee-browser/src/main/java/com/looksee/browser/Browser.java`. Each override calls the equivalent `BrowsingClient` method with `sessionId`:

| Browser method | RemoteBrowser impl |
|---|---|
| `navigateTo(String url)` | `client.navigate(sessionId, url)` |
| `close()` | `client.deleteSession(sessionId)` |
| `getSource()` | `return client.getSource(sessionId);` |
| `is503Error()` | derive from `client.getStatus(sessionId)` |
| `getViewportScreenshot()` | `client.screenshot(sessionId, VIEWPORT)` |
| `getFullPageScreenshot()` | `client.screenshot(sessionId, FULL_PAGE)` |
| `getFullPageScreenshotAshot()` | `client.screenshot(sessionId, FULL_PAGE_ASHOT)` |
| `getFullPageScreenshotShutterbug()` | `client.screenshot(sessionId, FULL_PAGE_SHUTTERBUG)` |
| `getViewportScrollOffset()` | `client.getViewport(sessionId).getScrollOffset()` |
| `waitForPageToLoad()` | derive from `client.getStatus(sessionId)` (poll until READY or timeout) |

### 5.3 Lombok getter overrides

`Browser.java` is Lombok-annotated; some generated getters would read the null driver. Override each to return session-state-derived values:

- `getBrowserName()` → return stored `browserName`
- `getViewportSize()` → from `client.getViewport(sessionId)`
- `getXScrollOffset()` / `getYScrollOffset()` → from `client.getViewport(sessionId)`

### 5.4 Unsupported — phase-3b TODOs

Every element-handle op throws `UnsupportedOperationException("phase 3b: remote element-handle ops")`:

- `findElement(...)`, `findWebElementByXpath(...)`
- `isDisplayed(WebElement)`, `extractAttributes(WebElement)`
- `getElementScreenshot(WebElement)`
- `scrollToElement(WebElement)`, `scrollToElementCentered(WebElement)`
- `removeElement(WebElement)`
- `removeDriftChat()`, `removeGDPR()`, `removeGDPRmodals()`
- `moveMouseOutOfFrame()`, `moveMouseToNonInteractive()`
- `isAlertPresent()`
- `getDriver()`

Keep this list exhaustive — the phase-3b plan inherits it as a ready-made task list.

Commit:

```bash
git add LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/RemoteBrowser.java
git commit -m "feat(core): add RemoteBrowser forwarding page-level ops via BrowsingClient"
```

## Step 6 — Fork `BrowserService.getConnection()` + `TestService.getConnection()`

Two call sites in looksee-core use `BrowserConnectionHelper.getConnection`:

- `LookseeCore/looksee-core/src/main/java/com/looksee/services/BrowserService.java` — `getConnection` around **line 129** (confirm on the branch before editing).
- `LookseeCore/looksee-core/src/main/java/com/looksee/services/TestService.java` — the only other call site.

### 6.1 `BrowserService`

Inject the properties and an optional client:

```java
@Service
public class BrowserService {
    private final LookseeBrowsingProperties browsingProps;
    private final BrowsingClient browsingClient;   // null when mode=local

    @Autowired
    public BrowserService(LookseeBrowsingProperties browsingProps,
                          @Autowired(required = false) BrowsingClient browsingClient) {
        this.browsingProps = browsingProps;
        this.browsingClient = browsingClient;
    }
    ...
}
```

Fork `getConnection` — keep local branch byte-identical:

```java
public Browser getConnection(BrowserType type, BrowserEnvironment env) {
    if (browsingProps.getMode() == LookseeBrowsingProperties.Mode.LOCAL) {
        // unchanged local path
        return BrowserConnectionHelper.getConnection(type, env, ...);
    }
    // remote
    Session s = browsingClient.createSession(type, env);
    return new RemoteBrowser(browsingClient, s.getId(), type.name());
}
```

### 6.2 `TestService`

Apply the **exact same fork** at the corresponding call site. No behavior change in local mode; identical remote-mode construction.

### 6.3 Commit

```bash
git add LookseeCore/looksee-core/src/main/java/com/looksee/services/BrowserService.java \
        LookseeCore/looksee-core/src/main/java/com/looksee/services/TestService.java
git commit -m "feat(core): fork BrowserService/TestService getConnection on mode flag"
```

## Step 7 — Add `BrowserService.capturePage(...)` high-level method

Today every audit flow walks: open browser → navigate → call `PageStateAdapter.toPageState(Browser, long, String)` (at `PageStateAdapter.java:162`) → close. In remote mode that's four HTTP round-trips when one would do — the service exposes `POST /v1/capture` for exactly this. Add a one-shot method consumers can opt into:

```java
public PageState capturePage(URL url, BrowserType type, long auditRecordId)
        throws WebDriverException, IOException {
    if (browsingProps.getMode() == LookseeBrowsingProperties.Mode.LOCAL) {
        Browser browser = getConnection(type, BrowserEnvironment.DISCOVERY);
        try {
            browser.navigateTo(url.toString());
            return PageStateAdapter.toPageState(browser, auditRecordId, url.toString());
        } finally {
            browser.close();
        }
    }
    // remote: single round-trip
    CaptureResponse resp = browsingClient.capture(new CaptureRequest(url, type));
    return PageStateAdapter.toPageState(
        resp.getScreenshotBytes(), resp.getSource(), auditRecordId, url.toString());
}
```

Add the sibling `PageStateAdapter` overload in the same commit:

```java
public static PageState toPageState(byte[] screenshot, String source,
                                    long auditRecordId, String url) {
    // Build a PageState from bytes + source without a live Browser. Pull the
    // existing logic out of the Browser-taking overload at line 162 into a
    // private helper both overloads call.
}
```

Local-mode consumers get transparent behavior continuity; remote-mode consumers get a one-shot capture.

Commit:

```bash
git add LookseeCore/looksee-core/src/main/java/com/looksee/services/BrowserService.java \
        LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/PageStateAdapter.java
git commit -m "feat(core): add BrowserService.capturePage as one-shot page capture"
```

## Step 8 — Tests

### 8.1 `looksee-browsing-client`

Already covered in Step 3 (`BrowsingClientTest`). No HTTP integration test needed for MVP — the generated `*Api` classes are Mockito-mocked.

### 8.2 `looksee-core`

Three new tests, two invariants:

1. **`BrowserServiceRemoteModeTest`** — `@SpringBootTest` with `@TestPropertySource(properties = { "looksee.browsing.mode=remote", "looksee.browsing.service-url=http://mock" })` and a `@MockBean BrowsingClient`. Assert:
   - `getConnection(...)` returns a `RemoteBrowser` and calls `browsingClient.createSession(...)` once.
   - `capturePage(...)` goes through `browsingClient.capture(...)` exactly once (no per-step calls).

2. **`BrowserServiceLocalModeTest`** — default props (no flag set). Assert that `BrowsingClient` is **not** autowired (bean absent) and `getConnection(...)` goes through the existing local path. This guards the default-unchanged promise.

3. **`RemoteBrowserTest`** — unit test with a mocked `BrowsingClient`:
   - Each page-level override forwards to the right client method with `sessionId`.
   - Every unsupported method throws `UnsupportedOperationException`.
   - Lombok getter overrides return session-state-derived values, not null.

**Existing tests:** `BrowserServiceTest`, `PageStateAdapterTest`, `StepExecutorTest`, and the audit-flow integration tests in looksee-core **must continue to pass without modification**. The byte-identical local branch is the contract.

Commit:

```bash
git add LookseeCore/looksee-core/src/test/
git commit -m "test(core): cover remote-mode forks and BrowsingClient"
```

## Step 9 — Version bump + CHANGELOG

### 9.1 Bump

- `LookseeCore/pom.xml` — `<version>0.5.0</version>` → `<version>0.6.0</version>`.
- Each child module `pom.xml` that declares `<parent>...<version>0.5.0</version></parent>` — bump to `0.6.0`.
- `Look-see/LOOKSEE_CORE_VERSION` (if present at repo root) — update.

Sanity check there are no stragglers:

```bash
grep -rn "0.5.0" LookseeCore/ | grep -v target | grep -v ".md"
# Should return only legitimate historical references (CHANGELOG, etc.), never a live pom version.
```

### 9.2 CHANGELOG

Add under `## [0.6.0]`:

```markdown
## [0.6.0] — YYYY-MM-DD

### Added
- New `looksee-browsing-client` module: generated DTOs + `BrowsingClient` facade for `brandonkindred/browser-service`.
- `looksee.browsing.mode` config flag (`local` | `remote`) with `service-url`, `connect-timeout`, `read-timeout` properties.
- `RemoteBrowser` — forwards page-level browser ops (navigate, screenshot, source, close) to browser-service over HTTP.
- `BrowserService.capturePage(URL, BrowserType, long)` — one-shot page capture that uses a single `/v1/capture` round-trip in remote mode.

### Changed
- `BrowserService.getConnection()` and `TestService.getConnection()` now fork on `looksee.browsing.mode`. Default (`local`) behavior is byte-identical — existing consumers upgrade with no code change.

### Deferred (phase 3b)
- Element-handle ops in `RemoteBrowser` (findElement, extractAttributes, journey steps) throw `UnsupportedOperationException`. Consumers that exercise those paths must stay on `local` mode until 3b ships.

### Migration
- Default mode is `local`. Existing consumers pick up 0.6.0 with no behavior change.
- To opt a consumer into remote mode: set `looksee.browsing.mode=remote` + `looksee.browsing.service-url=<browser-service-url>` in its `application.yml`.
```

Commit:

```bash
git add LookseeCore/pom.xml LookseeCore/**/pom.xml LookseeCore/CHANGELOG.md Look-see/LOOKSEE_CORE_VERSION
git commit -m "chore: bump LookseeCore to 0.6.0"
```

## Step 10 — Verification

1. **Clean build from LookseeCore root:**
   ```bash
   cd LookseeCore
   mvn -q clean verify
   ```
   Expect: `BUILD SUCCESS` across all three modules. All pre-existing tests green (local-mode regression). New tests green.

2. **Local-mode sanity:** spin up any consumer (e.g. PageBuilder) against the fresh 0.6.0 with no `looksee.browsing.*` config. Run its existing happy-path integration. Behavior must be indistinguishable from 0.5.0.

3. **Remote-mode sanity (optional, needs a running browser-service):**
   ```bash
   # In browser-service repo
   docker compose up -d

   # Tiny standalone Spring Boot consumer (can be a scratch main()):
   # application.yml:
   #   looksee:
   #     browsing:
   #       mode: remote
   #       service-url: http://localhost:8080/v1
   # Code:
   #   browserService.capturePage(new URL("https://example.com"), BrowserType.CHROME, 1L);
   ```
   Expect: non-empty `PageState` returned, screenshot field populated with real bytes of example.com, no exceptions.

4. **No unintended edits:**
   ```bash
   git diff --stat main..HEAD -- LookseeCore/looksee-browser/
   # Expect: empty output. looksee-browser must be untouched in this phase.
   ```

## Definition of done

- [ ] `looksee-browsing-client` module compiles; generator produces `SessionsApi`, `NavigationApi`, `ScreenshotsApi`, `CaptureApi` (at minimum).
- [ ] `BrowsingClient` facade covers the nine public methods in Step 3.2 with Mockito unit tests.
- [ ] `LookseeBrowsingProperties` registered; `BrowsingClient` bean created **only** when `looksee.browsing.mode=remote`.
- [ ] `RemoteBrowser` overrides every page-level method in the Step 5.2 table; every phase-3b method throws `UnsupportedOperationException`.
- [ ] `BrowserService.getConnection()` **and** `TestService.getConnection()` forked on the mode flag; local branch byte-identical to 0.5.0.
- [ ] `BrowserService.capturePage(...)` exists in both modes; remote mode uses a single round-trip via `/v1/capture`.
- [ ] All pre-existing looksee-core tests pass **without modification**.
- [ ] `A11yParent` at 0.6.0 across every pom; CHANGELOG has a `0.6.0` entry.
- [ ] `git diff main..HEAD -- LookseeCore/looksee-browser/` is empty.
- [ ] PR opened against `main` with title **"Phase 3: LookseeCore browsing-client shim (default local, opt-in remote)"**.

## Push and open PR

```bash
git push -u origin phase-3/looksee-shim
```

PR body should include:

- Summary of the eight commit categories (module scaffold, spec import, facade, config, `RemoteBrowser`, `getConnection` forks, `capturePage`, tests, version bump).
- `mvn verify` output snippet showing `BUILD SUCCESS` and test counts for each module.
- Explicit note: **default mode is `local`; existing consumers upgrade to 0.6.0 with no behavior change.**
- Link back to this document for context.
- Link to `phase-1a-engine-upgrade.md` for the engine side of the contract.

## 14. Open items flagged for reviewer

1. **OpenAPI spec drift.** The spec lives in two places now: authoritative copy in `brandonkindred/browser-service:openapi.yaml`, build input copy in `LookseeCore/looksee-browsing-client/src/main/resources/openapi.yaml`. Manual resync for now. **Recommendation:** promote to a Maven artifact published from browser-service once that repo's release pipeline stabilizes; the client module then `<dependency>`'s the artifact and the resources copy goes away.

2. **`looksee-browser` staying untouched.** The shim intentionally lives in `looksee-core`; `looksee-browser` remains the pure-local engine. Long-term, once every consumer is on remote and browser-service is battle-tested, `looksee-browser` can be deleted from LookseeCore entirely (phase 5). Short-term, it continues to back local mode and deleting it would break every downstream service overnight.

3. **Element-handle remote ops (phase 3b).** Every `UnsupportedOperationException` in `RemoteBrowser` (Step 5.4) is a phase-3b TODO. The list there is exhaustive — the 3b plan should inherit it verbatim as its in-scope task list. Consumers that exercise those paths (notably `journeyExecutor`, `element-enrichment`'s deep-enrichment pass, and StepExecutor-based audits) stay on `local` mode until 3b ships.

4. **`StepExecutor` stays local.** `LookseeCore/looksee-core/src/main/java/com/looksee/services/StepExecutor.java` is the biggest consumer of element-handle ops in-core. Not forked in this phase — any journey-executing consumer must stay on `local` for now. Call out explicitly in the 3b plan.

5. **`BrowserUtils` is all-static.** `LookseeCore/looksee-core/src/main/java/com/looksee/utils/BrowserUtils.java` uses helpers that operate on `Browser` instances but take no ambient driver state; they're fine against `RemoteBrowser` as long as the methods they call are ones `RemoteBrowser` overrides. Audit during Step 6 review that no static helper reaches through a `RemoteBrowser` into a phase-3b-deferred method.

6. **Spring Boot version.** LookseeCore is on Spring Boot 2.6.x. `@ConditionalOnProperty` + `@ConfigurationProperties` work identically in 2.x and 3.x. No migration pressure from this phase; when LookseeCore eventually moves to Spring Boot 3, the shim rides along unchanged.

7. **`Browser` super-constructor with null driver.** Step 5.1 passes `null` to `super(driver)`. If `Browser.java`'s constructor does any validation or non-trivial work on `driver`, this breaks. The recommended approach is to override every downstream getter in `RemoteBrowser` rather than mutate `Browser`. If a cleaner tweak to `Browser` is warranted, scope it minimally and flag in PR review — do **not** let it snowball into an engine refactor.

8. **Retries / backoff.** Not added. If phase-4 cutover surfaces transient HTTP failures, add to the facade in one place (`BrowsingClient`) rather than per-caller.
