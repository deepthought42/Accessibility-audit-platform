# Phase 1a — Engine Upgrade

> **Goal:** `brandonkindred/browser-service` builds and all tests pass as a **standalone Maven project** on **Java 21** with **Selenium 4** and **Appium Java Client 9**. No new features. No REST API layer yet — that's phase 1b.
>
> **Where to work:** locally in your `browser-service` clone. Suggest a feature branch: `git checkout -b phase-1a/engine-upgrade`.
>
> **Repo layout note:** this doc was originally written assuming the engine lived under `engine/` in the new repo. After phase 0 the `engine/` directory was flattened — everything lives at the repo root now. All path references below (e.g. `src/main/java/...`, `pom.xml`, `target/site/jacoco/`) are relative to the repo root. Ignore any stray `engine/` prefix that may still appear in quoted shell snippets.
>
> **Reference (read-only):** the old engine in Look-see's `LookseeCore/looksee-browser/` still exists on `claude/extract-browsing-service-MgOw3` if you need to diff anything.

## Why this phase exists first

When the subtree split moved `looksee-browser/` into the new repo, the module came without its parent. Its current `pom.xml` still references `<parent>com.looksee:A11yParent</parent>`, which doesn't resolve. The engine can't compile right now. Fixing that also gives us a clean moment to take the Selenium 3 → 4 and Appium 7 → 9 hit without mixing it with net-new API code.

## Scope

| In scope | Out of scope |
|---|---|
| Replace `pom.xml` with a standalone POM | New REST controllers / DTOs / session management |
| Upgrade Selenium 3.141.59 → 4.x | Adding new endpoints, DTOs, controllers |
| Upgrade Appium Java Client 7.6.0 → 9.x | Dockerfile, GitHub Actions |
| Upgrade screenshot libs (Shutterbug, AShot) | Refactors beyond what the version bumps force |
| Java 17 → 21 | Style changes, comment rewrites, method renames |
| Make all existing tests pass | New tests |

If something looks refactor-tempting, resist. The goal is a green test suite on the new versions.

## Prerequisites

```powershell
java -version     # must be 21.x
mvn -v            # 3.9+ recommended
git status        # clean working tree on main, up to date with origin/main
```

If you don't have Java 21 installed, grab Temurin 21 from <https://adoptium.net>.

## Step 0 — Branch + sanity

```powershell
cd C:\...\browser-service
git checkout -b phase-1a/engine-upgrade
```

Confirm the engine is currently broken the expected way:

```powershell
mvn -q compile
# Expected: failure resolving parent com.looksee:A11yParent:0.5.0
```

## Step 1 — Replace `pom.xml`

Replace the file wholesale with the version below. Summary of changes:

