# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.3.24] - 2026-03-27

### Added
- Comprehensive Design by Contract (DbC) enforcement across all packages:
  - **Service layer**: All 37 service classes now enforce preconditions with `assert` statements and document contracts in Javadoc (`precondition:` comments)
  - **Model/entity classes**: All domain models include class-level `invariant:` documentation and constructor preconditions
  - **Audit models**: AuditRecord, Audit, Score, and all audit message/recommendation classes enforce parameter contracts
  - **Journey models**: Journey, Step, DomainMap and all step types include precondition assertions
  - **DTO classes**: All data transfer objects validate constructor parameters
  - **Utility classes**: BrowserUtils, ColorUtils, ContentUtils, and other utilities enforce input contracts
  - **Browsing classes**: Crawler, ActionFactory, and form/table helpers include precondition checks
  - **GCP classes**: All PubSub publishers, CloudVisionUtils, CloudNLPUtils, and GoogleCloudStorage enforce contracts
  - **Rule classes**: Rule, RuleFactory, and all rule implementations validate inputs
  - **Message classes**: All inter-service message types include constructor preconditions
- Fixed constructor parameter assignment bugs in AuditRecord (startTime and aestheticAuditProgress were assigned wrong values)
- New `docs/DESIGN_BY_CONTRACT.md` documenting the project's DbC conventions and patterns

### Changed
- Updated README.md with Design by Contract section and updated version to 0.3.23
- Updated CONTRIBUTING.md with DbC guidelines for contributors
- LookseeObject base class now enforces key non-null precondition in parameterized constructor

## [0.3.22] - 2026-03-25

### Added
- Comprehensive test suite covering all major packages:
  - All 37 enum classes with factory method, toString, round-trip, and edge case tests
  - Model/entity classes (Account, Domain, ColorData, Screenshot, Group, Animation, etc.)
  - Extended model classes (Label, LatLng, SimpleElement, SimplePage, Template, etc.)
  - All 16 exception classes with message and HTTP status annotation verification
  - All DTO classes (AuditUpdateDto, Subscription, PageBuiltMessage, UXIssueReportDto, etc.)
  - Audit model hierarchy (Audit, AuditRecord, PageAuditRecord, DomainAuditRecord, Score, etc.)
  - Audit message and recommendation classes
  - Audit statistics classes
  - Rule classes and RuleFactory with all rule type builds
  - Journey model classes (Journey, SimpleStep, LoginStep, LandingStep, Redirect)
  - Message classes for inter-service communication
  - Browsing package classes (Coordinates, ElementNode, ValueDomain, FormField)
  - Competitive analysis and design system model classes
  - Mapper classes (Body, Base64 deserializers)
  - Configuration property classes (LookseeCoreProperties, PusherProperties, SeleniumProperties)
  - VS Code plugin classes (Tree, TreeNode, SessionTestTracker, TestMapper)
  - Utility classes (AuditUtils, ColorUtils, ColorPaletteUtils, JourneyUtils, ListUtils)
  - Service layer tests with Mockito mocking (AuditService)

### Changed
- Updated README.md with current version (0.3.22), comprehensive test documentation, and project structure
- Updated CONTRIBUTING.md with development guidelines, testing instructions, and code standards
- Updated CHANGELOG.md with project history

## [0.3.21] - Previous

### Added
- Expanded utility and enum test coverage
- Enum factory and timing utility unit tests

## [0.3.0] - Earlier

### Added
- Spring Boot auto-configuration for repositories and services
- Modular Pusher configuration with fallback support
- Conditional Pub/Sub publisher creation
- Selenium WebDriver configuration with properties
- MessageBroadcaster guaranteed availability pattern

### Fixed
- PageService.saveForUser scoped to user-specific records
- PageService.findLatestInsight null-safe returns
- PageService.addPageState guards against missing pages
- Removed debug System.out.println statements