- Remove `<parent>` block.
- Add explicit `<groupId>`, `<artifactId>`, `<version>`, `<packaging>`.
- Add Java 21 compiler settings.
- Pin versions that were previously inherited from parent (Jackson, Commons Codec, Lombok, JUnit, Mockito, plugins).
- Bump Selenium, Appium, Shutterbug.
- Drop `selenium-server` dependency entirely — unneeded for client code in Selenium 4.
- Wire up compiler, surefire, jacoco, source plugins directly (since there's no parent `pluginManagement` now).

**New `pom.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.browserservice</groupId>
    <artifactId>browser-service-engine</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Browser Service Engine</name>
    <description>Selenium/Appium-based browser automation engine powering the Browser Service.</description>
    <url>https://github.com/brandonkindred/browser-service</url>

    <licenses>
        <license>
            <name>MIT License</name>
            <url>https://opensource.org/licenses/MIT</url>
        </license>
    </licenses>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Dependencies -->
        <selenium.version>4.27.0</selenium.version>
        <appium.version>9.3.0</appium.version>
        <shutterbug.version>1.6</shutterbug.version>
        <ashot.version>1.5.4</ashot.version>
        <jackson.version>2.18.2</jackson.version>
        <commons-codec.version>1.17.1</commons-codec.version>
        <commons-text.version>1.13.1</commons-text.version>
        <jstyleparser.version>3.5</jstyleparser.version>
        <cssparser.version>0.9.30</cssparser.version>
        <xsoup.version>0.3.1</xsoup.version>
        <jtidy.version>r938</jtidy.version>
        <xml-apis.version>1.4.01</xml-apis.version>
        <lombok.version>1.18.34</lombok.version>

        <!-- Test -->
        <junit.version>5.11.3</junit.version>
        <mockito.version>5.14.2</mockito.version>

        <!-- Plugins -->
        <maven-compiler-plugin.version>3.13.0</maven-compiler-plugin.version>
        <maven-surefire-plugin.version>3.5.2</maven-surefire-plugin.version>
        <maven-source-plugin.version>3.3.1</maven-source-plugin.version>
        <jacoco.version>0.8.12</jacoco.version>
    </properties>

    <dependencies>
        <!-- Selenium 4 -->
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-java</artifactId>
            <version>${selenium.version}</version>
        </dependency>

        <!-- Screenshot strategies -->
        <dependency>
            <groupId>com.assertthat</groupId>
            <artifactId>selenium-shutterbug</artifactId>
            <version>${shutterbug.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>org.seleniumhq.selenium</groupId>
                    <artifactId>selenium-java</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>ru.yandex.qatools.ashot</groupId>
            <artifactId>ashot</artifactId>
            <version>${ashot.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>org.seleniumhq.selenium</groupId>
                    <artifactId>selenium-remote-driver</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!-- Appium 9 (Selenium 4 compatible) -->
        <dependency>
            <groupId>io.appium</groupId>
            <artifactId>java-client</artifactId>
            <version>${appium.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>org.seleniumhq.selenium</groupId>
                    <artifactId>selenium-java</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!-- CSS / HTML parsing -->
        <dependency>
            <groupId>net.sf.cssbox</groupId>
            <artifactId>jstyleparser</artifactId>
            <version>${jstyleparser.version}</version>
        </dependency>
        <dependency>
            <groupId>net.sourceforge.cssparser</groupId>
            <artifactId>cssparser</artifactId>
            <version>${cssparser.version}</version>
        </dependency>
        <dependency>
            <groupId>us.codecraft</groupId>
            <artifactId>xsoup</artifactId>
            <version>${xsoup.version}</version>
        </dependency>
        <dependency>
            <groupId>xml-apis</groupId>
            <artifactId>xml-apis</artifactId>
            <version>${xml-apis.version}</version>
        </dependency>
        <dependency>
            <groupId>net.sf.jtidy</groupId>
            <artifactId>jtidy</artifactId>
            <version>${jtidy.version}</version>
        </dependency>

        <!-- Apache Commons -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-text</artifactId>
            <version>${commons-text.version}</version>
        </dependency>
        <dependency>
            <groupId>commons-codec</groupId>
            <artifactId>commons-codec</artifactId>
            <version>${commons-codec.version}</version>
        </dependency>

        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <!-- Lombok (compile-only, annotation processor on plugin) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler-plugin.version}</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>UTF-8</encoding>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>${maven-surefire-plugin.version}</version>
                <configuration>
                    <includes>
                        <include>**/*Test.java</include>
                        <include>**/*Tests.java</include>
                    </includes>
                    <excludes>
                        <exclude>**/Abstract*.java</exclude>
                    </excludes>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>${jacoco.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>${maven-source-plugin.version}</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar-no-fork</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

Commit this alone so the next commit is scoped to code changes:

```powershell
git add pom.xml
git commit -m "chore: convert pom to standalone project, bump to Java 21 / Selenium 4 / Appium 9"
```

Running `mvn -q -U compile` now should fail with **source-code** errors (the good kind — Selenium 4 API changes), not dependency-resolution errors. That's expected; fix them in step 2.

> Use `-U` the first time to force Maven to refresh its cache; dependency-resolution failures are cached and won't be retried without it.

## Step 2 — Code changes for Selenium 4

Selenium 4 is largely source-compatible but has three known breaks that affect the engine:

### 2.1 `WebDriverWait` — `long` constructor is gone

Selenium 4 removed the `(WebDriver, long)` constructor in favor of `(WebDriver, Duration)`.

**File:** `src/main/java/com/looksee/browser/Browser.java`

```diff
+import java.time.Duration;
 ...
 public void waitForPageToLoad() {
-    new WebDriverWait(driver, 30L).until(
+    new WebDriverWait(driver, Duration.ofSeconds(30)).until(
         webDriver -> ((JavascriptExecutor) webDriver)
                 .executeScript("return document.readyState")
                 .equals("complete"));
 }
```

Also check `src/main/java/com/looksee/browser/MobileDevice.java` for the same pattern — the `waitForPageToLoad()` method needs identical treatment.

### 2.2 BrowserStack — legacy capability keys no longer accepted

`BrowserFactory.createBrowserStackDriver()` passes flat `browserstack.*` capability keys. Selenium 4 enforces W3C, and BrowserStack expects all vendor prefs nested under `bstack:options`.

**File:** `src/main/java/com/looksee/browser/BrowserFactory.java`

Replace the body of `createBrowserStackDriver()` (lines ~151–198 in the current file) with:

```java
private static WebDriver createBrowserStackDriver(String browserType, URL hubUrl,
        BrowserStackProperties properties) {
    MutableCapabilities caps = new MutableCapabilities();

    // W3C-compliant vendor options
    Map<String, Object> bstackOptions = new HashMap<>();
    bstackOptions.put("userName", properties.getUsername());
    bstackOptions.put("accessKey", properties.getAccessKey());
    bstackOptions.put("debug", properties.isDebug());
    bstackOptions.put("local", properties.isLocal());

    if (properties.getOs() != null) {
        bstackOptions.put("os", properties.getOs());
    }
    if (properties.getOsVersion() != null) {
        bstackOptions.put("osVersion", properties.getOsVersion());
    }
    if (properties.getProject() != null) {
        bstackOptions.put("projectName", properties.getProject());
    }
    if (properties.getBuild() != null) {
        bstackOptions.put("buildName", properties.getBuild());
    }
    if (properties.getName() != null) {
        bstackOptions.put("sessionName", properties.getName());
    }

    caps.setCapability("bstack:options", bstackOptions);

    // Standard W3C browser capability
    caps.setCapability("browserName",
            properties.getBrowser() != null ? properties.getBrowser() : browserType);
    if (properties.getBrowserVersion() != null) {
        caps.setCapability("browserVersion", properties.getBrowserVersion());
    }

    // Browser-specific options (Chrome only today)
    if ("chrome".equalsIgnoreCase(browserType)) {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("user-agent=LookseeBot");
        chromeOptions.addArguments("window-size=1920,1080");
        chromeOptions.addArguments("--remote-allow-origins=*");
        caps = caps.merge(chromeOptions);
    }

    log.debug("Creating BrowserStack RemoteWebDriver for browser: {}",
            caps.getCapability("browserName"));
    return new RemoteWebDriver(hubUrl, caps);
}
```

Required import changes at the top of `BrowserFactory.java`:

```diff
-import org.openqa.selenium.remote.DesiredCapabilities;
+import java.util.HashMap;
+import java.util.Map;
+import org.openqa.selenium.MutableCapabilities;
```

Drop the `capitalize()` helper at the bottom of the file — it was only used for the old flat capability format.

### 2.3 `RemoteWebDriver` creation — no change required

Double-check: the existing `new RemoteWebDriver(hubUrl, options)` constructor works fine in Selenium 4. No rework needed in `openWithChrome()` / `openWithFirefox()`. Firefox's `ImmutableCapabilities("browserName", "firefox")` still works too.

Consider swapping Firefox to `FirefoxOptions` for symmetry:

```java
FirefoxOptions firefoxOptions = new FirefoxOptions();
RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, firefoxOptions);
driver.manage().window().maximize();
```

Not required, but cheap and matches Chrome's pattern. Skip if it tempts further cleanup.

### 2.4 Commit

```powershell
git add src/main/java/com/looksee/browser/Browser.java src/main/java/com/looksee/browser/MobileDevice.java src/main/java/com/looksee/browser/BrowserFactory.java
git commit -m "refactor(engine): adapt Browser, MobileDevice, BrowserFactory to Selenium 4 APIs"
```

## Step 3 — Appium 9 mobile factory rewrite

Appium Java Client 8+ deprecated the `DesiredCapabilities`-based constructors in favor of typed `*Options` classes (`UiAutomator2Options`, `XCUITestOptions`). Version 9 removed the old constructors outright.

**File:** `src/main/java/com/looksee/browser/MobileFactory.java`

Rewrite `openWithAndroid()`:

```java
public static WebDriver openWithAndroid(URL appiumUrl) {
    UiAutomator2Options options = new UiAutomator2Options()
            .setPlatformName("Android")
            .setAutomationName("UiAutomator2")
            .setBrowserName("Chrome");
    return new AndroidDriver(appiumUrl, options);
}
```

Rewrite `openWithIOS()`:

```java
public static WebDriver openWithIOS(URL appiumUrl) {
    XCUITestOptions options = new XCUITestOptions()
            .setPlatformName("iOS")
            .setAutomationName("XCUITest")
            .setBrowserName("Safari");
    return new IOSDriver(appiumUrl, options);
}
```

Update imports:

```java
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
```

Remove the old `DesiredCapabilities`/`MobileCapabilityType` imports.

**`createBrowserStackMobileDriver()`**: similar pattern to section 2.2 — nest BrowserStack creds under `bstack:options` on a `MutableCapabilities`, then pass to `AndroidDriver`/`IOSDriver`.

```java
private static WebDriver createBrowserStackMobileDriver(String platformType, URL hubUrl,
        BrowserStackProperties properties) {
    Map<String, Object> bstackOptions = new HashMap<>();
    bstackOptions.put("userName", properties.getUsername());
    bstackOptions.put("accessKey", properties.getAccessKey());
    bstackOptions.put("debug", properties.isDebug());
    bstackOptions.put("local", properties.isLocal());
    if (properties.getDeviceName() != null) {
        bstackOptions.put("deviceName", properties.getDeviceName());
    }
    if (properties.getOsVersion() != null) {
        bstackOptions.put("osVersion", properties.getOsVersion());
    }
    bstackOptions.put("realMobile", properties.isRealMobile());

    if ("android".equalsIgnoreCase(platformType)) {
        UiAutomator2Options options = new UiAutomator2Options()
                .setBrowserName("Chrome");
        options.setCapability("bstack:options", bstackOptions);
        return new AndroidDriver(hubUrl, options);
    } else if ("ios".equalsIgnoreCase(platformType)) {
        XCUITestOptions options = new XCUITestOptions()
                .setBrowserName("Safari");
        options.setCapability("bstack:options", bstackOptions);
        return new IOSDriver(hubUrl, options);
    }
    throw new IllegalArgumentException("Unsupported mobile platform: " + platformType);
}
```

Commit:

```powershell
git add src/main/java/com/looksee/browser/MobileFactory.java
git commit -m "refactor(engine): adapt MobileFactory to Appium 9 typed Options"
```

## Step 4 — Run the test suite and fix what breaks

```powershell
cd engine
mvn -q clean test
```

Expected: most of the 37 tests pass. A handful will need small updates. Anticipated categories:

| Symptom | Fix |
|---|---|
| `WebDriverWait` mock expects `(WebDriver, long)` | Update assertion to `Duration.ofSeconds(...)` form |
| `DesiredCapabilities` type-reference in tests | Replace with `MutableCapabilities` |
| `AndroidDriver<?>`/`IOSDriver<?>` generic signatures in tests | Drop the generic parameter |
| `MobileCapabilityType.XYZ` constants missing | Use `UiAutomator2Options` setters |
| Mockito 5 stricter about strict-stubs | Either relax to `Mockito.lenient().when(...)` or remove unused stubs |

**Test files most likely affected:**
- `BrowserTest.java` — `waitForPageToLoad` test
- `BrowserFactoryTest.java` — BrowserStack driver creation test
- `MobileFactoryTest.java` — Android/iOS driver creation tests
- `MobileDeviceTest.java` — `waitForPageToLoad` test

Keep each fix minimal. If a test is so coupled to the old API that it needs rewriting, that's a flag — pause and note it in the PR description; we can cover it in phase 1b rather than force it now.

Commit test fixes in one batch:

```powershell
git add src/test/
git commit -m "test(engine): update mocks for Selenium 4 / Appium 9 / Mockito 5"
```

## Step 5 — Verification

All of these must pass before opening the PR:

1. **Clean build:**
   ```powershell
   mvn -q clean verify
   ```
   Expect: `BUILD SUCCESS`, non-empty `target/browser-service-engine-0.1.0-SNAPSHOT.jar`.

2. **Test count sanity check:** note the number of tests run in surefire output. Should be close to the pre-upgrade 37. If it drops significantly, some tests are being skipped silently — investigate.

3. **Coverage:** open `target/site/jacoco/index.html`. Should still be in the 90%+ range for main package classes. A small drop is OK if a test had to be disabled with a documented reason; a large drop isn't.

4. **No warnings from the compiler about removed/deprecated Selenium APIs:** a few deprecation warnings from Selenium 4 itself are fine; Look for warnings that say "removed in a future version" and address them only if trivial.

## Step 6 — Push and open PR

```powershell
git push -u origin phase-1a/engine-upgrade
```

Then open a PR against `main` with title **"Phase 1a: engine upgrade to Java 21 / Selenium 4 / Appium 9"** and a body that includes:

- Summary of the three commit categories (pom, Selenium code, Appium code, tests).
- `mvn verify` output snippet showing `BUILD SUCCESS` and test counts.
- A note that no new features / APIs / behavior changes were introduced — only version and API-shape updates.
- Link back to this document for context.

## Definition of done

- [ ] `pom.xml` is a standalone Maven project (no parent).
- [ ] Java 21 is the source/target.
- [ ] `mvn -q clean verify` passes from a cold `~/.m2` cache.
- [ ] Test count is at or near the pre-upgrade total.
- [ ] No references to `com.looksee:A11yParent` anywhere.
- [ ] PR merged into `main`.

## What phase 1b will cover

Once 1a is merged, phase 1b plan will:

- Add Spring Boot REST controllers, DTOs, and a session registry to the existing single Maven module (no new sub-modules — the repo is flattened).
- Scaffold Spring Boot 3 service generated from `openapi.yaml`.
- Implement the session registry + TTL sweeper.
- Wire REST controllers to engine methods, per the engine-to-endpoint map in `README.md`.
- Add OpenAPI contract tests.

Phases 1c (Docker/local-dev) and 1d (GitHub Actions CI) follow.

## Things I noticed while planning — flag if any matter to you

1. **Package names.** Everything is still under `com.looksee.browser.*`. It's tied to Look-see's historical namespace but the service is now a standalone product. Renaming to `io.browserservice.*` would be more honest — but a rename ripples through every file and every test. **Recommendation: leave it for now.** Revisit post-1b if it bothers you. The `groupId` in the pom is `io.browserservice` already, so the public Maven coordinates are clean even if the Java packages lag.

2. **`io.browserservice` groupId.** I picked that as a placeholder. If you want a different coordinate (e.g. `com.brandonkindred.browserservice`, or something tied to a domain you own), change the `<groupId>` in step 1 before committing.

3. **AShot.** The library is essentially unmaintained. It still works under Selenium 4 for viewport-pasting, but long-term we should either replace it with Chrome CDP's `Page.captureScreenshot` (full-page native, Chrome only) or drop the `full_page_ashot` screenshot strategy. **Not a 1a concern.** Note in phase 1b backlog.

4. **Selenium Grid version compatibility.** Existing Look-see services talk to a Selenium 3 Grid (via LookseeIaC). Selenium 4 clients can talk to a Selenium 4 Grid only — the wire protocols diverged. Once browser-service is deployed and LookseeCore is migrated to call it over HTTP, Grid upgrade isn't a problem (the service handles the Selenium 4 side). But if any legacy caller still drives Selenium 3 directly, the Grid upgrade needs coordination. **Not a 1a concern** since phase 1a doesn't touch deployment; flagging for phase 2.
