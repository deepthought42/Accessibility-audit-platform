# VSCode Accessibility Extension - Detailed Architecture & Implementation Specification

## Table of Contents
1. [Product Overview](#product-overview)
2. [User Stories & Acceptance Criteria](#user-stories--acceptance-criteria)
3. [Feature Specifications](#feature-specifications)
4. [Architecture Deep Dive](#architecture-deep-dive)
5. [WCAG 2.2 Rule Engine](#wcag-22-rule-engine)
6. [Framework Parsers](#framework-parsers)
7. [User Interface Components](#user-interface-components)
8. [Configuration & Settings](#configuration--settings)
9. [Performance Requirements](#performance-requirements)
10. [Error Handling & Edge Cases](#error-handling--edge-cases)
11. [Testing Strategy](#testing-strategy)
12. [Implementation Plan](#implementation-plan)
13. [API Reference](#api-reference)
14. [File Structure](#file-structure)

## Product Overview

### Purpose Statement
A VSCode extension that provides real-time WCAG 2.2 accessibility analysis for web developers, offering immediate feedback, intelligent quick fixes, and educational guidance to prevent accessibility violations before they reach production.

### Target Users
- **Primary**: Front-end developers working with React, Angular, Vue.js, or HTML
- **Secondary**: Full-stack developers, QA engineers, accessibility specialists
- **Tertiary**: Team leads implementing accessibility standards

### Value Proposition
- **Immediate Feedback**: Real-time accessibility checking as developers type
- **Educational**: Contextual learning about accessibility best practices
- **Productivity**: Automated fixes reduce manual remediation time by 80%
- **Quality**: Catch 70% of accessibility issues during development vs. testing

### Success Metrics
- **Adoption**: 10,000+ active users within 6 months
- **Engagement**: 15+ diagnostics shown per developer per day
- **Effectiveness**: 60% reduction in accessibility bugs reaching QA
- **Satisfaction**: 4.5+ star rating on VSCode marketplace

## User Stories & Acceptance Criteria

### Epic 1: Real-Time Accessibility Analysis

#### User Story 1.1: Basic Accessibility Checking
**As a** React developer  
**I want** to see accessibility violations highlighted in my code as I type  
**So that** I can fix issues immediately without disrupting my workflow  

**Acceptance Criteria:**
- ✅ Extension activates automatically when opening supported file types (.jsx, .tsx, .html, .vue)
- ✅ Accessibility violations appear as squiggly underlines within 500ms of typing
- ✅ Violations are categorized by severity (Error, Warning, Info)
- ✅ Hover tooltips show violation description and WCAG rule reference
- ✅ Problems panel displays all accessibility issues grouped by file
- ✅ Extension works offline without external API calls
- ✅ Performance impact <100ms per keystroke on files up to 10,000 lines

**Definition of Done:**
- All acceptance criteria met
- Unit tests achieve 90%+ code coverage
- Integration tests verify real-time updates
- Performance benchmarks met
- Documentation updated

#### User Story 1.2: Framework-Specific Analysis
**As a** developer using different frameworks  
**I want** the extension to understand framework-specific syntax (JSX, Angular templates, Vue SFCs)  
**So that** I get accurate analysis regardless of my tech stack  

**Acceptance Criteria:**
- ✅ Correctly parses JSX expressions: `<img src={logoUrl} />`
- ✅ Understands Angular template syntax: `<img [src]="logoUrl">`
- ✅ Processes Vue.js single-file components with `<template>` sections
- ✅ Handles TypeScript in all supported frameworks
- ✅ Distinguishes between HTML attributes and framework props
- ✅ Supports component libraries (Material-UI, Angular Material, etc.)
- ✅ Ignores framework-specific syntax not related to accessibility

#### User Story 1.3: Multi-File Project Analysis
**As a** developer working on large projects  
**I want** accessibility checking across all project files  
**So that** I maintain consistency across my entire codebase  

**Acceptance Criteria:**
- ✅ Analyzes all supported files when workspace is opened
- ✅ Updates analysis when files are added/removed from workspace
- ✅ Provides project-wide accessibility summary in status bar
- ✅ Allows filtering violations by severity, rule type, or file pattern
- ✅ Maintains analysis state when switching between files
- ✅ Processes up to 1000 files without significant performance impact

### Epic 2: Intelligent Quick Fixes

#### User Story 2.1: One-Click Remediation
**As a** developer encountering accessibility violations  
**I want** to fix issues with a single click  
**So that** I can maintain my coding flow without manual research  

**Acceptance Criteria:**
- ✅ Light bulb icon appears next to violations with available fixes
- ✅ Clicking light bulb shows contextual quick fix options
- ✅ Fixes are applied instantly without requiring save/reload
- ✅ Multiple fix options provided when appropriate (e.g., decorative vs. descriptive alt text)
- ✅ Preview shows before/after code changes
- ✅ Undo works correctly after applying fixes
- ✅ Fixes maintain code formatting and style

#### User Story 2.2: Intelligent Suggestions
**As a** developer adding alt text to images  
**I want** meaningful suggestions based on image context  
**So that** I don't have to manually craft descriptions from scratch  

**Acceptance Criteria:**
- ✅ Analyzes image filenames to suggest alt text: "company-logo.png" → "company logo"
- ✅ Considers surrounding context (nearby headings, captions, etc.)
- ✅ Provides different suggestions for decorative vs. content images
- ✅ Offers framework-specific syntax (React: `alt={description}` vs HTML: `alt="description"`)
- ✅ Learns from user choices to improve future suggestions
- ✅ Handles edge cases like data URIs, dynamic src attributes

#### User Story 2.3: Bulk Operations
**As a** developer inheriting legacy code  
**I want** to fix multiple accessibility issues at once  
**So that** I can efficiently remediate large codebases  

**Acceptance Criteria:**
- ✅ "Fix All" command available in command palette
- ✅ Applies all auto-fixable violations in current file
- ✅ "Fix All in Workspace" for project-wide remediation
- ✅ Preview mode shows all changes before applying
- ✅ Allows selective application of fixes
- ✅ Progress indicator for large operations
- ✅ Rollback capability if bulk operation causes issues

### Epic 3: Educational Guidance

#### User Story 3.1: Contextual Learning
**As a** developer learning accessibility  
**I want** explanations and examples for each violation  
**So that** I understand why the issue matters and how to prevent it  

**Acceptance Criteria:**
- ✅ Detailed violation descriptions include WCAG rule references
- ✅ Explanations use plain language, not technical jargon
- ✅ Examples show both problematic and corrected code
- ✅ Links to authoritative accessibility resources (MDN, W3C, WebAIM)
- ✅ Impact descriptions explain how violations affect users with disabilities
- ✅ Progressive disclosure: basic info with option for detailed explanation

#### User Story 3.2: Best Practice Recommendations
**As a** developer implementing accessibility features  
**I want** proactive suggestions for accessibility improvements  
**So that** I can go beyond basic compliance to create truly accessible experiences  

**Acceptance Criteria:**
- ✅ Suggests semantic HTML elements over generic divs with ARIA
- ✅ Recommends skip links for navigation-heavy pages
- ✅ Proposes focus management improvements for SPAs
- ✅ Identifies opportunities for ARIA landmarks
- ✅ Suggests keyboard navigation enhancements
- ✅ Recommendations are contextual to current code patterns

### Epic 4: Team Collaboration

#### User Story 4.1: Shared Configuration
**As a** team lead  
**I want** to enforce consistent accessibility standards across my team  
**So that** our entire codebase maintains the same quality level  

**Acceptance Criteria:**
- ✅ Team configuration file (.accessibilityrc.json) overrides user settings
- ✅ Configuration can be committed to version control
- ✅ Supports rule enablement/disablement per project
- ✅ Allows custom severity levels for different violation types
- ✅ Framework-specific configuration options
- ✅ Integration with existing ESLint/Prettier configurations

#### User Story 4.2: Reporting & Metrics
**As a** project manager  
**I want** visibility into accessibility compliance across projects  
**So that** I can track progress and identify areas needing attention  

**Acceptance Criteria:**
- ✅ Generates accessibility reports in JSON/HTML formats
- ✅ Tracks violations by category, severity, and file
- ✅ Historical trend analysis when reports are generated regularly
- ✅ Integration with CI/CD systems for automated reporting
- ✅ Customizable report templates for different audiences
- ✅ Export capabilities for external tools and dashboards

## Feature Specifications

### F1: Real-Time Diagnostic Provider

#### F1.1: Core Diagnostic Engine

**Description:** The foundation component that coordinates accessibility analysis across all supported file types and provides real-time feedback through VSCode's diagnostic system.

**Technical Requirements:**
- **Response Time**: <500ms from keystroke to diagnostic update
- **Memory Usage**: <50MB additional RAM for projects up to 1000 files
- **CPU Usage**: <5% during active typing, <1% when idle
- **Compatibility**: VSCode 1.74.0+ on Windows, macOS, Linux

**Behavior Specifications:**

1. **Activation Trigger**
   ```typescript
   // Extension activates when any supported file is opened
   activationEvents: [
     "onLanguage:javascript",
     "onLanguage:typescript", 
     "onLanguage:javascriptreact",
     "onLanguage:typescriptreact",
     "onLanguage:html",
     "onLanguage:vue"
   ]
   ```

2. **Diagnostic Lifecycle**
   - **File Open**: Full analysis within 1 second
   - **Content Change**: Incremental analysis within 500ms
   - **File Save**: Validation and optional auto-fix application
   - **File Close**: Cleanup and memory release

3. **Diagnostic Categories**
   ```typescript
   enum DiagnosticSeverity {
     Error = 0,    // WCAG A violations - blocks accessibility
     Warning = 1,  // WCAG AA violations - impacts usability
     Information = 2, // WCAG AAA violations - enhances experience
     Hint = 3     // Best practice suggestions
   }
   ```

4. **Error Recovery**
   - Malformed code doesn't break analysis of other files
   - Parser errors logged but don't crash extension
   - Fallback to basic text analysis if AST parsing fails
   - Graceful degradation for unknown syntax

**Data Models:**

```typescript
interface AccessibilityDiagnostic extends vscode.Diagnostic {
  readonly ruleId: string;           // "wcag-1.1.1-alt-text"
  readonly wcagLevel: 'A' | 'AA' | 'AAA';
  readonly category: AccessibilityCategory;
  readonly element: ElementReference;
  readonly autoFixable: boolean;
  readonly learnMoreUrl?: string;
  readonly examples?: {
    problematic: string;
    corrected: string;
  };
}

interface ElementReference {
  readonly tagName: string;
  readonly attributes: ReadonlyMap<string, string>;
  readonly location: ElementLocation;
  readonly framework: SupportedFramework;
}

enum AccessibilityCategory {
  ALT_TEXT = "alt-text",
  ARIA_LABELS = "aria-labels", 
  HEADER_HIERARCHY = "header-hierarchy",
  BUTTON_LABELS = "button-labels",
  INPUT_LABELS = "input-labels",
  TITLE_METADATA = "title-metadata"
}
```

**Implementation Details:**

```typescript
export class AccessibilityDiagnosticProvider {
  private readonly diagnosticCollection: vscode.DiagnosticCollection;
  private readonly parserEngine: FileParserEngine;
  private readonly ruleEngine: WCAGRuleEngine;
  private readonly configManager: ConfigurationManager;
  private readonly analysisCache: Map<string, AnalysisResult> = new Map();

  constructor(context: vscode.ExtensionContext) {
    this.diagnosticCollection = vscode.languages.createDiagnosticCollection('accessibility');
    this.parserEngine = new FileParserEngine();
    this.ruleEngine = new WCAGRuleEngine();
    this.configManager = new ConfigurationManager();
    
    context.subscriptions.push(this.diagnosticCollection);
    this.registerEventHandlers(context);
  }

  private registerEventHandlers(context: vscode.ExtensionContext): void {
    // Document change handler with debouncing
    const changeHandler = this.debounce(
      (event: vscode.TextDocumentChangeEvent) => this.handleDocumentChange(event),
      300
    );

    context.subscriptions.push(
      vscode.workspace.onDidChangeTextDocument(changeHandler),
      vscode.workspace.onDidOpenTextDocument(doc => this.analyzeDocument(doc)),
      vscode.workspace.onDidCloseTextDocument(doc => this.clearDiagnostics(doc)),
      vscode.workspace.onDidChangeConfiguration(e => this.handleConfigChange(e))
    );
  }

  async analyzeDocument(document: vscode.TextDocument): Promise<void> {
    if (!this.shouldAnalyze(document)) {
      return;
    }

    try {
      // Check cache first
      const cacheKey = this.getCacheKey(document);
      const cached = this.analysisCache.get(cacheKey);
      
      if (cached && cached.isValid(document)) {
        this.updateDiagnostics(document, cached.diagnostics);
        return;
      }

      // Parse document
      const startTime = performance.now();
      const parsedDocument = await this.parserEngine.parse(document);
      
      // Apply WCAG rules
      const violations = await this.ruleEngine.analyzeDocument(parsedDocument);
      
      // Convert to diagnostics
      const diagnostics = violations.map(v => this.createDiagnostic(v));
      
      // Cache results
      const analysisTime = performance.now() - startTime;
      this.analysisCache.set(cacheKey, new AnalysisResult(diagnostics, document.version, analysisTime));
      
      // Update UI
      this.updateDiagnostics(document, diagnostics);
      
      // Performance monitoring
      if (analysisTime > 1000) {
        console.warn(`Slow accessibility analysis: ${analysisTime}ms for ${document.fileName}`);
      }
      
    } catch (error) {
      this.handleAnalysisError(document, error);
    }
  }

  private shouldAnalyze(document: vscode.TextDocument): boolean {
    // Skip analysis for specific conditions
    if (document.uri.scheme !== 'file') return false;
    if (document.isUntitled && document.getText().length === 0) return false;
    if (document.languageId === 'plaintext') return false;
    
    // Check file size limit (prevent analysis of minified files)
    if (document.getText().length > 100000) {
      console.log(`Skipping analysis of large file: ${document.fileName}`);
      return false;
    }
    
    // Check if enabled for this file type
    return this.configManager.isEnabledForLanguage(document.languageId);
  }

  private debounce<T extends (...args: any[]) => any>(
    func: T, 
    delay: number
  ): (...args: Parameters<T>) => void {
    let timeoutId: NodeJS.Timeout;
    
    return (...args: Parameters<T>) => {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => func(...args), delay);
    };
  }
}

class AnalysisResult {
  constructor(
    public readonly diagnostics: AccessibilityDiagnostic[],
    public readonly documentVersion: number,
    public readonly analysisTime: number,
    private readonly timestamp: number = Date.now()
  ) {}

  isValid(document: vscode.TextDocument): boolean {
    const maxAge = 30000; // 30 seconds
    return document.version === this.documentVersion && 
           (Date.now() - this.timestamp) < maxAge;
  }
}
```

### F2: Framework-Specific Parsers

#### F2.1: React/JSX Parser

**Description:** Specialized parser for React components that understands JSX syntax, component props, and React-specific patterns.

**Technical Requirements:**
- Parse JSX and TSX files using Babel parser
- Handle React components (functional and class-based)
- Understand React props vs HTML attributes
- Support React Fragments, conditional rendering, and loops
- Handle styled-components and emotion CSS-in-JS

**Supported Syntax Patterns:**

```jsx
// Standard JSX elements
<img src={logoUrl} alt="Company logo" />

// Conditional attributes
<img src={logo} alt={isDecorative ? "" : "Company logo"} />

// Dynamic content
<img src={`/images/${imageName}.png`} alt={imageDescription} />

// Component props
<CustomImage source={logoUrl} altText="Company logo" />

// Fragments
<>
  <h1>Title</h1>
  <img src={logo} />
</>

// Conditional rendering
{showImage && <img src={logo} alt="Logo" />}

// Loops
{images.map(img => <img key={img.id} src={img.url} alt={img.alt} />)}
```

**Implementation:**

```typescript
export class ReactParser implements FrameworkParser {
  private readonly babelOptions: ParserOptions = {
    sourceType: 'module',
    allowImportExportEverywhere: true,
    allowReturnOutsideFunction: true,
    plugins: [
      'jsx',
      'typescript',
      'classProperties',
      'dynamicImport',
      'exportDefaultFrom',
      'exportNamespaceFrom',
      'decorators-legacy',
      'optionalChaining',
      'nullishCoalescingOperator'
    ]
  };

  async parse(content: string, fileName: string): Promise<ParsedDocument> {
    try {
      const ast = parse(content, this.babelOptions);
      const elements = this.extractElements(ast);
      const components = this.extractComponents(ast);
      
      return new ParsedDocument({
        content,
        fileName,
        framework: 'react',
        ast,
        elements,
        components,
        imports: this.extractImports(ast)
      });
    } catch (error) {
      throw new ParseError(`Failed to parse React file ${fileName}`, error);
    }
  }

  private extractElements(ast: t.File): ParsedElement[] {
    const elements: ParsedElement[] = [];
    
    traverse(ast, {
      JSXElement: (path) => {
        const element = this.parseJSXElement(path.node, path);
        if (element) {
          elements.push(element);
        }
      },
      
      JSXFragment: (path) => {
        // Handle React Fragments
        const fragmentElements = this.parseJSXFragment(path.node, path);
        elements.push(...fragmentElements);
      }
    });

    return elements;
  }

  private parseJSXElement(node: t.JSXElement, path: NodePath<t.JSXElement>): ParsedElement | null {
    const openingElement = node.openingElement;
    
    if (!t.isJSXIdentifier(openingElement.name)) {
      return null; // Skip complex member expressions for now
    }

    const tagName = openingElement.name.name;
    const attributes = this.parseJSXAttributes(openingElement.attributes, path);
    const children = this.parseJSXChildren(node.children, path);
    const location = this.getElementLocation(node);

    // Determine if this is a DOM element or React component
    const isDOMElement = tagName.charAt(0) === tagName.charAt(0).toLowerCase();
    const elementType = isDOMElement ? 'dom' : 'component';

    // Extract accessibility-relevant information
    const accessibilityInfo = this.extractAccessibilityInfo(tagName, attributes, children);

    return new ParsedElement({
      tagName,
      attributes,
      children,
      location,
      framework: 'react',
      elementType,
      accessibilityInfo,
      context: this.getElementContext(path)
    });
  }

  private parseJSXAttributes(
    attributes: (t.JSXAttribute | t.JSXSpreadAttribute)[],
    path: NodePath<t.JSXElement>
  ): ElementAttributes {
    const attrs = new Map<string, AttributeValue>();

    for (const attr of attributes) {
      if (t.isJSXAttribute(attr) && t.isJSXIdentifier(attr.name)) {
        const name = attr.name.name;
        const value = this.parseAttributeValue(attr.value, path);
        attrs.set(name, value);
      } else if (t.isJSXSpreadAttribute(attr)) {
        // Handle spread attributes: {...props}
        const spreadValue = this.parseSpreadAttribute(attr.argument, path);
        if (spreadValue) {
          attrs.set('...spread', spreadValue);
        }
      }
    }

    return attrs;
  }

  private parseAttributeValue(
    value: t.JSXAttribute['value'],
    path: NodePath<t.JSXElement>
  ): AttributeValue {
    if (!value) {
      // Boolean attribute like <input required />
      return { type: 'boolean', value: true, raw: 'true' };
    }

    if (t.isStringLiteral(value)) {
      return { 
        type: 'string', 
        value: value.value, 
        raw: `"${value.value}"` 
      };
    }

    if (t.isJSXExpressionContainer(value)) {
      return this.parseJSXExpression(value.expression, path);
    }

    return { type: 'unknown', value: null, raw: '' };
  }

  private parseJSXExpression(
    expression: t.Expression | t.JSXEmptyExpression,
    path: NodePath<t.JSXElement>
  ): AttributeValue {
    if (t.isJSXEmptyExpression(expression)) {
      return { type: 'empty', value: null, raw: '{}' };
    }

    if (t.isStringLiteral(expression)) {
      return { 
        type: 'string', 
        value: expression.value, 
        raw: `{"${expression.value}"}` 
      };
    }

    if (t.isNumericLiteral(expression)) {
      return { 
        type: 'number', 
        value: expression.value, 
        raw: `{${expression.value}}` 
      };
    }

    if (t.isBooleanLiteral(expression)) {
      return { 
        type: 'boolean', 
        value: expression.value, 
        raw: `{${expression.value}}` 
      };
    }

    if (t.isIdentifier(expression)) {
      return { 
        type: 'variable', 
        value: expression.name, 
        raw: `{${expression.name}}`,
        scope: this.resolveVariableScope(expression.name, path)
      };
    }

    if (t.isTemplateLiteral(expression)) {
      const staticValue = this.evaluateTemplateLiteral(expression);
      return {
        type: 'template',
        value: staticValue,
        raw: this.getCodeSnippet(expression),
        dynamic: staticValue === null
      };
    }

    if (t.isConditionalExpression(expression)) {
      return {
        type: 'conditional',
        value: null, // Cannot determine at static analysis
        raw: this.getCodeSnippet(expression),
        branches: {
          consequent: this.parseJSXExpression(expression.consequent, path),
          alternate: this.parseJSXExpression(expression.alternate, path)
        }
      };
    }

    // Fallback for complex expressions
    return {
      type: 'expression',
      value: null,
      raw: this.getCodeSnippet(expression),
      complex: true
    };
  }

  private parseJSXChildren(
    children: (t.JSXText | t.JSXElement | t.JSXFragment | t.JSXExpressionContainer)[],
    path: NodePath<t.JSXElement>
  ): ElementChildren {
    const textContent: string[] = [];
    const childElements: ParsedElement[] = [];

    for (const child of children) {
      if (t.isJSXText(child)) {
        const text = child.value.trim();
        if (text) {
          textContent.push(text);
        }
      } else if (t.isJSXElement(child)) {
        const childElement = this.parseJSXElement(child, path);
        if (childElement) {
          childElements.push(childElement);
        }
      } else if (t.isJSXExpressionContainer(child)) {
        const expressionText = this.extractTextFromExpression(child.expression);
        if (expressionText) {
          textContent.push(expressionText);
        }
      }
    }

    return {
      textContent: textContent.join(' '),
      childElements,
      hasInteractiveChildren: childElements.some(el => this.isInteractiveElement(el))
    };
  }

  private extractAccessibilityInfo(
    tagName: string,
    attributes: ElementAttributes,
    children: ElementChildren
  ): AccessibilityInfo {
    return {
      isInteractive: this.isInteractiveElement({ tagName, attributes }),
      hasAccessibleName: this.hasAccessibleName(tagName, attributes, children),
      ariaAttributes: this.extractAriaAttributes(attributes),
      semanticRole: this.getSemanticRole(tagName, attributes),
      focusable: this.isFocusable(tagName, attributes)
    };
  }

  private isInteractiveElement(element: { tagName: string; attributes: ElementAttributes }): boolean {
    const interactiveTags = ['button', 'a', 'input', 'select', 'textarea'];
    
    if (interactiveTags.includes(element.tagName.toLowerCase())) {
      return true;
    }

    // Check for onClick handlers or interactive roles
    const hasClickHandler = element.attributes.has('onClick') || 
                           element.attributes.has('onPress');
    
    const role = element.attributes.get('role')?.value;
    const interactiveRoles = ['button', 'link', 'tab', 'menuitem'];
    
    return hasClickHandler || (typeof role === 'string' && interactiveRoles.includes(role));
  }

  private hasAccessibleName(
    tagName: string,
    attributes: ElementAttributes,
    children: ElementChildren
  ): boolean {
    // Check for aria-label
    if (attributes.has('aria-label') || attributes.has('ariaLabel')) {
      const ariaLabel = attributes.get('aria-label') || attributes.get('ariaLabel');
      return ariaLabel?.value !== null && ariaLabel?.value !== '';
    }

    // Check for aria-labelledby
    if (attributes.has('aria-labelledby') || attributes.has('ariaLabelledBy')) {
      return true;
    }

    // Check for alt text on images
    if (tagName === 'img') {
      return attributes.has('alt');
    }

    // Check for text content
    return children.textContent.trim().length > 0;
  }

  private getElementContext(path: NodePath<t.JSXElement>): ElementContext {
    const parent = path.findParent(p => p.isJSXElement());
    const component = path.findParent(p => 
      p.isFunctionDeclaration() || 
      p.isArrowFunctionExpression() ||
      p.isVariableDeclarator()
    );

    return {
      parentElement: parent ? this.getElementInfo(parent.node as t.JSXElement) : null,
      componentName: this.getComponentName(component),
      isConditionallyRendered: this.isConditionallyRendered(path),
      isInLoop: this.isInLoop(path)
    };
  }

  private resolveVariableScope(
    variableName: string,
    path: NodePath<t.JSXElement>
  ): VariableScope | null {
    const binding = path.scope.getBinding(variableName);
    
    if (!binding) {
      return null;
    }

    return {
      type: this.getBindingType(binding),
      source: this.getBindingSource(binding),
      mutable: binding.kind !== 'const'
    };
  }

  private evaluateTemplateLiteral(node: t.TemplateLiteral): string | null {
    // Only evaluate if all expressions are static
    if (node.expressions.some(expr => !this.isStaticExpression(expr))) {
      return null;
    }

    let result = '';
    for (let i = 0; i < node.quasis.length; i++) {
      result += node.quasis[i].value.cooked || '';
      if (i < node.expressions.length) {
        const expr = node.expressions[i];
        const staticValue = this.evaluateStaticExpression(expr);
        if (staticValue === null) {
          return null;
        }
        result += String(staticValue);
      }
    }

    return result;
  }
}

// Data models for React parser
interface AttributeValue {
  type: 'string' | 'number' | 'boolean' | 'variable' | 'expression' | 'template' | 'conditional' | 'empty' | 'unknown';
  value: any;
  raw: string;
  dynamic?: boolean;
  scope?: VariableScope;
  branches?: {
    consequent: AttributeValue;
    alternate: AttributeValue;
  };
  complex?: boolean;
}

interface ElementChildren {
  textContent: string;
  childElements: ParsedElement[];
  hasInteractiveChildren: boolean;
}

interface AccessibilityInfo {
  isInteractive: boolean;
  hasAccessibleName: boolean;
  ariaAttributes: Map<string, AttributeValue>;
  semanticRole: string | null;
  focusable: boolean;
}

interface ElementContext {
  parentElement: ElementInfo | null;
  componentName: string | null;
  isConditionallyRendered: boolean;
  isInLoop: boolean;
}

interface VariableScope {
  type: 'param' | 'local' | 'imported' | 'global';
  source: string;
  mutable: boolean;
}
```

#### F2.2: Angular Template Parser

**Description:** Parser for Angular component templates that understands Angular template syntax, directives, and data binding.

**Supported Syntax Patterns:**

```html
<!-- Property binding -->
<img [src]="logoUrl" [alt]="logoAltText">

<!-- Event binding -->
<button (click)="handleClick()" [attr.aria-label]="buttonLabel">

<!-- Two-way binding -->
<input [(ngModel)]="userInput" [attr.aria-describedby]="helpId">

<!-- Structural directives -->
<div *ngIf="showContent" role="main">
  <img *ngFor="let image of images" [src]="image.url" [alt]="image.alt">
</div>

<!-- Template reference variables -->
<input #usernameInput aria-labelledby="username-label">
<label id="username-label" for="usernameInput">Username</label>

<!-- Pipes -->
<img [alt]="imageDescription | titlecase">
```

**Implementation:**

```typescript
export class AngularParser implements FrameworkParser {
  private readonly htmlParser = new Parser(getHtmlTagDefinition);

  async parse(content: string, fileName: string): Promise<ParsedDocument> {
    try {
      // Parse Angular template
      const parseResult = parseTemplate(content, fileName, {
        preserveWhitespaces: false,
        interpolationConfig: DEFAULT_INTERPOLATION_CONFIG,
        range: { startPos: 0, startLine: 0, startCol: 0 },
        enableI18nLegacyMessageIdFormat: false
      });

      if (parseResult.errors.length > 0) {
        console.warn(`Angular template parsing errors in ${fileName}:`, parseResult.errors);
      }

      const elements = this.extractElements(parseResult.nodes);
      
      return new ParsedDocument({
        content,
        fileName,
        framework: 'angular',
        ast: parseResult,
        elements,
        templateVariables: this.extractTemplateVariables(parseResult.nodes),
        directives: this.extractDirectives(parseResult.nodes)
      });
    } catch (error) {
      throw new ParseError(`Failed to parse Angular template ${fileName}`, error);
    }
  }

  private extractElements(nodes: TmplAstNode[]): ParsedElement[] {
    const elements: ParsedElement[] = [];
    
    const visitor = new class extends RecursiveTemplateAstVisitor {
      override visitElement(element: TmplAstElement): void {
        const parsedElement = this.parseAngularElement(element);
        if (parsedElement) {
          elements.push(parsedElement);
        }
        super.visitElement(element);
      }
    };

    nodes.forEach(node => visitor.visit(node));
    return elements;
  }

  private parseAngularElement(element: TmplAstElement): ParsedElement {
    const tagName = element.name;
    const attributes = this.parseAngularAttributes(element);
    const children = this.parseAngularChildren(element.children);
    const location = this.getAngularElementLocation(element);

    return new ParsedElement({
      tagName,
      attributes,
      children,
      location,
      framework: 'angular',
      elementType: 'dom',
      directives: this.extractElementDirectives(element),
      bindings: this.extractElementBindings(element),
      accessibilityInfo: this.extractAccessibilityInfo(tagName, attributes, children)
    });
  }

  private parseAngularAttributes(element: TmplAstElement): ElementAttributes {
    const attrs = new Map<string, AttributeValue>();

    // Regular attributes
    element.attributes.forEach(attr => {
      attrs.set(attr.name, {
        type: 'string',
        value: attr.value,
        raw: `${attr.name}="${attr.value}"`
      });
    });

    // Property bindings [property]="value"
    element.inputs.forEach(input => {
      const binding = this.parseAngularBinding(input);
      attrs.set(input.name, binding);
    });

    // Event bindings (click)="handler()"
    element.outputs.forEach(output => {
      attrs.set(`(${output.name})`, {
        type: 'event',
        value: output.handler.toString(),
        raw: `(${output.name})="${output.handler}"`
      });
    });

    return attrs;
  }

  private parseAngularBinding(input: TmplAstBoundAttribute): AttributeValue {
    const ast = input.value;
    
    if (ast instanceof LiteralPrimitive) {
      return {
        type: typeof ast.value as any,
        value: ast.value,
        raw: `[${input.name}]="${ast.value}"`
      };
    }

    if (ast instanceof PropertyRead) {
      return {
        type: 'variable',
        value: ast.name,
        raw: `[${input.name}]="${ast.name}"`,
        scope: { type: 'component', source: 'component', mutable: true }
      };
    }

    if (ast instanceof Interpolation) {
      return {
        type: 'interpolation',
        value: this.evaluateInterpolation(ast),
        raw: `[${input.name}]="${this.getASTString(ast)}"`,
        dynamic: true
      };
    }

    // Complex expressions
    return {
      type: 'expression',
      value: null,
      raw: `[${input.name}]="${this.getASTString(ast)}"`,
      complex: true
    };
  }

  private extractElementDirectives(element: TmplAstElement): AngularDirective[] {
    const directives: AngularDirective[] = [];

    // Structural directives (*ngIf, *ngFor, etc.)
    element.references.forEach(ref => {
      if (ref.name.startsWith('*')) {
        directives.push({
          name: ref.name,
          type: 'structural',
          value: ref.value
        });
      }
    });

    return directives;
  }

  private parseAngularChildren(children: TmplAstNode[]): ElementChildren {
    const textContent: string[] = [];
    const childElements: ParsedElement[] = [];

    children.forEach(child => {
      if (child instanceof TmplAstText) {
        textContent.push(child.value.trim());
      } else if (child instanceof TmplAstElement) {
        const childElement = this.parseAngularElement(child);
        childElements.push(childElement);
      } else if (child instanceof TmplAstBoundText) {
        // Handle interpolated text {{value}}
        const interpolatedText = this.evaluateInterpolation(child.value);
        if (interpolatedText) {
          textContent.push(interpolatedText);
        }
      }
    });

    return {
      textContent: textContent.filter(text => text.length > 0).join(' '),
      childElements,
      hasInteractiveChildren: childElements.some(el => this.isInteractiveElement(el))
    };
  }

  private evaluateInterpolation(ast: AST): string | null {
    // Simple static evaluation for basic cases
    if (ast instanceof LiteralPrimitive) {
      return String(ast.value);
    }

    if (ast instanceof PropertyRead && this.isStaticProperty(ast.name)) {
      // Could be evaluated if we had component context
      return null;
    }

    return null; // Cannot evaluate complex interpolations statically
  }
}

interface AngularDirective {
  name: string;
  type: 'structural' | 'attribute';
  value: string;
}
```

#### F2.3: Vue.js Parser

**Description:** Parser for Vue.js Single File Components (SFCs) that understands Vue template syntax, directives, and component structure.

**Supported Syntax Patterns:**

```vue
<template>
  <!-- Attribute binding -->
  <img :src="logoUrl" :alt="logoAltText">
  
  <!-- Event handling -->
  <button @click="handleClick" :aria-label="buttonLabel">
  
  <!-- Two-way binding -->
  <input v-model="userInput" :aria-describedby="helpId">
  
  <!-- Conditional rendering -->
  <div v-if="showContent" role="main">
    <img v-for="image in images" :key="image.id" :src="image.url" :alt="image.alt">
  </div>
  
  <!-- Slots -->
  <CustomButton>
    <span aria-hidden="true">Icon</span>
    Button Text
  </CustomButton>
</template>
```

**Implementation:**

```typescript
export class VueParser implements FrameworkParser {
  async parse(content: string, fileName: string): Promise<ParsedDocument> {
    try {
      const { descriptor, errors } = compileTemplate({
        source: content,
        filename: fileName,
        id: this.generateComponentId(fileName)
      });

      if (errors.length > 0) {
        console.warn(`Vue template parsing errors in ${fileName}:`, errors);
      }

      const templateContent = descriptor.template?.content || '';
      const elements = this.extractElements(templateContent);

      return new ParsedDocument({
        content,
        fileName,
        framework: 'vue',
        ast: descriptor,
        elements,
        components: this.extractComponentUsages(templateContent),
        directives: this.extractCustomDirectives(templateContent)
      });
    } catch (error) {
      throw new ParseError(`Failed to parse Vue SFC ${fileName}`, error);
    }
  }

  private extractElements(templateContent: string): ParsedElement[] {
    const elements: ParsedElement[] = [];
    
    // Use HTML parser with Vue-specific handling
    const ast = parse(templateContent, {
      sourceCodeLocationInfo: true,
      onParseError: (error) => {
        console.warn('Vue template parse error:', error);
      }
    });

    this.traverseVueTemplate(ast, (node) => {
      if (node.nodeName !== '#text' && node.nodeName !== '#comment') {
        const element = this.parseVueElement(node);
        if (element) {
          elements.push(element);
        }
      }
    });

    return elements;
  }

  private parseVueElement(node: any): ParsedElement {
    const tagName = node.nodeName;
    const attributes = this.parseVueAttributes(node.attrs || []);
    const children = this.parseVueChildren(node.childNodes || []);
    const location = this.getVueElementLocation(node);

    return new ParsedElement({
      tagName,
      attributes,
      children,
      location,
      framework: 'vue',
      elementType: this.isVueComponent(tagName) ? 'component' : 'dom',
      directives: this.extractVueDirectives(node.attrs || []),
      accessibilityInfo: this.extractAccessibilityInfo(tagName, attributes, children)
    });
  }

  private parseVueAttributes(attrs: any[]): ElementAttributes {
    const attributes = new Map<string, AttributeValue>();

    attrs.forEach(attr => {
      const name = attr.name;
      const value = attr.value;

      if (name.startsWith('v-')) {
        // Vue directive
        attributes.set(name, this.parseVueDirective(name, value));
      } else if (name.startsWith(':')) {
        // Shorthand for v-bind
        const bindingName = name.slice(1);
        attributes.set(bindingName, this.parseVueBinding(bindingName, value));
      } else if (name.startsWith('@')) {
        // Shorthand for v-on
        const eventName = name.slice(1);
        attributes.set(`@${eventName}`, {
          type: 'event',
          value: value,
          raw: `@${eventName}="${value}"`
        });
      } else {
        // Regular attribute
        attributes.set(name, {
          type: 'string',
          value: value,
          raw: `${name}="${value}"`
        });
      }
    });

    return attributes;
  }

  private parseVueDirective(name: string, value: string): AttributeValue {
    const directiveName = name.replace('v-', '');
    
    switch (directiveName) {
      case 'model':
        return {
          type: 'model',
          value: value,
          raw: `v-model="${value}"`,
          twoWay: true
        };
      
      case 'if':
      case 'show':
        return {
          type: 'conditional',
          value: value,
          raw: `v-${directiveName}="${value}"`
        };
      
      case 'for':
        return {
          type: 'loop',
          value: value,
          raw: `v-for="${value}"`
        };
      
      default:
        return {
          type: 'directive',
          value: value,
          raw: `v-${directiveName}="${value}"`
        };
    }
  }

  private parseVueBinding(name: string, value: string): AttributeValue {
    // Simple static analysis for Vue bindings
    if (this.isStringLiteral(value)) {
      return {
        type: 'string',
        value: this.parseStringLiteral(value),
        raw: `:${name}="${value}"`
      };
    }

    if (this.isNumericLiteral(value)) {
      return {
        type: 'number',
        value: Number(value),
        raw: `:${name}="${value}"`
      };
    }

    if (this.isBooleanLiteral(value)) {
      return {
        type: 'boolean',
        value: value === 'true',
        raw: `:${name}="${value}"`
      };
    }

    // Variable or expression
    return {
      type: 'variable',
      value: value,
      raw: `:${name}="${value}"`,
      scope: { type: 'component', source: 'data', mutable: true }
    };
  }
}
```

### F3: WCAG Rule Engine Implementation

#### F3.1: Rule Architecture

**Description:** Extensible rule engine that implements WCAG 2.2 accessibility checks with configurable severity levels and intelligent fix suggestions.

**Design Principles:**
- **Modularity**: Each WCAG rule is a separate module
- **Extensibility**: Easy to add new rules or modify existing ones
- **Performance**: Rules execute in parallel when possible
- **Configuration**: Rules can be enabled/disabled per project
- **Localization**: Support for multiple languages in rule messages

**Core Architecture:**

```typescript
abstract class WCAGRule {
  abstract readonly id: string;
  abstract readonly wcagCriterion: string;
  abstract readonly level: 'A' | 'AA' | 'AAA';
  abstract readonly category: AccessibilityCategory;
  abstract readonly description: string;
  
  protected constructor(protected config: RuleConfiguration) {}
  
  abstract analyze(element: ParsedElement, context: DocumentContext): Promise<RuleViolation[]>;
  abstract generateFixes(violation: RuleViolation): Promise<QuickFix[]>;
  
  protected isEnabled(): boolean {
    return this.config.isRuleEnabled(this.id);
  }
  
  protected getSeverity(): DiagnosticSeverity {
    return this.config.getSeverityForLevel(this.level);
  }
  
  protected createViolation(params: ViolationParams): RuleViolation {
    return new RuleViolation({
      ruleId: this.id,
      wcagCriterion: this.wcagCriterion,
      level: this.getSeverity(),
      category: this.category,
      message: this.localizeMessage(params.messageKey, params.messageParams),
      element: params.element,
      location: params.location,
      fixes: params.fixes || [],
      learnMoreUrl: this.getLearnMoreUrl(),
      impact: this.getImpactDescription()
    });
  }
  
  protected localizeMessage(key: string, params?: Record<string, any>): string {
    return this.config.messageProvider.getMessage(key, params);
  }
  
  protected getLearnMoreUrl(): string {
    return `https://www.w3.org/WAI/WCAG21/Understanding/${this.wcagCriterion.replace('.', '')}.html`;
  }
}

class WCAGRuleEngine {
  private rules: Map<string, WCAGRule> = new Map();
  private ruleCategories: Map<AccessibilityCategory, WCAGRule[]> = new Map();
  
  constructor(private config: EngineConfiguration) {
    this.initializeRules();
  }
  
  private initializeRules(): void {
    const ruleClasses = [
      AltTextRule,
      AriaLabelRule,
      HeaderHierarchyRule,
      ButtonLabelRule,
      InputLabelRule,
      TitleMetadataRule,
      ColorContrastRule,
      KeyboardNavigationRule,
      FocusManagementRule
    ];
    
    ruleClasses.forEach(RuleClass => {
      const rule = new RuleClass(this.config.getRuleConfig(RuleClass.prototype.id));
      this.rules.set(rule.id, rule);
      
      if (!this.ruleCategories.has(rule.category)) {
        this.ruleCategories.set(rule.category, []);
      }
      this.ruleCategories.get(rule.category)!.push(rule);
    });
  }
  
  async analyzeDocument(document: ParsedDocument): Promise<RuleViolation[]> {
    const context = new DocumentContext(document);
    const violations: RuleViolation[] = [];
    
    // Analyze elements with applicable rules
    for (const element of document.elements) {
      const applicableRules = this.getApplicableRules(element);
      
      // Run rules in parallel for performance
      const rulePromises = applicableRules.map(rule => 
        rule.analyze(element, context).catch(error => {
          console.error(`Error in rule ${rule.id}:`, error);
          return [];
        })
      );
      
      const ruleResults = await Promise.all(rulePromises);
      violations.push(...ruleResults.flat());
    }
    
    // Run document-level rules
    const documentRules = this.getDocumentLevelRules();
    for (const rule of documentRules) {
      try {
        const documentViolations = await rule.analyzeDocument(document, context);
        violations.push(...documentViolations);
      } catch (error) {
        console.error(`Error in document rule ${rule.id}:`, error);
      }
    }
    
    return this.sortAndDeduplicateViolations(violations);
  }
  
  private getApplicableRules(element: ParsedElement): WCAGRule[] {
    const rules: WCAGRule[] = [];
    
    // Get rules based on element type
    if (this.isImageElement(element)) {
      rules.push(...this.getRulesForCategory(AccessibilityCategory.ALT_TEXT));
    }
    
    if (this.isInteractiveElement(element)) {
      rules.push(...this.getRulesForCategory(AccessibilityCategory.ARIA_LABELS));
      rules.push(...this.getRulesForCategory(AccessibilityCategory.BUTTON_LABELS));
    }
    
    if (this.isFormElement(element)) {
      rules.push(...this.getRulesForCategory(AccessibilityCategory.INPUT_LABELS));
    }
    
    if (this.isHeadingElement(element)) {
      rules.push(...this.getRulesForCategory(AccessibilityCategory.HEADER_HIERARCHY));
    }
    
    return rules.filter(rule => rule.isEnabled());
  }
}
```

#### F3.2: Alt Text Rule Implementation

**Description:** Comprehensive implementation of WCAG 1.1.1 (Non-text Content) with intelligent suggestions and context awareness.

**Rule Specification:**

```typescript
export class AltTextRule extends WCAGRule {
  readonly id = 'wcag-1.1.1-alt-text';
  readonly wcagCriterion = '1.1.1';
  readonly level = 'A' as const;
  readonly category = AccessibilityCategory.ALT_TEXT;
  readonly description = 'Images must have alternative text that serves the equivalent purpose';

  private readonly decorativeImagePatterns = [
    /decoration/i,
    /border/i,
    /divider/i,
    /spacer/i,
    /bullet/i
  ];

  private readonly genericAltTextPatterns = [
    /^(image|picture|photo|graphic|icon|logo)$/i,
    /^(untitled|image\d+|img_\d+)$/i,
    /\.(jpg|jpeg|png|gif|svg|webp)$/i
  ];

  async analyze(element: ParsedElement, context: DocumentContext): Promise<RuleViolation[]> {
    if (!this.isImageElement(element)) {
      return [];
    }

    const violations: RuleViolation[] = [];
    
    // Check for missing alt attribute
    const missingAltViolation = this.checkMissingAlt(element);
    if (missingAltViolation) {
      violations.push(missingAltViolation);
    }

    // Check alt text quality
    const qualityViolations = this.checkAltTextQuality(element, context);
    violations.push(...qualityViolations);

    // Check for redundant alt text
    const redundancyViolation = this.checkRedundantAltText(element, context);
    if (redundancyViolation) {
      violations.push(redundancyViolation);
    }

    return violations;
  }

  private checkMissingAlt(element: ParsedElement): RuleViolation | null {
    const altAttribute = element.attributes.get('alt');
    
    if (!altAttribute) {
      return this.createViolation({
        messageKey: 'alt-text.missing',
        element,
        location: element.location,
        fixes: this.generateMissingAltFixes(element)
      });
    }

    return null;
  }

  private checkAltTextQuality(element: ParsedElement, context: DocumentContext): RuleViolation[] {
    const violations: RuleViolation[] = [];
    const altAttribute = element.attributes.get('alt');
    
    if (!altAttribute || !altAttribute.value) {
      return violations;
    }

    const altText = String(altAttribute.value).trim();
    
    // Check for empty alt on content images
    if (altText === '' && !this.isDecorativeImage(element, context)) {
      violations.push(this.createViolation({
        messageKey: 'alt-text.empty-content-image',
        element,
        location: element.location,
        fixes: this.generateContentImageFixes(element, context)
      }));
    }

    // Check for filename as alt text
    if (this.isFilenameAsAltText(altText, element)) {
      violations.push(this.createViolation({
        messageKey: 'alt-text.filename-as-alt',
        messageParams: { altText },
        element,
        location: element.location,
        fixes: this.generateDescriptiveAltFixes(element, context)
      }));
    }

    // Check for generic alt text
    if (this.isGenericAltText(altText)) {
      violations.push(this.createViolation({
        messageKey: 'alt-text.generic',
        messageParams: { altText },
        element,
        location: element.location,
        fixes: this.generateDescriptiveAltFixes(element, context)
      }));
    }

    // Check alt text length
    if (altText.length > 125) {
      violations.push(this.createViolation({
        messageKey: 'alt-text.too-long',
        messageParams: { length: altText.length },
        element,
        location: element.location,
        fixes: this.generateConciseAltFixes(element, altText)
      }));
    }

    return violations;
  }

  private checkRedundantAltText(element: ParsedElement, context: DocumentContext): RuleViolation | null {
    const altAttribute = element.attributes.get('alt');
    const altText = altAttribute?.value ? String(altAttribute.value).trim() : '';
    
    if (!altText) {
      return null;
    }

    // Check if alt text repeats nearby text content
    const nearbyText = context.getNearbyTextContent(element, 100); // 100 char radius
    
    if (this.isTextRedundant(altText, nearbyText)) {
      return this.createViolation({
        messageKey: 'alt-text.redundant',
        messageParams: { altText, nearbyText },
        element,
        location: element.location,
        fixes: this.generateRedundancyFixes(element, altText, nearbyText)
      });
    }

    return null;
  }

  async generateFixes(violation: RuleViolation): Promise<QuickFix[]> {
    const element = violation.element;
    
    switch (violation.messageKey) {
      case 'alt-text.missing':
        return this.generateMissingAltFixes(element);
      
      case 'alt-text.empty-content-image':
        return this.generateContentImageFixes(element, violation.context);
      
      case 'alt-text.filename-as-alt':
      case 'alt-text.generic':
        return this.generateDescriptiveAltFixes(element, violation.context);
      
      case 'alt-text.too-long':
        return this.generateConciseAltFixes(element, violation.currentAltText);
      
      case 'alt-text.redundant':
        return this.generateRedundancyFixes(element, violation.altText, violation.nearbyText);
      
      default:
        return [];
    }
  }

  private generateMissingAltFixes(element: ParsedElement): QuickFix[] {
    const fixes: QuickFix[] = [];
    
    // Option 1: Mark as decorative
    fixes.push(new QuickFix({
      id: 'add-empty-alt',
      title: 'Mark as decorative (alt="")',
      description: 'Add empty alt attribute to indicate this image is decorative',
      category: 'decorative',
      confidence: this.isLikelyDecorative(element) ? 'high' : 'medium',
      edit: new AttributeEdit({
        type: 'add-attribute',
        attribute: 'alt',
        value: ''
      })
    }));

    // Option 2: Add descriptive alt text
    const suggestedAlt = this.generateAltTextSuggestion(element);
    fixes.push(new QuickFix({
      id: 'add-descriptive-alt',
      title: 'Add descriptive alt text',
      description: 'Add alternative text that describes the image content',
      category: 'descriptive',
      confidence: 'medium',
      edit: new AttributeEdit({
        type: 'add-attribute',
        attribute: 'alt',
        value: suggestedAlt
      }),
      userInput: {
        type: 'text',
        prompt: 'Describe what this image shows:',
        placeholder: suggestedAlt,
        validation: {
          minLength: 1,
          maxLength: 125,
          pattern: /^[^<>]*$/ // No HTML tags
        }
      }
    }));

    return fixes;
  }

  private generateContentImageFixes(element: ParsedElement, context: DocumentContext): QuickFix[] {
    const fixes: QuickFix[] = [];
    const suggestedAlt = this.generateContextualAltText(element, context);

    fixes.push(new QuickFix({
      id: 'add-content-alt',
      title: `Add alt text: "${suggestedAlt}"`,
      description: 'Add descriptive alternative text for this content image',
      category: 'descriptive',
      confidence: 'high',
      edit: new AttributeEdit({
        type: 'modify-attribute',
        attribute: 'alt',
        value: suggestedAlt
      })
    }));

    // Option to convert to decorative if user disagrees
    fixes.push(new QuickFix({
      id: 'mark-decorative',
      title: 'Actually, this image is decorative',
      description: 'Keep empty alt text to mark as decorative',
      category: 'decorative',
      confidence: 'medium'
      // No edit needed - alt="" already exists
    }));

    return fixes;
  }

  private generateDescriptiveAltFixes(element: ParsedElement, context: DocumentContext): QuickFix[] {
    const fixes: QuickFix[] = [];
    const suggestions = this.generateMultipleAltSuggestions(element, context);

    suggestions.forEach((suggestion, index) => {
      fixes.push(new QuickFix({
        id: `improve-alt-${index}`,
        title: `Use: "${suggestion.text}"`,
        description: suggestion.reasoning,
        category: 'descriptive',
        confidence: suggestion.confidence,
        edit: new AttributeEdit({
          type: 'modify-attribute',
          attribute: 'alt',
          value: suggestion.text
        })
      }));
    });

    // Custom option
    fixes.push(new QuickFix({
      id: 'custom-alt',
      title: 'Write custom alt text',
      description: 'Enter your own description for this image',
      category: 'descriptive',
      confidence: 'high',
      userInput: {
        type: 'text',
        prompt: 'Describe what this image shows:',
        placeholder: suggestions[0]?.text || '',
        validation: {
          minLength: 1,
          maxLength: 125,
          pattern: /^[^<>]*$/
        }
      },
      edit: new AttributeEdit({
        type: 'modify-attribute',
        attribute: 'alt',
        value: '${userInput}' // Placeholder for user input
      })
    }));

    return fixes;
  }

  private generateAltTextSuggestion(element: ParsedElement): string {
    const src = element.attributes.get('src')?.value;
    
    if (!src) {
      return 'Describe this image';
    }

    // Extract meaningful words from filename
    const filename = String(src).split('/').pop()?.split('.')[0] || '';
    const words = filename
      .replace(/[-_]/g, ' ')
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .toLowerCase()
      .split(' ')
      .filter(word => word.length > 2 && !this.isCommonWord(word));

    if (words.length === 0) {
      return 'Describe this image';
    }

    return this.capitalizeFirstLetter(words.join(' '));
  }

  private generateContextualAltText(element: ParsedElement, context: DocumentContext): string {
    // Analyze surrounding context for clues
    const nearbyHeadings = context.getNearbyHeadings(element, 200);
    const parentContent = context.getParentContent(element);
    const siblingImages = context.getSiblingImages(element);

    // Use context to generate more relevant alt text
    if (nearbyHeadings.length > 0) {
      const heading = nearbyHeadings[0];
      return `Image related to ${heading.toLowerCase()}`;
    }

    if (parentContent.includes('logo')) {
      return 'Logo';
    }

    if (parentContent.includes('profile')) {
      return 'Profile photo';
    }

    return this.generateAltTextSuggestion(element);
  }

  private generateMultipleAltSuggestions(element: ParsedElement, context: DocumentContext): AltTextSuggestion[] {
    const suggestions: AltTextSuggestion[] = [];
    
    // Filename-based suggestion
    const filenameSuggestion = this.generateAltTextSuggestion(element);
    if (filenameSuggestion !== 'Describe this image') {
      suggestions.push({
        text: filenameSuggestion,
        reasoning: 'Based on the image filename',
        confidence: 'medium'
      });
    }

    // Context-based suggestion
    const contextSuggestion = this.generateContextualAltText(element, context);
    if (contextSuggestion !== filenameSuggestion) {
      suggestions.push({
        text: contextSuggestion,
        reasoning: 'Based on surrounding content',
        confidence: 'high'
      });
    }

    // Generic but appropriate suggestions
    const elementContext = this.analyzeElementContext(element, context);
    if (elementContext.isInNavigation) {
      suggestions.push({
        text: 'Navigation icon',
        reasoning: 'Image appears to be in navigation area',
        confidence: 'medium'
      });
    }

    if (elementContext.isInArticle) {
      suggestions.push({
        text: 'Article illustration',
        reasoning: 'Image appears in article content',
        confidence: 'medium'
      });
    }

    return suggestions.slice(0, 3); // Limit to top 3 suggestions
  }

  private isImageElement(element: ParsedElement): boolean {
    const imageTags = ['img', 'Image', 'picture'];
    return imageTags.includes(element.tagName) ||
           (element.tagName === 'input' && element.attributes.get('type')?.value === 'image');
  }

  private isDecorativeImage(element: ParsedElement, context: DocumentContext): boolean {
    const src = String(element.attributes.get('src')?.value || '');
    const className = String(element.attributes.get('class')?.value || '') + 
                     String(element.attributes.get('className')?.value || '');
    
    // Check filename patterns
    if (this.decorativeImagePatterns.some(pattern => pattern.test(src))) {
      return true;
    }

    // Check CSS classes
    if (/\b(decoration|ornament|divider|spacer)\b/i.test(className)) {
      return true;
    }

    // Check if image is very small (likely decorative)
    const width = element.attributes.get('width')?.value;
    const height = element.attributes.get('height')?.value;
    
    if (width && height) {
      const w = Number(width);
      const h = Number(height);
      if ((w <= 10 && h <= 10) || w <= 1 || h <= 1) {
        return true;
      }
    }

    // Check context
    const parentRole = context.getParentElement(element)?.attributes.get('role')?.value;
    if (parentRole === 'presentation' || parentRole === 'decoration') {
      return true;
    }

    return false;
  }

  private isFilenameAsAltText(altText: string, element: ParsedElement): boolean {
    const src = String(element.attributes.get('src')?.value || '');
    const filename = src.split('/').pop()?.split('.')[0] || '';
    
    return altText.toLowerCase() === filename.toLowerCase() ||
           altText.toLowerCase() === filename.toLowerCase().replace(/[-_]/g, ' ');
  }

  private isGenericAltText(altText: string): boolean {
    return this.genericAltTextPatterns.some(pattern => pattern.test(altText));
  }

  private isLikelyDecorative(element: ParsedElement): boolean {
    const src = String(element.attributes.get('src')?.value || '');
    const className = String(element.attributes.get('class')?.value || '') + 
                     String(element.attributes.get('className')?.value || '');
    
    return this.decorativeImagePatterns.some(pattern => pattern.test(src + ' ' + className));
  }

  private analyzeElementContext(element: ParsedElement, context: DocumentContext): ElementContext {
    const parentElements = context.getParentChain(element);
    
    return {
      isInNavigation: parentElements.some(p => 
        p.tagName === 'nav' || 
        p.attributes.get('role')?.value === 'navigation'
      ),
      isInArticle: parentElements.some(p => 
        p.tagName === 'article' || 
        p.attributes.get('role')?.value === 'article'
      ),
      isInHeader: parentElements.some(p => 
        p.tagName === 'header' || 
        p.attributes.get('role')?.value === 'banner'
      ),
      isInFooter: parentElements.some(p => 
        p.tagName === 'footer' || 
        p.attributes.get('role')?.value === 'contentinfo'
      )
    };
  }
}

interface AltTextSuggestion {
  text: string;
  reasoning: string;
  confidence: 'low' | 'medium' | 'high';
}

interface ElementContext {
  isInNavigation: boolean;
  isInArticle: boolean;
  isInHeader: boolean;
  isInFooter: boolean;
}
```

#### F3.3: ARIA Labels Rule Implementation

**Description:** Implementation of WCAG 4.1.2 (Name, Role, Value) focusing on proper ARIA labeling for interactive elements.

```typescript
export class AriaLabelRule extends WCAGRule {
  readonly id = 'wcag-4.1.2-aria-label';
  readonly wcagCriterion = '4.1.2';
  readonly level = 'A' as const;
  readonly category = AccessibilityCategory.ARIA_LABELS;
  readonly description = 'Interactive elements must have accessible names';

  private readonly interactiveElements = [
    'button', 'a', 'input', 'select', 'textarea', 'details', 'summary'
  ];

  private readonly interactiveRoles = [
    'button', 'link', 'tab', 'tabpanel', 'menuitem', 'option', 'checkbox', 
    'radio', 'slider', 'spinbutton', 'textbox', 'combobox', 'searchbox'
  ];

  async analyze(element: ParsedElement, context: DocumentContext): Promise<RuleViolation[]> {
    if (!this.isInteractiveElement(element)) {
      return [];
    }

    const violations: RuleViolation[] = [];
    
    // Check for accessible name
    const accessibleName = this.getAccessibleName(element, context);
    
    if (!accessibleName.hasName) {
      violations.push(this.createViolation({
        messageKey: 'aria-label.missing-accessible-name',
        messageParams: { 
          elementType: this.getElementTypeDescription(element),
          tagName: element.tagName 
        },
        element,
        location: element.location,
        fixes: this.generateAccessibleNameFixes(element, context)
      }));
    } else if (accessibleName.isEmpty) {
      violations.push(this.createViolation({
        messageKey: 'aria-label.empty-accessible-name',
        element,
        location: element.location,
        fixes: this.generateNonEmptyNameFixes(element, context)
      }));
    } else if (accessibleName.isGeneric) {
      violations.push(this.createViolation({
        messageKey: 'aria-label.generic-accessible-name',
        messageParams: { name: accessibleName.computedName },
        element,
        location: element.location,
        fixes: this.generateDescriptiveNameFixes(element, context)
      }));
    }

    // Check for proper ARIA attributes
    const ariaViolations = this.checkAriaAttributes(element, context);
    violations.push(...ariaViolations);

    return violations;
  }

  private getAccessibleName(element: ParsedElement, context: DocumentContext): AccessibleNameResult {
    // ARIA name calculation according to spec
    // https://www.w3.org/TR/accname-1.1/
    
    // Step 1: aria-labelledby
    const labelledBy = element.attributes.get('aria-labelledby') || element.attributes.get('ariaLabelledBy');
    if (labelledBy && labelledBy.value) {
      const referencedName = this.getNameFromLabelledBy(String(labelledBy.value), context);
      return {
        hasName: referencedName.length > 0,
        isEmpty: referencedName.trim().length === 0,
        isGeneric: this.isGenericName(referencedName),
        computedName: referencedName,
        source: 'aria-labelledby'
      };
    }

    // Step 2: aria-label
    const ariaLabel = element.attributes.get('aria-label') || element.attributes.get('ariaLabel');
    if (ariaLabel && ariaLabel.value) {
      const labelText = String(ariaLabel.value).trim();
      return {
        hasName: labelText.length > 0,
        isEmpty: labelText.length === 0,
        isGeneric: this.isGenericName(labelText),
        computedName: labelText,
        source: 'aria-label'
      };
    }

    // Step 3: Native labeling (for form controls)
    if (this.isFormControl(element)) {
      const nativeLabel = this.getNativeLabel(element, context);
      if (nativeLabel) {
        return {
          hasName: nativeLabel.length > 0,
          isEmpty: nativeLabel.trim().length === 0,
          isGeneric: this.isGenericName(nativeLabel),
          computedName: nativeLabel,
          source: 'label'
        };
      }
    }

    // Step 4: Text content (for buttons, links, etc.)
    if (this.usesTextContent(element)) {
      const textContent = this.getTextContent(element, context);
      if (textContent) {
        return {
          hasName: textContent.length > 0,
          isEmpty: textContent.trim().length === 0,
          isGeneric: this.isGenericName(textContent),
          computedName: textContent,
          source: 'content'
        };
      }
    }

    // Step 5: title attribute (last resort)
    const title = element.attributes.get('title');
    if (title && title.value) {
      const titleText = String(title.value).trim();
      return {
        hasName: titleText.length > 0,
        isEmpty: titleText.length === 0,
        isGeneric: this.isGenericName(titleText),
        computedName: titleText,
        source: 'title'
      };
    }

    // No accessible name found
    return {
      hasName: false,
      isEmpty: true,
      isGeneric: false,
      computedName: '',
      source: null
    };
  }

  private generateAccessibleNameFixes(element: ParsedElement, context: DocumentContext): QuickFix[] {
    const fixes: QuickFix[] = [];
    const elementDescription = this.getElementTypeDescription(element);

    // Fix 1: Add aria-label
    const suggestedLabel = this.generateLabelSuggestion(element, context);
    fixes.push(new QuickFix({
      id: 'add-aria-label',
      title: `Add aria-label="${suggestedLabel}"`,
      description: `Add aria-label attribute to provide an accessible name for this ${elementDescription}`,
      category: 'aria-label',
      confidence: 'high',
      edit: new AttributeEdit({
        type: 'add-attribute',
        attribute: element.framework === 'react' ? 'aria-label' : 'aria-label',
        value: suggestedLabel
      }),
      userInput: {
        type: 'text',
        prompt: `What does this ${elementDescription} do?`,
        placeholder: suggestedLabel,
        validation: {
          minLength: 1,
          maxLength: 100,
          pattern: /^[^<>]*$/
        }
      }
    }));

    // Fix 2: Add text content (for buttons)
    if (element.tagName === 'button' && !element.children.textContent) {
      fixes.push(new QuickFix({
        id: 'add-button-text',
        title: 'Add button text',
        description: 'Add descriptive text content to the button',
        category: 'content',
        confidence: 'high',
        edit: new ContentEdit({
          type: 'add-text-content',
          content: suggestedLabel
        })
      }));
    }

    // Fix 3: Reference existing label (for form controls)
    if (this.isFormControl(element)) {
      const nearbyLabels = context.getNearbyLabels(element);
      nearbyLabels.forEach((label, index) => {
        fixes.push(new QuickFix({
          id: `reference-label-${index}`,
          title: `Use nearby label: "${label.text}"`,
          description: 'Reference the existing label element',
          category: 'label-reference',
          confidence: 'medium',
          edit: new MultiEdit([
            new AttributeEdit({
              type: 'add-attribute',
              attribute: 'id',
              value: this.generateUniqueId(element)
            }),
            new AttributeEdit({
              type: 'add-attribute',
              attribute: 'for',
              value: this.generateUniqueId(element),
              targetElement: label.element
            })
          ])
        }));
      });
    }

    return fixes;
  }

  private generateLabelSuggestion(element: ParsedElement, context: DocumentContext): string {
    // Generate contextual suggestions based on element type and context
    
    if (element.tagName === 'button') {
      const nearbyText = context.getNearbyTextContent(element, 50);
      if (nearbyText.includes('submit')) return 'Submit';
      if (nearbyText.includes('cancel')) return 'Cancel';
      if (nearbyText.includes('save')) return 'Save';
      if (nearbyText.includes('delete')) return 'Delete';
      if (nearbyText.includes('edit')) return 'Edit';
      return 'Describe button action';
    }

    if (element.tagName === 'a') {
      const href = element.attributes.get('href')?.value;
      if (href) {
        if (href.includes('mailto:')) return 'Send email';
        if (href.includes('tel:')) return 'Call phone number';
        if (href.includes('download')) return 'Download file';
      }
      return 'Describe link destination';
    }

    if (this.isFormControl(element)) {
      const type = element.attributes.get('type')?.value;
      const name = element.attributes.get('name')?.value;
      
      if (name) {
        return this.humanizeFieldName(String(name));
      }
      
      switch (type) {
        case 'email': return 'Email address';
        case 'password': return 'Password';
        case 'search': return 'Search';
        case 'tel': return 'Phone number';
        case 'url': return 'Website URL';
        default: return 'Enter value';
      }
    }

    return `Describe ${this.getElementTypeDescription(element)}`;
  }

  private humanizeFieldName(name: string): string {
    return name
      .replace(/[-_]/g, ' ')
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .toLowerCase()
      .replace(/\b\w/g, l => l.toUpperCase());
  }

  private isInteractiveElement(element: ParsedElement): boolean {
    // Check if element is interactive by tag name
    if (this.interactiveElements.includes(element.tagName.toLowerCase())) {
      return true;
    }

    // Check if element has interactive role
    const role = element.attributes.get('role')?.value;
    if (role && this.interactiveRoles.includes(String(role))) {
      return true;
    }

    // Check for event handlers (framework-specific)
    const interactiveProps = ['onClick', 'onPress', 'onTap', 'onclick'];
    return interactiveProps.some(prop => element.attributes.has(prop));
  }

  private isFormControl(element: ParsedElement): boolean {
    const formControls = ['input', 'select', 'textarea'];
    return formControls.includes(element.tagName.toLowerCase());
  }

  private getElementTypeDescription(element: ParsedElement): string {
    const role = element.attributes.get('role')?.value;
    if (role) {
      return String(role);
    }

    switch (element.tagName.toLowerCase()) {
      case 'button': return 'button';
      case 'a': return 'link';
      case 'input': 
        const type = element.attributes.get('type')?.value;
        return type ? `${type} input` : 'input field';
      case 'select': return 'dropdown';
      case 'textarea': return 'text area';
      default: return 'interactive element';
    }
  }
}

interface AccessibleNameResult {
  hasName: boolean;
  isEmpty: boolean;
  isGeneric: boolean;
  computedName: string;
  source: 'aria-labelledby' | 'aria-label' | 'label' | 'content' | 'title' | null;
}
```

#### F3.4: Header Hierarchy Rule Implementation

**Description:** Implementation of WCAG 1.3.1 (Info and Relationships) focusing on proper heading structure and hierarchy.

```typescript
export class HeaderHierarchyRule extends WCAGRule {
  readonly id = 'wcag-1.3.1-heading-hierarchy';
  readonly wcagCriterion = '1.3.1';
  readonly level = 'A' as const;
  readonly category = AccessibilityCategory.HEADER_HIERARCHY;
  readonly description = 'Headings must be used to convey document structure';

  private readonly headingTags = ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'];

  async analyze(element: ParsedElement, context: DocumentContext): Promise<RuleViolation[]> {
    // This rule analyzes the entire document structure
    return [];
  }

  async analyzeDocument(document: ParsedDocument, context: DocumentContext): Promise<RuleViolation[]> {
    const headings = this.extractHeadings(document, context);
    const violations: RuleViolation[] = [];

    // Check for missing h1
    const h1Count = headings.filter(h => h.level === 1).length;
    if (h1Count === 0) {
      violations.push(this.createDocumentViolation({
        messageKey: 'heading-hierarchy.missing-h1',
        fixes: this.generateAddH1Fixes(document, context)
      }));
    } else if (h1Count > 1) {
      violations.push(this.createDocumentViolation({
        messageKey: 'heading-hierarchy.multiple-h1',
        messageParams: { count: h1Count },
        fixes: this.generateFixMultipleH1Fixes(headings.filter(h => h.level === 1))
      }));
    }

    // Check hierarchy
    const hierarchyViolations = this.checkHierarchy(headings);
    violations.push(...hierarchyViolations);

    // Check empty headings
    const emptyHeadings = headings.filter(h => h.isEmpty);
    emptyHeadings.forEach(heading => {
      violations.push(this.createViolation({
        messageKey: 'heading-hierarchy.empty-heading',
        messageParams: { level: heading.level },
        element: heading.element,
        location: heading.element.location,
        fixes: this.generateEmptyHeadingFixes(heading)
      }));
    });

    // Check headings used for styling
    const stylingViolations = this.checkHeadingsUsedForStyling(headings, context);
    violations.push(...stylingViolations);

    return violations;
  }

  private extractHeadings(document: ParsedDocument, context: DocumentContext): DocumentHeading[] {
    const headings: DocumentHeading[] = [];

    document.elements.forEach(element => {
      if (this.isHeading(element)) {
        const level = this.getHeadingLevel(element);
        const textContent = this.getHeadingText(element, context);
        
        headings.push({
          element,
          level,
          textContent,
          isEmpty: textContent.trim().length === 0,
          location: element.location,
          context: this.getHeadingContext(element, context)
        });
      }
    });

    // Sort by document order
    return headings.sort((a, b) => {
      if (a.location.line !== b.location.line) {
        return a.location.line - b.location.line;
      }
      return a.location.column - b.location.column;
    });
  }

  private checkHierarchy(headings: DocumentHeading[]): RuleViolation[] {
    const violations: RuleViolation[] = [];
    let previousLevel = 0;

    headings.forEach((heading, index) => {
      const currentLevel = heading.level;
      
      // Check for level skipping
      if (currentLevel > previousLevel + 1) {
        violations.push(this.createViolation({
          messageKey: 'heading-hierarchy.skipped-level',
          messageParams: {
            currentLevel,
            previousLevel,
            expectedLevel: previousLevel + 1
          },
          element: heading.element,
          location: heading.location,
          fixes: this.generateHierarchyFixes(heading, previousLevel + 1)
        }));
      }

      // Check for improper nesting
      if (index > 0) {
        const context = this.analyzeHeadingContext(heading, headings.slice(0, index));
        if (context.hasStructuralIssues) {
          violations.push(this.createViolation({
            messageKey: 'heading-hierarchy.improper-nesting',
            messageParams: {
              headingText: heading.textContent,
              level: currentLevel
            },
            element: heading.element,
            location: heading.location,
            fixes: this.generateNestingFixes(heading, context)
          }));
        }
      }

      previousLevel = Math.max(previousLevel, currentLevel);
    });

    return violations;
  }

  private generateHierarchyFixes(heading: DocumentHeading, recommendedLevel: number): QuickFix[] {
    const fixes: QuickFix[] = [];
    const currentTag = heading.element.tagName.toLowerCase();
    const recommendedTag = `h${recommendedLevel}`;

    // Primary fix: Change to recommended level
    fixes.push(new QuickFix({
      id: 'fix-heading-level',
      title: `Change ${currentTag} to ${recommendedTag}`,
      description: `Adjust heading level to maintain proper hierarchy`,
      category: 'hierarchy',
      confidence: 'high',
      edit: new TagEdit({
        type: 'change-tag',
        from: currentTag,
        to: recommendedTag
      })
    }));

    // Alternative fixes for different levels
    for (let level = 1; level <= 6; level++) {
      if (level !== heading.level && level !== recommendedLevel) {
        const tag = `h${level}`;
        fixes.push(new QuickFix({
          id: `change-to-h${level}`,
          title: `Change to ${tag}`,
          description: `Change heading to level ${level}`,
          category: 'hierarchy',
          confidence: level < recommendedLevel ? 'medium' : 'low',
          edit: new TagEdit({
            type: 'change-tag',
            from: currentTag,
            to: tag
          })
        }));
      }
    }

    return fixes;
  }

  private checkHeadingsUsedForStyling(
    headings: DocumentHeading[], 
    context: DocumentContext
  ): RuleViolation[] {
    const violations: RuleViolation[] = [];

    headings.forEach(heading => {
      // Check if heading appears to be used for styling rather than structure
      const isStylingHeading = this.isUsedForStyling(heading, context);
      
      if (isStylingHeading) {
        violations.push(this.createViolation({
          messageKey: 'heading-hierarchy.styling-not-structure',
          messageParams: {
            level: heading.level,
            text: heading.textContent
          },
          element: heading.element,
          location: heading.location,
          fixes: this.generateStylingToStructureFixes(heading)
        }));
      }
    });

    return violations;
  }

  private isUsedForStyling(heading: DocumentHeading, context: DocumentContext): boolean {
    // Heuristics to detect headings used for styling
    
    // Very short headings that don't introduce sections
    if (heading.textContent.length < 3) {
      return true;
    }

    // Headings followed immediately by another heading of higher level
    const nextElement = context.getNextElement(heading.element);
    if (nextElement && this.isHeading(nextElement)) {
      const nextLevel = this.getHeadingLevel(nextElement);
      if (nextLevel <= heading.level) {
        return true;
      }
    }

    // Headings that are just emphasizing text (common styling patterns)
    const stylingPatterns = [
      /^(note|warning|tip|info|important)$/i,
      /^[★☆•▪▫◦‣⁃]+$/,
      /^\d+[\.\)]\s*$/
    ];

    return stylingPatterns.some(pattern => pattern.test(heading.textContent.trim()));
  }

  private generateStylingToStructureFixes(heading: DocumentHeading): QuickFix[] {
    const fixes: QuickFix[] = [];

    // Fix 1: Convert to styled paragraph
    fixes.push(new QuickFix({
      id: 'convert-to-paragraph',
      title: 'Convert to styled paragraph',
      description: 'Change this heading to a paragraph with appropriate styling',
      category: 'structure',
      confidence: 'high',
      edit: new TagEdit({
        type: 'change-tag',
        from: heading.element.tagName.toLowerCase(),
        to: 'p',
        attributes: {
          class: 'emphasized-text' // or appropriate styling class
        }
      })
    }));

    // Fix 2: Convert to strong text
    fixes.push(new QuickFix({
      id: 'convert-to-strong',
      title: 'Convert to emphasized text',
      description: 'Change to <strong> for emphasis without heading semantics',
      category: 'structure',
      confidence: 'medium',
      edit: new TagEdit({
        type: 'change-tag',
        from: heading.element.tagName.toLowerCase(),
        to: 'strong'
      })
    }));

    return fixes;
  }

  private isHeading(element: ParsedElement): boolean {
    return this.headingTags.includes(element.tagName.toLowerCase()) ||
           element.attributes.get('role')?.value === 'heading';
  }

  private getHeadingLevel(element: ParsedElement): number {
    const role = element.attributes.get('role')?.value;
    if (role === 'heading') {
      const ariaLevel = element.attributes.get('aria-level')?.value;
      return ariaLevel ? Number(ariaLevel) : 1;
    }

    const tagName = element.tagName.toLowerCase();
    const match = tagName.match(/^h([1-6])$/);
    return match ? Number(match[1]) : 1;
  }

  private getHeadingText(element: ParsedElement, context: DocumentContext): string {
    return context.getTextContent(element).trim();
  }
}

interface DocumentHeading {
  element: ParsedElement;
  level: number;
  textContent: string;
  isEmpty: boolean;
  location: ElementLocation;
  context: HeadingContext;
}

interface HeadingContext {
  hasStructuralIssues: boolean;
  isInNavigation: boolean;
  isInArticle: boolean;
  hasSubheadings: boolean;
}
```

## User Interface Components

### F4: Quick Fix Provider

**Description:** Advanced code action provider that offers intelligent, context-aware fixes for accessibility violations with preview capabilities and user input handling.

```typescript
export class AccessibilityCodeActionProvider implements vscode.CodeActionProvider {
  private readonly ruleEngine: WCAGRuleEngine;
  private readonly previewProvider: FixPreviewProvider;
  private readonly userInputHandler: UserInputHandler;

  constructor(
    ruleEngine: WCAGRuleEngine,
    private config: ConfigurationManager
  ) {
    this.ruleEngine = ruleEngine;
    this.previewProvider = new FixPreviewProvider();
    this.userInputHandler = new UserInputHandler();
  }

  async provideCodeActions(
    document: vscode.TextDocument,
    range: vscode.Range | vscode.Selection,
    context: vscode.CodeActionContext,
    token: vscode.CancellationToken
  ): Promise<vscode.CodeAction[]> {
    const actions: vscode.CodeAction[] = [];

    // Filter for accessibility diagnostics
    const accessibilityDiagnostics = context.diagnostics.filter(
      diag => diag.source === 'accessibility'
    );

    for (const diagnostic of accessibilityDiagnostics) {
      if (token.isCancellationRequested) {
        break;
      }

      const quickFixes = await this.generateQuickFixes(document, diagnostic, range);
      actions.push(...quickFixes);
    }

    // Add bulk fix actions if multiple diagnostics
    if (accessibilityDiagnostics.length > 1) {
      const bulkActions = await this.generateBulkActions(document, accessibilityDiagnostics);
      actions.push(...bulkActions);
    }

    return actions;
  }

  private async generateQuickFixes(
    document: vscode.TextDocument,
    diagnostic: vscode.Diagnostic,
    range: vscode.Range
  ): Promise<vscode.CodeAction[]> {
    const actions: vscode.CodeAction[] = [];
    
    // Get the rule that generated this diagnostic
    const ruleId = diagnostic.code as string;
    const rule = this.ruleEngine.getRule(ruleId);
    
    if (!rule) {
      return actions;
    }

    try {
      // Parse element at diagnostic location
      const element = await this.parseElementAtLocation(document, diagnostic.range);
      if (!element) {
        return actions;
      }

      // Generate fixes from the rule
      const fixes = await rule.generateFixes(new RuleViolation({
        ruleId,
        element,
        location: element.location,
        messageKey: this.extractMessageKey(diagnostic.message)
      }));

      // Convert fixes to VSCode code actions
      for (const fix of fixes) {
        const action = await this.createCodeAction(document, fix, diagnostic);
        if (action) {
          actions.push(action);
        }
      }

    } catch (error) {
      console.error(`Error generating quick fixes for ${ruleId}:`, error);
    }

    return actions;
  }

  private async createCodeAction(
    document: vscode.TextDocument,
    fix: QuickFix,
    diagnostic: vscode.Diagnostic
  ): Promise<vscode.CodeAction | null> {
    const action = new vscode.CodeAction(
      fix.title,
      vscode.CodeActionKind.QuickFix
    );

    action.diagnostics = [diagnostic];
    action.isPreferred = fix.confidence === 'high';

    // Handle user input requirements
    if (fix.userInput) {
      action.command = {
        command: 'accessibility.applyFixWithInput',
        title: fix.title,
        arguments: [document.uri, fix, diagnostic]
      };
    } else {
      // Apply fix directly
      action.edit = await this.createWorkspaceEdit(document, fix);
    }

    return action;
  }

  private async createWorkspaceEdit(
    document: vscode.TextDocument,
    fix: QuickFix
  ): Promise<vscode.WorkspaceEdit> {
    const edit = new vscode.WorkspaceEdit();

    switch (fix.edit.type) {
      case 'add-attribute':
      case 'modify-attribute':
        await this.applyAttributeEdit(edit, document, fix.edit as AttributeEdit);
        break;

      case 'change-tag':
        await this.applyTagEdit(edit, document, fix.edit as TagEdit);
        break;

      case 'add-text-content':
        await this.applyContentEdit(edit, document, fix.edit as ContentEdit);
        break;

      case 'multi-edit':
        await this.applyMultiEdit(edit, document, fix.edit as MultiEdit);
        break;

      default:
        console.warn(`Unknown edit type: ${fix.edit.type}`);
    }

    return edit;
  }

  private async applyAttributeEdit(
    edit: vscode.WorkspaceEdit,
    document: vscode.TextDocument,
    attributeEdit: AttributeEdit
  ): Promise<void> {
    const element = attributeEdit.targetElement || attributeEdit.element;
    const insertPosition = await this.findAttributeInsertPosition(document, element);
    
    const attributeText = this.formatAttribute(
      attributeEdit.attribute,
      attributeEdit.value,
      element.framework
    );

    if (attributeEdit.type === 'add-attribute') {
      edit.insert(document.uri, insertPosition, ` ${attributeText}`);
    } else if (attributeEdit.type === 'modify-attribute') {
      const existingRange = await this.findAttributeRange(document, element, attributeEdit.attribute);
      if (existingRange) {
        edit.replace(document.uri, existingRange, attributeText);
      } else {
        edit.insert(document.uri, insertPosition, ` ${attributeText}`);
      }
    }
  }

  private formatAttribute(attribute: string, value: string, framework: string): string {
    switch (framework) {
      case 'react':
        // Handle React-specific attribute formatting
        if (attribute.startsWith('aria-')) {
          return `${attribute}="${value}"`;
        }
        // Convert HTML attributes to React props
        const reactProp = this.convertToReactProp(attribute);
        if (this.needsBraces(value)) {
          return `${reactProp}={${value}}`;
        }
        return `${reactProp}="${value}"`;

      case 'vue':
        // Handle Vue.js attribute binding
        if (this.isDynamicValue(value)) {
          return `:${attribute}="${value}"`;
        }
        return `${attribute}="${value}"`;

      case 'angular':
        // Handle Angular property binding
        if (this.isDynamicValue(value)) {
          return `[${attribute}]="${value}"`;
        }
        return `${attribute}="${value}"`;

      default:
        return `${attribute}="${value}"`;
    }
  }

  private async generateBulkActions(
    document: vscode.TextDocument,
    diagnostics: vscode.Diagnostic[]
  ): Promise<vscode.CodeAction[]> {
    const actions: vscode.CodeAction[] = [];

    // Group diagnostics by rule type
    const diagnosticsByRule = new Map<string, vscode.Diagnostic[]>();
    diagnostics.forEach(diag => {
      const ruleId = diag.code as string;
      if (!diagnosticsByRule.has(ruleId)) {
        diagnosticsByRule.set(ruleId, []);
      }
      diagnosticsByRule.get(ruleId)!.push(diag);
    });

    // Create bulk fix actions
    for (const [ruleId, ruleDiagnostics] of diagnosticsByRule) {
      const rule = this.ruleEngine.getRule(ruleId);
      if (!rule || ruleDiagnostics.length < 2) {
        continue;
      }

      const bulkAction = new vscode.CodeAction(
        `Fix all ${rule.description.toLowerCase()} issues (${ruleDiagnostics.length})`,
        vscode.CodeActionKind.QuickFix
      );

      bulkAction.command = {
        command: 'accessibility.fixAllInFile',
        title: bulkAction.title,
        arguments: [document.uri, ruleId, ruleDiagnostics]
      };

      bulkAction.diagnostics = ruleDiagnostics;
      actions.push(bulkAction);
    }

    // Fix all accessibility issues
    if (diagnostics.length > 2) {
      const fixAllAction = new vscode.CodeAction(
        `Fix all accessibility issues (${diagnostics.length})`,
        vscode.CodeActionKind.QuickFix
      );

      fixAllAction.command = {
        command: 'accessibility.fixAllInFile',
        title: fixAllAction.title,
        arguments: [document.uri, 'all', diagnostics]
      };

      fixAllAction.diagnostics = diagnostics;
      actions.push(fixAllAction);
    }

    return actions;
  }
}

class UserInputHandler {
  async promptForInput(fix: QuickFix, document: vscode.TextDocument): Promise<string | null> {
    const input = fix.userInput!;
    
    switch (input.type) {
      case 'text':
        return await this.promptForText(input);
      
      case 'select':
        return await this.promptForSelection(input);
      
      case 'multiselect':
        return await this.promptForMultiSelection(input);
      
      default:
        return null;
    }
  }

  private async promptForText(input: TextInput): Promise<string | null> {
    const options: vscode.InputBoxOptions = {
      prompt: input.prompt,
      placeHolder: input.placeholder,
      validateInput: (value: string) => {
        if (input.validation) {
          if (value.length < (input.validation.minLength || 0)) {
            return `Minimum length is ${input.validation.minLength}`;
          }
          if (value.length > (input.validation.maxLength || 1000)) {
            return `Maximum length is ${input.validation.maxLength}`;
          }
          if (input.validation.pattern && !input.validation.pattern.test(value)) {
            return 'Invalid format';
          }
        }
        return undefined;
      }
    };

    return await vscode.window.showInputBox(options);
  }

  private async promptForSelection(input: SelectInput): Promise<string | null> {
    const selected = await vscode.window.showQuickPick(
      input.options.map(opt => ({
        label: opt.label,
        description: opt.description,
        value: opt.value
      })),
      {
        placeHolder: input.prompt
      }
    );

    return selected ? selected.value : null;
  }
}

class FixPreviewProvider {
  async showPreview(
    document: vscode.TextDocument,
    fix: QuickFix,
    element: ParsedElement
  ): Promise<boolean> {
    // Create preview content
    const previewContent = this.generatePreviewContent(document, fix, element);
    
    // Show preview in new editor
    const previewDoc = await vscode.workspace.openTextDocument({
      content: previewContent,
      language: document.languageId
    });

    await vscode.window.showTextDocument(previewDoc, {
      viewColumn: vscode.ViewColumn.Beside,
      preview: true
    });

    // Ask user to confirm
    const confirmed = await vscode.window.showInformationMessage(
      `Apply this accessibility fix?`,
      { modal: true },
      'Apply',
      'Cancel'
    );

    return confirmed === 'Apply';
  }

  private generatePreviewContent(
    document: vscode.TextDocument,
    fix: QuickFix,
    element: ParsedElement
  ): string {
    const lines = document.getText().split('\n');
    const elementLine = element.location.line - 1;
    
    // Apply the fix to generate preview
    const modifiedLines = [...lines];
    
    // Simple preview generation (would be more sophisticated in real implementation)
    switch (fix.edit.type) {
      case 'add-attribute':
      case 'modify-attribute':
        const attributeEdit = fix.edit as AttributeEdit;
        const currentLine = modifiedLines[elementLine];
        const attributeText = ` ${attributeEdit.attribute}="${attributeEdit.value}"`;
        
        // Find insertion point
        const tagEndIndex = currentLine.indexOf('>', element.location.column);
        if (tagEndIndex !== -1) {
          modifiedLines[elementLine] = 
            currentLine.slice(0, tagEndIndex) + 
            attributeText + 
            currentLine.slice(tagEndIndex);
        }
        break;
        
      // Add other edit types as needed
    }

    return modifiedLines.join('\n');
  }
}

// Enhanced data models
interface QuickFix {
  id: string;
  title: string;
  description: string;
  category: string;
  confidence: 'low' | 'medium' | 'high';
  edit: Edit;
  userInput?: UserInput;
  preview?: boolean;
}

interface Edit {
  type: 'add-attribute' | 'modify-attribute' | 'change-tag' | 'add-text-content' | 'multi-edit';
}

interface AttributeEdit extends Edit {
  type: 'add-attribute' | 'modify-attribute';
  attribute: string;
  value: string;
  targetElement?: ParsedElement;
}

interface TagEdit extends Edit {
  type: 'change-tag';
  from: string;
  to: string;
  preserveAttributes?: boolean;
  attributes?: Record<string, string>;
}

interface ContentEdit extends Edit {
  type: 'add-text-content';
  content: string;
  position?: 'start' | 'end' | 'replace';
}

interface MultiEdit extends Edit {
  type: 'multi-edit';
  edits: Edit[];
}

interface UserInput {
  type: 'text' | 'select' | 'multiselect';
}

interface TextInput extends UserInput {
  type: 'text';
  prompt: string;
  placeholder?: string;
  validation?: {
    minLength?: number;
    maxLength?: number;
    pattern?: RegExp;
  };
}

interface SelectInput extends UserInput {
  type: 'select';
  prompt: string;
  options: Array<{
    label: string;
    value: string;
    description?: string;
  }>;
}
```

## Configuration & Settings

### F5: Configuration Management System

**Description:** Comprehensive configuration system supporting team-wide settings, per-project customization, and integration with existing development tools.

```typescript
export class ConfigurationManager {
  private readonly configKeys = {
    enabled: 'accessibility.enabled',
    wcagLevel: 'accessibility.wcagLevel',
    rules: 'accessibility.rules',
    severity: 'accessibility.severity',
    frameworks: 'accessibility.frameworks',
    autoFix: 'accessibility.autoFix',
    showInlineHelp: 'accessibility.showInlineHelp',
    teamConfig: 'accessibility.teamConfig'
  };

  private teamConfig: TeamConfiguration | null = null;
  private watchers: vscode.Disposable[] = [];

  constructor(private context: vscode.ExtensionContext) {
    this.loadTeamConfiguration();
    this.setupConfigurationWatchers();
  }

  // Core configuration getters
  isEnabled(): boolean {
    return this.getConfigValue('enabled', true);
  }

  getWCAGLevel(): 'A' | 'AA' | 'AAA' {
    return this.getConfigValue('wcagLevel', 'AA');
  }

  isRuleEnabled(ruleId: string): boolean {
    const rules = this.getConfigValue('rules', {});
    const ruleName = this.extractRuleName(ruleId);
    
    // Check team configuration first
    if (this.teamConfig?.rules?.[ruleName] !== undefined) {
      return this.teamConfig.rules[ruleName];
    }
    
    return rules[ruleName] !== false;
  }

  getRuleSeverity(wcagLevel: 'A' | 'AA' | 'AAA'): vscode.DiagnosticSeverity {
    const severityConfig = this.getConfigValue('severity', {});
    const levelKey = `level${wcagLevel}`;
    
    // Check team configuration first
    const teamSeverity = this.teamConfig?.severity?.[levelKey];
    const userSeverity = severityConfig[levelKey];
    const severity = teamSeverity || userSeverity || 'warning';
    
    switch (severity) {
      case 'error': return vscode.DiagnosticSeverity.Error;
      case 'warning': return vscode.DiagnosticSeverity.Warning;
      case 'info': return vscode.DiagnosticSeverity.Information;
      default: return vscode.DiagnosticSeverity.Hint;
    }
  }

  getSupportedFrameworks(): string[] {
    const frameworks = this.getConfigValue('frameworks', ['react', 'angular', 'vue', 'html']);
    return this.teamConfig?.frameworks || frameworks;
  }

  getAutoFixSettings(): AutoFixConfiguration {
    const autoFix = this.getConfigValue('autoFix', {});
    
    return {
      onSave: this.teamConfig?.autoFix?.onSave ?? autoFix.onSave ?? false,
      onPaste: this.teamConfig?.autoFix?.onPaste ?? autoFix.onPaste ?? false,
      enabledRules: this.teamConfig?.autoFix?.enabledRules ?? autoFix.enabledRules ?? [],
      maxFixesPerSession: this.teamConfig?.autoFix?.maxFixesPerSession ?? autoFix.maxFixesPerSession ?? 10
    };
  }

  // Team configuration management
  private async loadTeamConfiguration(): Promise<void> {
    try {
      const workspaceFolders = vscode.workspace.workspaceFolders;
      if (!workspaceFolders) {
        return;
      }

      for (const folder of workspaceFolders) {
        const configPath = vscode.Uri.joinPath(folder.uri, '.accessibilityrc.json');
        
        try {
          const configData = await vscode.workspace.fs.readFile(configPath);
          const configText = Buffer.from(configData).toString('utf8');
          this.teamConfig = JSON.parse(configText);
          console.log('Loaded team accessibility configuration from', configPath.fsPath);
          break;
        } catch (error) {
          // Config file doesn't exist or is invalid, continue to next folder
        }
      }

      // Also check for ESLint integration
      await this.loadESLintIntegration();
      
    } catch (error) {
      console.error('Error loading team configuration:', error);
    }
  }

  private async loadESLintIntegration(): Promise<void> {
    try {
      const workspaceFolders = vscode.workspace.workspaceFolders;
      if (!workspaceFolders) {
        return;
      }

      for (const folder of workspaceFolders) {
        const eslintPath = vscode.Uri.joinPath(folder.uri, '.eslintrc.json');
        
        try {
          const eslintData = await vscode.workspace.fs.readFile(eslintPath);
          const eslintConfig = JSON.parse(Buffer.from(eslintData).toString('utf8'));
          
          // Extract accessibility-related ESLint rules
          const accessibilityRules = this.extractAccessibilityESLintRules(eslintConfig);
          
          if (Object.keys(accessibilityRules).length > 0) {
            this.integrateESLintRules(accessibilityRules);
          }
          
          break;
        } catch (error) {
          // ESLint config doesn't exist, continue
        }
      }
    } catch (error) {
      console.error('Error loading ESLint integration:', error);
    }
  }

  private extractAccessibilityESLintRules(eslintConfig: any): Record<string, boolean> {
    const accessibilityRules: Record<string, boolean> = {};
    const rules = eslintConfig.rules || {};
    
    // Map ESLint accessibility rules to our rule names
    const ruleMapping = {
      'jsx-a11y/alt-text': 'altText',
      'jsx-a11y/aria-props': 'ariaLabels',
      'jsx-a11y/heading-has-content': 'headerHierarchy',
      'jsx-a11y/aria-role': 'ariaLabels',
      'jsx-a11y/click-events-have-key-events': 'keyboardNavigation',
      'jsx-a11y/no-noninteractive-element-interactions': 'ariaLabels'
    };

    Object.entries(ruleMapping).forEach(([eslintRule, ourRule]) => {
      if (rules[eslintRule]) {
        const ruleConfig = rules[eslintRule];
        // ESLint rules can be 'error', 'warn', 'off', or [level, options]
        const isEnabled = Array.isArray(ruleConfig) 
          ? ruleConfig[0] !== 'off' 
          : ruleConfig !== 'off';
        
        accessibilityRules[ourRule] = isEnabled;
      }
    });

    return accessibilityRules;
  }

  private integrateESLintRules(eslintRules: Record<string, boolean>): void {
    if (!this.teamConfig) {
      this.teamConfig = { rules: {} };
    }

    if (!this.teamConfig.rules) {
      this.teamConfig.rules = {};
    }

    // Merge ESLint rules into team config
    Object.assign(this.teamConfig.rules, eslintRules);
  }

  // Configuration persistence
  async updateUserConfiguration<T>(key: string, value: T): Promise<void> {
    const config = vscode.workspace.getConfiguration();
    await config.update(key, value, vscode.ConfigurationTarget.Global);
  }

  async updateWorkspaceConfiguration<T>(key: string, value: T): Promise<void> {
    const config = vscode.workspace.getConfiguration();
    await config.update(key, value, vscode.ConfigurationTarget.Workspace);
  }

  async createTeamConfiguration(config: TeamConfiguration): Promise<void> {
    const workspaceFolders = vscode.workspace.workspaceFolders;
    if (!workspaceFolders) {
      throw new Error('No workspace folder open');
    }

    const configPath = vscode.Uri.joinPath(workspaceFolders[0].uri, '.accessibilityrc.json');
    const configText = JSON.stringify(config, null, 2);
    
    await vscode.workspace.fs.writeFile(configPath, Buffer.from(configText, 'utf8'));
    this.teamConfig = config;
  }

  // Configuration validation
  validateConfiguration(config: any): ConfigurationValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    // Validate WCAG level
    if (config.wcagLevel && !['A', 'AA', 'AAA'].includes(config.wcagLevel)) {
      errors.push(`Invalid WCAG level: ${config.wcagLevel}. Must be A, AA, or AAA.`);
    }

    // Validate rules configuration
    if (config.rules && typeof config.rules !== 'object') {
      errors.push('Rules configuration must be an object');
    }

    // Validate frameworks
    if (config.frameworks && !Array.isArray(config.frameworks)) {
      errors.push('Frameworks configuration must be an array');
    } else if (config.frameworks) {
      const validFrameworks = ['react', 'angular', 'vue', 'html'];
      const invalidFrameworks = config.frameworks.filter(f => !validFrameworks.includes(f));
      if (invalidFrameworks.length > 0) {
        warnings.push(`Unknown frameworks: ${invalidFrameworks.join(', ')}`);
      }
    }

    return {
      isValid: errors.length === 0,
      errors,
      warnings
    };
  }

  private setupConfigurationWatchers(): void {
    // Watch for configuration changes
    const configWatcher = vscode.workspace.onDidChangeConfiguration(event => {
      if (event.affectsConfiguration('accessibility')) {
        this.handleConfigurationChange(event);
      }
    });

    // Watch for team configuration file changes
    const fileWatcher = vscode.workspace.createFileSystemWatcher('**/.accessibilityrc.json');
    
    fileWatcher.onDidChange(() => this.loadTeamConfiguration());
    fileWatcher.onDidCreate(() => this.loadTeamConfiguration());
    fileWatcher.onDidDelete(() => {
      this.teamConfig = null;
    });

    this.watchers.push(configWatcher, fileWatcher);
  }

  private handleConfigurationChange(event: vscode.ConfigurationChangeEvent): void {
    // Notify extension components of configuration changes
    this.context.subscriptions.forEach(subscription => {
      if ('onConfigurationChanged' in subscription) {
        (subscription as any).onConfigurationChanged(event);
      }
    });
  }

  private getConfigValue<T>(key: string, defaultValue: T): T {
    const config = vscode.workspace.getConfiguration();
    return config.get(this.configKeys[key], defaultValue);
  }

  private extractRuleName(ruleId: string): string {
    // Convert "wcag-1.1.1-alt-text" to "altText"
    const parts = ruleId.split('-');
    if (parts.length < 3) return ruleId;
    
    const nameParts = parts.slice(2);
    return nameParts.map((part, index) => 
      index === 0 ? part : part.charAt(0).toUpperCase() + part.slice(1)
    ).join('');
  }

  dispose(): void {
    this.watchers.forEach(watcher => watcher.dispose());
  }
}

// Configuration interfaces
interface TeamConfiguration {
  wcagLevel?: 'A' | 'AA' | 'AAA';
  rules?: Record<string, boolean>;
  severity?: Record<string, 'error' | 'warning' | 'info' | 'hint'>;
  frameworks?: string[];
  autoFix?: AutoFixConfiguration;
  customRules?: CustomRuleConfiguration[];
  excludePatterns?: string[];
  includePatterns?: string[];
}

interface AutoFixConfiguration {
  onSave?: boolean;
  onPaste?: boolean;
  enabledRules?: string[];
  maxFixesPerSession?: number;
}

interface CustomRuleConfiguration {
  id: string;
  name: string;
  description: string;
  selector: string;
  message: string;
  severity: 'error' | 'warning' | 'info';
  autoFixable?: boolean;
}

interface ConfigurationValidationResult {
  isValid: boolean;
  errors: string[];
  warnings: string[];
}
```

### F6: Settings Schema

**Description:** Complete settings schema with validation, documentation, and IntelliSense support.

```json
{
  "contributes": {
    "configuration": {
      "title": "Accessibility Checker",
      "properties": {
        "accessibility.enabled": {
          "type": "boolean",
          "default": true,
          "description": "Enable or disable accessibility checking",
          "scope": "window"
        },
        "accessibility.wcagLevel": {
          "type": "string",
          "enum": ["A", "AA", "AAA"],
          "default": "AA",
          "description": "WCAG compliance level to check against",
          "enumDescriptions": [
            "Level A - Basic accessibility (minimum level)",
            "Level AA - Standard accessibility (recommended)",
            "Level AAA - Enhanced accessibility (highest level)"
          ],
          "scope": "window"
        },
        "accessibility.rules": {
          "type": "object",
          "description": "Enable or disable specific accessibility rules",
          "properties": {
            "altText": {
              "type": "boolean",
              "default": true,
              "description": "Check for missing or inadequate alt text on images"
            },
            "ariaLabels": {
              "type": "boolean", 
              "default": true,
              "description": "Check for missing ARIA labels on interactive elements"
            },
            "headerHierarchy": {
              "type": "boolean",
              "default": true,
              "description": "Check for proper heading hierarchy and structure"
            },
            "buttonLabels": {
              "type": "boolean",
              "default": true,
              "description": "Check for descriptive button labels"
            },
            "inputLabels": {
              "type": "boolean",
              "default": true,
              "description": "Check for properly labeled form inputs"
            },
            "titleMetadata": {
              "type": "boolean",
              "default": true,
              "description": "Check for page titles and metadata"
            },
            "colorContrast": {
              "type": "boolean",
              "default": false,
              "description": "Check for sufficient color contrast (experimental)"
            },
            "keyboardNavigation": {
              "type": "boolean",
              "default": false,
              "description": "Check for keyboard navigation support (experimental)"
            },
            "focusManagement": {
              "type": "boolean",
              "default": false,
              "description": "Check for proper focus management (experimental)"
            }
          },
          "additionalProperties": false,
          "scope": "window"
        },
        "accessibility.severity": {
          "type": "object",
          "description": "Configure diagnostic severity levels for different WCAG levels",
          "properties": {
            "levelA": {
              "type": "string",
              "enum": ["error", "warning", "info", "hint"],
              "default": "error",
              "description": "Severity for WCAG Level A violations"
            },
            "levelAA": {
              "type": "string", 
              "enum": ["error", "warning", "info", "hint"],
              "default": "warning",
              "description": "Severity for WCAG Level AA violations"
            },
            "levelAAA": {
              "type": "string",
              "enum": ["error", "warning", "info", "hint"], 
              "default": "info",
              "description": "Severity for WCAG Level AAA violations"
            }
          },
          "additionalProperties": false,
          "scope": "window"
        },
        "accessibility.frameworks": {
          "type": "array",
          "items": {
            "type": "string",
            "enum": ["react", "angular", "vue", "html"]
          },
          "default": ["react", "angular", "vue", "html"],
          "description": "Frameworks to analyze for accessibility issues",
          "scope": "window"
        },
        "accessibility.autoFix": {
          "type": "object",
          "description": "Automatic fix configuration",
          "properties": {
            "onSave": {
              "type": "boolean",
              "default": false,
              "description": "Automatically apply safe fixes when saving files"
            },
            "onPaste": {
              "type": "boolean",
              "default": false,
              "description": "Automatically apply fixes when pasting code"
            },
            "enabledRules": {
              "type": "array",
              "items": {
                "type": "string"
              },
              "default": ["altText", "ariaLabels"],
              "description": "Rules that are allowed to auto-fix"
            },
            "maxFixesPerSession": {
              "type": "number",
              "default": 10,
              "minimum": 1,
              "maximum": 100,
              "description": "Maximum number of auto-fixes to apply in a single session"
            }
          },
          "additionalProperties": false,
          "scope": "window"
        },
        "accessibility.showInlineHelp": {
          "type": "boolean",
          "default": true,
          "description": "Show inline help and suggestions for accessibility issues",
          "scope": "window"
        },
        "accessibility.excludePatterns": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": ["**/node_modules/**", "**/dist/**", "**/build/**"],
          "description": "File patterns to exclude from accessibility checking",
          "scope": "window"
        },
        "accessibility.includePatterns": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "default": ["**/*.{js,jsx,ts,tsx,html,vue}"],
          "description": "File patterns to include in accessibility checking",
          "scope": "window"
        },
        "accessibility.teamConfig": {
          "type": "string",
          "description": "Path to team accessibility configuration file",
          "scope": "window"
        },
        "accessibility.experimental": {
          "type": "object",
          "description": "Experimental features (may be unstable)",
          "properties": {
            "aiSuggestions": {
              "type": "boolean",
              "default": false,
              "description": "Use AI to generate accessibility suggestions"
            },
            "liveRegions": {
              "type": "boolean",
              "default": false,
              "description": "Check for proper live region usage"
            },
            "landmarkRoles": {
              "type": "boolean",
              "default": false,
              "description": "Check for semantic landmark roles"
            }
          },
          "additionalProperties": false,
          "scope": "window"
        }
      }
    }
  }
}
```

## Performance Requirements

### F7: Performance Optimization

**Description:** Comprehensive performance requirements and optimization strategies to ensure the extension doesn't impact developer productivity.

**Performance Targets:**

| Metric | Target | Measurement Method |
|--------|--------|--------------------|
| Extension Activation | < 2 seconds | Time from VSCode start to ready state |
| File Analysis (Small) | < 200ms | Files up to 1,000 lines |
| File Analysis (Medium) | < 500ms | Files up to 5,000 lines |
| File Analysis (Large) | < 1 second | Files up to 10,000 lines |
| Memory Usage | < 50MB | Additional RAM usage |
| CPU Usage (Active) | < 5% | During active typing |
| CPU Usage (Idle) | < 1% | When no files are being edited |

**Optimization Strategies:**

```typescript
class PerformanceOptimizer {
  private readonly analysisCache = new LRUCache<string, AnalysisResult>(1000);
  private readonly parseCache = new LRUCache<string, ParsedDocument>(100);
  private readonly debounceMap = new Map<string, NodeJS.Timeout>();

  // Intelligent caching strategy
  getCachedAnalysis(document: vscode.TextDocument): AnalysisResult | null {
    const cacheKey = this.generateCacheKey(document);
    const cached = this.analysisCache.get(cacheKey);
    
    if (cached && this.isCacheValid(cached, document)) {
      return cached;
    }
    
    return null;
  }

  setCachedAnalysis(document: vscode.TextDocument, result: AnalysisResult): void {
    const cacheKey = this.generateCacheKey(document);
    result.timestamp = Date.now();
    result.documentVersion = document.version;
    this.analysisCache.set(cacheKey, result);
  }

  // Incremental analysis for large files
  async analyzeIncrementally(
    document: vscode.TextDocument,
    changes: vscode.TextDocumentContentChangeEvent[]
  ): Promise<AnalysisResult> {
    const cached = this.getCachedAnalysis(document);
    
    if (cached && this.canUseIncrementalAnalysis(changes)) {
      return await this.updateAnalysisIncrementally(cached, changes);
    }
    
    return await this.analyzeCompletely(document);
  }

  private canUseIncrementalAnalysis(changes: vscode.TextDocumentContentChangeEvent[]): boolean {
    // Only use incremental analysis for small changes
    const totalChangedChars = changes.reduce((total, change) => 
      total + change.text.length + change.rangeLength, 0
    );
    
    return totalChangedChars < 100 && changes.length < 5;
  }

  // Worker thread for heavy parsing
  async parseInWorker(content: string, framework: string): Promise<ParsedDocument> {
    if (this.shouldUseWorker(content)) {
      return await this.parseInWorkerThread(content, framework);
    }
    
    return await this.parseInMainThread(content, framework);
  }

  private shouldUseWorker(content: string): boolean {
    return content.length > 10000; // Use worker for files > 10KB
  }

  // Debounced analysis to prevent excessive computation
  debounceAnalysis(
    document: vscode.TextDocument,
    analysisFunction: () => Promise<void>,
    delay: number = 300
  ): void {
    const key = document.uri.toString();
    
    // Clear existing timeout
    const existingTimeout = this.debounceMap.get(key);
    if (existingTimeout) {
      clearTimeout(existingTimeout);
    }
    
    // Set new timeout
    const timeout = setTimeout(async () => {
      await analysisFunction();
      this.debounceMap.delete(key);
    }, delay);
    
    this.debounceMap.set(key, timeout);
  }

  // Memory management
  cleanupCache(): void {
    const now = Date.now();
    const maxAge = 5 * 60 * 1000; // 5 minutes
    
    // Clean analysis cache
    for (const [key, result] of this.analysisCache.entries()) {
      if (now - result.timestamp > maxAge) {
        this.analysisCache.delete(key);
      }
    }
    
    // Clean parse cache
    for (const [key, doc] of this.parseCache.entries()) {
      if (now - doc.parseTime > maxAge) {
        this.parseCache.delete(key);
      }
    }
  }

  // Performance monitoring
  measurePerformance<T>(
    operation: string,
    fn: () => Promise<T>
  ): Promise<T> {
    const startTime = performance.now();
    
    return fn().then(result => {
      const duration = performance.now() - startTime;
      this.recordPerformanceMetric(operation, duration);
      return result;
    });
  }

  private recordPerformanceMetric(operation: string, duration: number): void {
    if (duration > 1000) {
      console.warn(`Slow operation: ${operation} took ${duration}ms`);
    }
    
    // Could integrate with telemetry service here
  }
}

class LRUCache<K, V> {
  private cache = new Map<K, V>();
  private maxSize: number;

  constructor(maxSize: number) {
    this.maxSize = maxSize;
  }

  get(key: K): V | undefined {
    const value = this.cache.get(key);
    if (value !== undefined) {
      // Move to end (most recently used)
      this.cache.delete(key);
      this.cache.set(key, value);
    }
    return value;
  }

  set(key: K, value: V): void {
    if (this.cache.has(key)) {
      this.cache.delete(key);
    } else if (this.cache.size >= this.maxSize) {
      // Remove least recently used (first item)
      const firstKey = this.cache.keys().next().value;
      this.cache.delete(firstKey);
    }
    
    this.cache.set(key, value);
  }

  delete(key: K): boolean {
    return this.cache.delete(key);
  }

  entries(): IterableIterator<[K, V]> {
    return this.cache.entries();
  }
}
```

## Error Handling & Edge Cases

### F8: Robust Error Handling

**Description:** Comprehensive error handling strategy to ensure the extension gracefully handles edge cases and provides helpful feedback to users.

```typescript
class ErrorHandler {
  private readonly errorReporter: ErrorReporter;
  private readonly recoveryStrategies: Map<string, RecoveryStrategy>;

  constructor() {
    this.errorReporter = new ErrorReporter();
    this.recoveryStrategies = new Map([
      ['ParseError', new ParseErrorRecovery()],
      ['TimeoutError', new TimeoutRecovery()],
      ['MemoryError', new MemoryErrorRecovery()],
      ['NetworkError', new NetworkErrorRecovery()]
    ]);
  }

  async handleError(error: Error, context: ErrorContext): Promise<ErrorHandlingResult> {
    try {
      // Log error with context
      this.errorReporter.reportError(error, context);

      // Attempt recovery
      const recovery = this.recoveryStrategies.get(error.constructor.name);
      if (recovery) {
        const result = await recovery.recover(error, context);
        if (result.success) {
          return result;
        }
      }

      // Graceful degradation
      return await this.gracefulDegradation(error, context);

    } catch (recoveryError) {
      // Last resort: safe fallback
      return this.safeFallback(error, recoveryError, context);
    }
  }

  private async gracefulDegradation(
    error: Error, 
    context: ErrorContext
  ): Promise<ErrorHandlingResult> {
    switch (context.operation) {
      case 'file-analysis':
        // Fall back to basic text analysis
        return {
          success: true,
          fallbackUsed: true,
          message: 'Using basic analysis due to parsing error',
          result: await this.performBasicAnalysis(context.document)
        };

      case 'quick-fix':
        // Disable auto-fixes, show manual suggestion
        return {
          success: true,
          fallbackUsed: true,
          message: 'Auto-fix unavailable, showing manual guidance',
          result: this.generateManualGuidance(context.violation)
        };

      case 'configuration':
        // Use default configuration
        return {
          success: true,
          fallbackUsed: true,
          message: 'Using default configuration',
          result: this.getDefaultConfiguration()
        };

      default:
        return this.safeFallback(error, null, context);
    }
  }

  private safeFallback(
    originalError: Error,
    recoveryError: Error | null,
    context: ErrorContext
  ): ErrorHandlingResult {
    // Disable problematic feature temporarily
    this.disableFeatureTemporarily(context.feature);

    // Show user-friendly error message
    const userMessage = this.createUserFriendlyMessage(originalError, context);
    vscode.window.showWarningMessage(userMessage);

    return {
      success: false,
      fallbackUsed: true,
      message: 'Feature temporarily disabled due to error',
      error: originalError
    };
  }
}

// Specific error types
class ParseError extends Error {
  constructor(
    message: string,
    public readonly fileName: string,
    public readonly line?: number,
    public readonly column?: number,
    public readonly cause?: Error
  ) {
    super(message);
    this.name = 'ParseError';
  }
}

class AccessibilityAnalysisError extends Error {
  constructor(
    message: string,
    public readonly ruleId: string,
    public readonly element?: ParsedElement,
    public readonly cause?: Error
  ) {
    super(message);
    this.name = 'AccessibilityAnalysisError';
  }
}

class ConfigurationError extends Error {
  constructor(
    message: string,
    public readonly configPath?: string,
    public readonly invalidValue?: any,
    public readonly cause?: Error
  ) {
    super(message);
    this.name = 'ConfigurationError';
  }
}

// Recovery strategies
class ParseErrorRecovery implements RecoveryStrategy {
  async recover(error: ParseError, context: ErrorContext): Promise<ErrorHandlingResult> {
    // Try alternative parsers
    if (context.document && context.framework) {
      const alternativeParsers = this.getAlternativeParsers(context.framework);
      
      for (const parser of alternativeParsers) {
        try {
          const result = await parser.parse(context.document.getText(), context.document.fileName);
          return {
            success: true,
            fallbackUsed: true,
            message: `Recovered using ${parser.name}`,
            result
          };
        } catch (altError) {
          // Continue to next parser
        }
      }
    }

    // Try parsing with error tolerance
    try {
      const tolerantResult = await this.parseWithErrorTolerance(context.document);
      return {
        success: true,
        fallbackUsed: true,
        message: 'Recovered with error-tolerant parsing',
        result: tolerantResult
      };
    } catch (tolerantError) {
      return { success: false, error: tolerantError };
    }
  }

  private async parseWithErrorTolerance(document: vscode.TextDocument): Promise<ParsedDocument> {
    // Implementation that ignores syntax errors and extracts what it can
    const content = document.getText();
    const elements: ParsedElement[] = [];

    // Use regex-based extraction as fallback
    const imgMatches = content.matchAll(/<img[^>]*>/gi);
    for (const match of imgMatches) {
      try {
        const element = this.parseElementFromRegex(match[0], 'img', match.index!);
        elements.push(element);
      } catch (e) {
        // Skip invalid elements
      }
    }

    // Similar for other important elements...

    return new ParsedDocument({
      content,
      fileName: document.fileName,
      framework: 'unknown',
      elements,
      parseMode: 'error-tolerant'
    });
  }
}

// Edge case handlers
class EdgeCaseHandler {
  handleMinifiedCode(document: vscode.TextDocument): boolean {
    const content = document.getText();
    
    // Detect minified code
    const avgLineLength = content.length / content.split('\n').length;
    const hasVeryLongLines = content.split('\n').some(line => line.length > 500);
    
    if (avgLineLength > 200 || hasVeryLongLines) {
      vscode.window.showInformationMessage(
        'Accessibility checking disabled for minified files',
        'Learn More'
      ).then(selection => {
        if (selection === 'Learn More') {
          vscode.env.openExternal(vscode.Uri.parse(
            'https://docs.example.com/accessibility-extension/minified-files'
          ));
        }
      });
      
      return false; // Don't analyze minified files
    }
    
    return true;
  }

  handleLargeFiles(document: vscode.TextDocument): AnalysisStrategy {
    const content = document.getText();
    const sizeInMB = content.length / (1024 * 1024);
    
    if (sizeInMB > 5) {
      // Use sampling strategy for very large files
      return {
        type: 'sampling',
        sampleRate: 0.1, // Analyze 10% of elements
        prioritizeInteractiveElements: true
      };
    } else if (sizeInMB > 1) {
      // Use incremental analysis
      return {
        type: 'incremental',
        chunkSize: 1000 // Analyze 1000 lines at a time
      };
    }
    
    return { type: 'full' };
  }

  handleDynamicContent(element: ParsedElement): AccessibilityAdvice {
    // Handle JSX expressions, template literals, etc.
    const dynamicAttributes = this.findDynamicAttributes(element);
    
    if (dynamicAttributes.length > 0) {
      return {
        type: 'dynamic-content-warning',
        message: 'This element has dynamic content that cannot be fully analyzed statically',
        suggestions: [
          'Ensure dynamic alt text is meaningful',
          'Test with actual data values',
          'Consider using aria-live regions for changing content'
        ],
        testingRequired: true
      };
    }
    
    return { type: 'none' };
  }

  handleFrameworkSpecificIssues(framework: string, element: ParsedElement): void {
    switch (framework) {
      case 'react':
        this.handleReactSpecifics(element);
        break;
      case 'vue':
        this.handleVueSpecifics(element);
        break;
      case 'angular':
        this.handleAngularSpecifics(element);
        break;
    }
  }

  private handleReactSpecifics(element: ParsedElement): void {
    // Handle React-specific patterns
    if (element.tagName === 'Fragment' || element.tagName === '') {
      // React Fragments don't affect accessibility
      return;
    }

    // Check for styled-components or emotion
    const hasStyledComponents = element.attributes.has('css') || 
                               element.tagName.match(/^styled\./);
    
    if (hasStyledComponents) {
      // Provide CSS-in-JS specific guidance
    }
  }

  private handleVueSpecifics(element: ParsedElement): void {
    // Handle Vue-specific directives
    const vueDirectives = ['v-if', 'v-show', 'v-for', 'v-model'];
    const hasDirectives = vueDirectives.some(directive => 
      element.attributes.has(directive)
    );

    if (hasDirectives) {
      // Provide Vue directive-specific accessibility guidance
    }
  }
}

interface ErrorContext {
  operation: string;
  feature: string;
  document?: vscode.TextDocument;
  framework?: string;
  violation?: RuleViolation;
  userAction?: string;
}

interface ErrorHandlingResult {
  success: boolean;
  fallbackUsed?: boolean;
  message?: string;
  result?: any;
  error?: Error;
}

interface RecoveryStrategy {
  recover(error: Error, context: ErrorContext): Promise<ErrorHandlingResult>;
}

interface AnalysisStrategy {
  type: 'full' | 'incremental' | 'sampling';
  sampleRate?: number;
  chunkSize?: number;
  prioritizeInteractiveElements?: boolean;
}

interface AccessibilityAdvice {
  type: string;
  message?: string;
  suggestions?: string[];
  testingRequired?: boolean;
}
```

## Testing Strategy

### F9: Comprehensive Testing Framework

**Description:** Multi-layer testing strategy ensuring reliability, performance, and usability of the accessibility extension.

```typescript
// Unit Test Structure
describe('AltTextRule', () => {
  let rule: AltTextRule;
  let mockConfig: MockConfigurationManager;

  beforeEach(() => {
    mockConfig = new MockConfigurationManager();
    rule = new AltTextRule(mockConfig);
  });

  describe('Missing Alt Text Detection', () => {
    it('should detect missing alt attribute on img elements', async () => {
      // Arrange
      const element = createMockElement({
        tagName: 'img',
        attributes: new Map([['src', 'logo.png']]),
        framework: 'react'
      });

      // Act
      const violations = await rule.analyze(element, createMockContext());

      // Assert
      expect(violations).toHaveLength(1);
      expect(violations[0].ruleId).toBe('wcag-1.1.1-alt-text');
      expect(violations[0].level).toBe('error');
    });

    it('should not flag decorative images with empty alt', async () => {
      // Arrange
      const element = createMockElement({
        tagName: 'img',
        attributes: new Map([
          ['src', 'decoration.png'],
          ['alt', '']
        ])
      });

      // Act
      const violations = await rule.analyze(element, createMockContext());

      // Assert
      expect(violations).toHaveLength(0);
    });

    it('should handle React JSX expressions', async () => {
      // Arrange
      const element = createMockElement({
        tagName: 'img',
        attributes: new Map([
          ['src', '{logoUrl}'],
          ['alt', '{logoAlt}']
        ]),
        framework: 'react'
      });

      // Act
      const violations = await rule.analyze(element, createMockContext());

      // Assert
      expect(violations).toHaveLength(0); // Dynamic content assumed valid
    });

    describe('Quick Fix Generation', () => {
      it('should generate appropriate fixes for missing alt', async () => {
        // Arrange
        const element = createMockElement({
          tagName: 'img',
          attributes: new Map([['src', 'company-logo.png']])
        });

        // Act
        const fixes = await rule.generateFixes(createMockViolation(element));

        // Assert
        expect(fixes).toHaveLength(2);
        expect(fixes[0].id).toBe('add-empty-alt');
        expect(fixes[1].id).toBe('add-descriptive-alt');
        expect(fixes[1].edit.value).toContain('company logo');
      });

      it('should suggest contextual alt text', async () => {
        // Arrange
        const element = createMockElement({
          tagName: 'img',
          attributes: new Map([['src', 'profile-photo.jpg']])
        });
        
        const context = createMockContext({
          nearbyText: 'About John Smith'
        });

        // Act
        const fixes = await rule.generateFixes(createMockViolation(element, context));

        // Assert
        const descriptiveFix = fixes.find(f => f.category === 'descriptive');
        expect(descriptiveFix?.edit.value).toMatch(/profile/i);
      });
    });
  });

  describe('Framework-Specific Handling', () => {
    it.each([
      ['react', '<img src={url} alt={altText} />'],
      ['vue', '<img :src="url" :alt="altText" />'],
      ['angular', '<img [src]="url" [alt]="altText" />'],
      ['html', '<img src="logo.png" alt="Logo" />']
    ])('should handle %s syntax correctly', async (framework, elementHtml) => {
      // Framework-specific test cases
      const element = parseElementFromHtml(elementHtml, framework);
      const violations = await rule.analyze(element, createMockContext());
      
      // Should not throw errors for valid framework syntax
      expect(() => violations).not.toThrow();
    });
  });

  describe('Edge Cases', () => {
    it('should handle malformed HTML gracefully', async () => {
      const element = createMockElement({
        tagName: 'img',
        attributes: new Map([['src', '<broken>']]),
        malformed: true
      });

      const violations = await rule.analyze(element, createMockContext());
      
      // Should not crash
      expect(violations).toBeDefined();
    });

    it('should handle very long alt text', async () => {
      const longAlt = 'A'.repeat(200);
      const element = createMockElement({
        tagName: 'img',
        attributes: new Map([
          ['src', 'image.png'],
          ['alt', longAlt]
        ])
      });

      const violations = await rule.analyze(element, createMockContext());
      
      expect(violations.some(v => v.messageKey === 'alt-text.too-long')).toBe(true);
    });
  });
});

// Integration Tests
describe('Extension Integration', () => {
  let extension: vscode.Extension<any>;
  let testDocument: vscode.TextDocument;

  beforeEach(async () => {
    extension = vscode.extensions.getExtension('publisher.accessibility-extension');
    await extension?.activate();
  });

  describe('Document Analysis', () => {
    it('should provide diagnostics for React files', async () => {
      // Create test document
      testDocument = await vscode.workspace.openTextDocument({
        content: `
          import React from 'react';
          
          function App() {
            return (
              <div>
                <img src="logo.png" /> {/* Missing alt */}
                <button></button> {/* Empty button */}
              </div>
            );
          }
        `,
        language: 'typescriptreact'
      });

      await vscode.window.showTextDocument(testDocument);
      
      // Wait for analysis
      await waitForDiagnostics(testDocument.uri, 2000);
      
      const diagnostics = vscode.languages.getDiagnostics(testDocument.uri);
      const accessibilityDiagnostics = diagnostics.filter(d => d.source === 'accessibility');
      
      expect(accessibilityDiagnostics.length).toBeGreaterThan(0);
      
      const altTextDiag = accessibilityDiagnostics.find(d => d.code === 'wcag-1.1.1-alt-text');
      expect(altTextDiag).toBeDefined();
    });

    it('should provide code actions for violations', async () => {
      const actions = await vscode.commands.executeCommand<vscode.CodeAction[]>(
        'vscode.executeCodeActionProvider',
        testDocument.uri,
        new vscode.Range(5, 16, 5, 35) // img element range
      );

      const accessibilityActions = actions?.filter(action => 
        action.kind?.value === vscode.CodeActionKind.QuickFix.value &&
        action.diagnostics?.some(d => d.source === 'accessibility')
      );

      expect(accessibilityActions?.length).toBeGreaterThan(0);
    });
  });

  describe('Configuration', () => {
    it('should respect rule enablement settings', async () => {
      // Disable alt text rule
      await vscode.workspace.getConfiguration('accessibility').update(
        'rules.altText',
        false,
        vscode.ConfigurationTarget.Workspace
      );

      // Trigger analysis
      await vscode.commands.executeCommand('accessibility.analyzeDocument');
      
      const diagnostics = vscode.languages.getDiagnostics(testDocument.uri);
      const altTextDiagnostics = diagnostics.filter(d => d.code === 'wcag-1.1.1-alt-text');
      
      expect(altTextDiagnostics.length).toBe(0);
    });
  });
});

// Performance Tests
describe('Performance', () => {
  describe('Analysis Speed', () => {
    it('should analyze small files quickly', async () => {
      const smallFile = createTestDocument('small', 100); // 100 lines
      
      const startTime = performance.now();
      await analyzeDocument(smallFile);
      const duration = performance.now() - startTime;
      
      expect(duration).toBeLessThan(200); // < 200ms
    });

    it('should handle large files within time limit', async () => {
      const largeFile = createTestDocument('large', 5000); // 5000 lines
      
      const startTime = performance.now();
      await analyzeDocument(largeFile);
      const duration = performance.now() - startTime;
      
      expect(duration).toBeLessThan(1000); // < 1 second
    });

    it('should use caching effectively', async () => {
      const testFile = createTestDocument('cache-test', 1000);
      
      // First analysis
      const firstStart = performance.now();
      await analyzeDocument(testFile);
      const firstDuration = performance.now() - firstStart;
      
      // Second analysis (should use cache)
      const secondStart = performance.now();
      await analyzeDocument(testFile);
      const secondDuration = performance.now() - secondStart;
      
      expect(secondDuration).toBeLessThan(firstDuration * 0.1); // 90% faster
    });
  });

  describe('Memory Usage', () => {
    it('should not leak memory with repeated analysis', async () => {
      const initialMemory = process.memoryUsage().heapUsed;
      
      // Analyze 100 different files
      for (let i = 0; i < 100; i++) {
        const testFile = createTestDocument(`test-${i}`, 500);
        await analyzeDocument(testFile);
      }
      
      // Force garbage collection
      if (global.gc) {
        global.gc();
      }
      
      const finalMemory = process.memoryUsage().heapUsed;
      const memoryIncrease = finalMemory - initialMemory;
      
      // Should not increase by more than 50MB
      expect(memoryIncrease).toBeLessThan(50 * 1024 * 1024);
    });
  });
});

// User Experience Tests
describe('User Experience', () => {
  describe('Error Messages', () => {
    it('should provide helpful error messages', async () => {
      const malformedFile = await vscode.workspace.openTextDocument({
        content: '<img src="logo.png" <-- malformed',
        language: 'html'
      });

      await vscode.window.showTextDocument(malformedFile);
      await waitForDiagnostics(malformedFile.uri, 2000);
      
      const diagnostics = vscode.languages.getDiagnostics(malformedFile.uri);
      
      // Should not crash, should provide helpful message
      expect(diagnostics.length).toBeGreaterThanOrEqual(0);
    });
  });

  describe('Quick Fix UX', () => {
    it('should apply fixes correctly', async () => {
      const testDoc = await vscode.workspace.openTextDocument({
        content: '<img src="logo.png" />',
        language: 'html'
      });

      const editor = await vscode.window.showTextDocument(testDoc);
      
      // Apply alt text fix
      const fixes = await vscode.commands.executeCommand<vscode.CodeAction[]>(
        'vscode.executeCodeActionProvider',
        testDoc.uri,
        new vscode.Range(0, 0, 0, 22)
      );

      const altFix = fixes?.find(f => f.title.includes('alt'));
      if (altFix?.edit) {
        await vscode.workspace.applyEdit(altFix.edit);
      }

      const updatedContent = testDoc.getText();
      expect(updatedContent).toContain('alt=');
    });
  });
});

// Test Utilities
function createMockElement(config: Partial<ParsedElement>): ParsedElement {
  return {
    tagName: 'div',
    attributes: new Map(),
    children: { textContent: '', childElements: [], hasInteractiveChildren: false },
    location: { line: 1, column: 0, length: 10 },
    framework: 'html',
    elementType: 'dom',
    accessibilityInfo: {
      isInteractive: false,
      hasAccessibleName: false,
      ariaAttributes: new Map(),
      semanticRole: null,
      focusable: false
    },
    ...config
  } as ParsedElement;
}

function createMockContext(config?: Partial<DocumentContext>): DocumentContext {
  return {
    document: {} as ParsedDocument,
    getNearbyTextContent: () => config?.nearbyText || '',
    getParentElement: () => null,
    getNearbyHeadings: () => [],
    ...config
  } as DocumentContext;
}

function createMockViolation(element: ParsedElement, context?: DocumentContext): RuleViolation {
  return {
    ruleId: 'wcag-1.1.1-alt-text',
    element,
    location: element.location,
    messageKey: 'alt-text.missing',
    context
  } as RuleViolation;
}

async function waitForDiagnostics(uri: vscode.Uri, timeout: number): Promise<void> {
  return new Promise((resolve) => {
    const interval = setInterval(() => {
      const diagnostics = vscode.languages.getDiagnostics(uri);
      if (diagnostics.length > 0) {
        clearInterval(interval);
        resolve();
      }
    }, 100);

    setTimeout(() => {
      clearInterval(interval);
      resolve();
    }, timeout);
  });
}

function createTestDocument(name: string, lines: number): vscode.TextDocument {
  const content = Array(lines).fill(0).map((_, i) => 
    `<div>Line ${i} content <img src="image${i}.png" /></div>`
  ).join('\n');

  return vscode.workspace.openTextDocument({
    content,
    language: 'html'
  });
}
```

## Implementation Plan

### F10: Phased Development Strategy

**Description:** Detailed implementation roadmap with milestones, deliverables, and success criteria.

**Phase 1: Foundation (Weeks 1-4)**

*Goal: Establish core architecture and basic React support*

**Week 1: Project Setup & Architecture**
- [ ] Set up TypeScript project with VSCode extension boilerplate
- [ ] Implement core interfaces and abstract classes
- [ ] Set up testing framework (Jest + VSCode test runner)
- [ ] Create CI/CD pipeline with GitHub Actions
- [ ] **Deliverable**: Working extension skeleton with activation

**Week 2: React Parser & Basic Rule Engine**
- [ ] Implement React/JSX parser using Babel
- [ ] Create basic WCAG rule engine architecture
- [ ] Implement Alt Text rule (complete)
- [ ] Add diagnostic provider integration
- [ ] **Deliverable**: Extension detects missing alt text in React files

**Week 3: Quick Fix Provider**
- [ ] Implement code action provider
- [ ] Create attribute edit functionality
- [ ] Add user input handling for custom alt text
- [ ] Implement fix preview system
- [ ] **Deliverable**: One-click alt text fixes working

**Week 4: Configuration & Testing**
- [ ] Implement configuration manager
- [ ] Add team configuration support
- [ ] Write comprehensive unit tests
- [ ] Performance optimization and caching
- [ ] **Deliverable**: Configurable, tested alt text checking

**Success Criteria:**
- ✅ Extension activates in <2 seconds
- ✅ Detects missing alt text in React files accurately
- ✅ Provides intelligent fix suggestions
- ✅ 90%+ code coverage for implemented features
- ✅ Performance targets met for files up to 5,000 lines

**Phase 2: Core Rules (Weeks 5-8)**

*Goal: Implement all essential WCAG rules*

**Week 5: ARIA Labels Rule**
- [ ] Implement interactive element detection
- [ ] Add accessible name calculation
- [ ] Create ARIA label fix suggestions
- [ ] Handle framework-specific ARIA patterns
- [ ] **Deliverable**: Complete ARIA labeling support

**Week 6: Header Hierarchy Rule**
- [ ] Implement document-level analysis
- [ ] Add heading structure validation
- [ ] Create hierarchy fix suggestions
- [ ] Handle semantic vs. styling heading detection
- [ ] **Deliverable**: Heading hierarchy checking

**Week 7: Form Labels Rule**
- [ ] Implement form control detection
- [ ] Add label association checking
- [ ] Create label generation and association fixes
- [ ] Handle different labeling methods
- [ ] **Deliverable**: Form accessibility checking

**Week 8: Button Labels & Polish**
- [ ] Implement button labeling rules
- [ ] Add bulk fix operations
- [ ] Improve error handling and edge cases
- [ ] Performance optimization round 2
- [ ] **Deliverable**: Complete core rule set

**Success Criteria:**
- ✅ All 6 core rules implemented and tested
- ✅ Bulk operations working efficiently
- ✅ Handles 95% of common accessibility issues
- ✅ User satisfaction rating >4.0 in beta testing

**Phase 3: Multi-Framework Support (Weeks 9-12)**

*Goal: Add Angular and Vue.js support*

**Week 9: Angular Parser**
- [ ] Implement Angular template parser
- [ ] Handle Angular-specific syntax (property binding, directives)
- [ ] Adapt existing rules for Angular
- [ ] Add Angular-specific quick fixes
- [ ] **Deliverable**: Full Angular support

**Week 10: Vue.js Parser**
- [ ] Implement Vue SFC parser
- [ ] Handle Vue template syntax and directives
- [ ] Adapt rules for Vue.js patterns
- [ ] Add Vue-specific quick fixes
- [ ] **Deliverable**: Full Vue.js support

**Week 11: Framework Integration**
- [ ] Improve framework detection
- [ ] Add framework-specific configuration
- [ ] Create framework-specific documentation
- [ ] Cross-framework testing
- [ ] **Deliverable**: Seamless multi-framework experience

**Week 12: Advanced Features**
- [ ] Add title/metadata rules
- [ ] Implement experimental rules (color contrast)
- [ ] Add telemetry and usage analytics
- [ ] Final performance optimization
- [ ] **Deliverable**: Feature-complete extension

**Success Criteria:**
- ✅ React, Angular, Vue, and HTML fully supported
- ✅ Framework-specific fixes working correctly
- ✅ Performance maintained across all frameworks
- ✅ Documentation complete for all frameworks

**Phase 4: Polish & Release (Weeks 13-16)**

*Goal: Prepare for marketplace release*

**Week 13: User Experience**
- [ ] Implement advanced quick fix features
- [ ] Add comprehensive help documentation
- [ ] Create tutorial and getting started guide
- [ ] Improve error messages and user feedback
- [ ] **Deliverable**: Production-ready UX

**Week 14: Integration & Compatibility**
- [ ] ESLint integration
- [ ] Prettier compatibility
- [ ] Other extension compatibility testing
- [ ] Workspace and multi-root support
- [ ] **Deliverable**: Enterprise-ready extension

**Week 15: Testing & Validation**
- [ ] Beta testing with select users
- [ ] Performance testing at scale
- [ ] Accessibility testing of the extension itself
- [ ] Security review and compliance
- [ ] **Deliverable**: Tested, secure extension

**Week 16: Release Preparation**
- [ ] Marketplace listing and assets
- [ ] Release notes and changelog
- [ ] Marketing materials
- [ ] Launch preparation
- [ ] **Deliverable**: Published extension

**Success Criteria:**
- ✅ Extension published to VSCode marketplace
- ✅ Documentation and tutorials complete
- ✅ Initial user adoption >1000 installs in first month
- ✅ User rating >4.5 stars
- ✅ Zero critical bugs reported

**Risk Mitigation:**

| Risk | Impact | Probability | Mitigation |
|------|---------|-------------|------------|
| Performance issues with large files | High | Medium | Implement incremental analysis and caching early |
| Framework parser complexity | Medium | High | Start with regex fallbacks, improve incrementally |
| VSCode API changes | Medium | Low | Use stable APIs, monitor beta releases |
| User adoption challenges | High | Medium | Focus on developer experience, get early feedback |
| Competition from existing tools | Medium | Medium | Differentiate with better UX and intelligence |

**Success Metrics:**

| Metric | Target | Measurement |
|--------|--------|-------------|
| Extension activation time | <2 seconds | Automated testing |
| Analysis speed (1000 lines) | <200ms | Performance benchmarks |
| Memory usage | <50MB additional | Memory profiling |
| Rule accuracy | >95% | Manual testing and user feedback |
| User satisfaction | >4.5 stars | Marketplace reviews |
| Active users (Month 3) | >5000 | Telemetry data |

## API Reference

### F11: Complete API Documentation

**Description:** Comprehensive API reference for all public interfaces, classes, and methods.

```typescript
/**
 * Main extension API namespace
 */
declare namespace AccessibilityExtension {
  
  /**
   * Core extension interface
   */
  interface IAccessibilityExtension {
    /**
     * Analyze a document for accessibility issues
     * @param document - VSCode text document to analyze
     * @returns Promise resolving to analysis results
     */
    analyzeDocument(document: vscode.TextDocument): Promise<AnalysisResult>;
    
    /**
     * Get available quick fixes for a diagnostic
     * @param diagnostic - Accessibility diagnostic
     * @param document - Source document
     * @returns Promise resolving to available fixes
     */
    getQuickFixes(diagnostic: vscode.Diagnostic, document: vscode.TextDocument): Promise<QuickFix[]>;
    
    /**
     * Apply a quick fix to a document
     * @param fix - Quick fix to apply
     * @param document - Target document
     * @returns Promise resolving to success status
     */
    applyQuickFix(fix: QuickFix, document: vscode.TextDocument): Promise<boolean>;
    
    /**
     * Get current configuration
     * @returns Current extension configuration
     */
    getConfiguration(): IConfiguration;
    
    /**
     * Update configuration
     * @param config - New configuration values
     * @returns Promise resolving when configuration is updated
     */
    updateConfiguration(config: Partial<IConfiguration>): Promise<void>;
  }

  /**
   * Analysis result interface
   */
  interface AnalysisResult {
    /** Document that was analyzed */
    readonly document: vscode.TextDocument;
    
    /** List of accessibility violations found */
    readonly violations: RuleViolation[];
    
    /** Analysis metadata */
    readonly metadata: AnalysisMetadata;
    
    /** Summary statistics */
    readonly summary: AnalysisSummary;
  }

  /**
   * Analysis metadata
   */
  interface AnalysisMetadata {
    /** Framework detected in the document */
    framework: SupportedFramework;
    
    /** Time taken for analysis in milliseconds */
    analysisTime: number;
    
    /** Number of elements analyzed */
    elementsAnalyzed: number;
    
    /** Analysis mode used */
    analysisMode: 'full' | 'incremental' | 'cached';
    
    /** Version of the extension that performed analysis */
    extensionVersion: string;
  }

  /**
   * Analysis summary statistics
   */
  interface AnalysisSummary {
    /** Total violations by severity */
    violationsBySeverity: {
      error: number;
      warning: number;
      info: number;
      hint: number;
    };
    
    /** Violations by WCAG level */
    violationsByLevel: {
      A: number;
      AA: number;
      AAA: number;
    };
    
    /** Violations by category */
    violationsByCategory: Record<AccessibilityCategory, number>;
    
    /** Overall accessibility score (0-100) */
    accessibilityScore: number;
    
    /** Number of auto-fixable violations */
    autoFixableCount: number;
  }

  /**
   * Rule violation interface
   */
  interface RuleViolation {
    /** Unique identifier for the rule */
    readonly ruleId: string;
    
    /** WCAG criterion (e.g., "1.1.1") */
    readonly wcagCriterion: string;
    
    /** WCAG level (A, AA, AAA) */
    readonly wcagLevel: 'A' | 'AA' | 'AAA';
    
    /** Violation category */
    readonly category: AccessibilityCategory;
    
    /** Human-readable violation message */
    readonly message: string;
    
    /** Element that has the violation */
    readonly element: ParsedElement;
    
    /** Location in the document */
    readonly location: ElementLocation;
    
    /** Diagnostic severity */
    readonly severity: vscode.DiagnosticSeverity;
    
    /** Available quick fixes */
    readonly fixes: QuickFix[];
    
    /** URL for more information */
    readonly learnMoreUrl?: string;
    
    /** Impact description */
    readonly impact?: string;
    
    /** Code examples */
    readonly examples?: {
      problematic: string;
      corrected: string;
    };
  }

  /**
   * Parsed element interface
   */
  interface ParsedElement {
    /** HTML tag name */
    readonly tagName: string;
    
    /** Element attributes */
    readonly attributes: ReadonlyMap<string, AttributeValue>;
    
    /** Child elements and text content */
    readonly children: ElementChildren;
    
    /** Location in source code */
    readonly location: ElementLocation;
    
    /** Framework this element belongs to */
    readonly framework: SupportedFramework;
    
    /** Element type (DOM element or component) */
    readonly elementType: 'dom' | 'component';
    
    /** Accessibility-specific information */
    readonly accessibilityInfo: AccessibilityInfo;
    
    /** Element context information */
    readonly context?: ElementContext;
  }

  /**
   * Quick fix interface
   */
  interface QuickFix {
    /** Unique identifier for this fix */
    readonly id: string;
    
    /** Human-readable title */
    readonly title: string;
    
    /** Detailed description */
    readonly description: string;
    
    /** Fix category */
    readonly category: string;
    
    /** Confidence level */
    readonly confidence: 'low' | 'medium' | 'high';
    
    /** Edit operations to apply */
    readonly edit: Edit;
    
    /** Whether this fix is preferred */
    readonly isPreferred?: boolean;
    
    /** User input required */
    readonly userInput?: UserInput;
    
    /** Whether to show preview */
    readonly showPreview?: boolean;
  }

  /**
   * Configuration interface
   */
  interface IConfiguration {
    /** Whether extension is enabled */
    enabled: boolean;
    
    /** WCAG compliance level */
    wcagLevel: 'A' | 'AA' | 'AAA';
    
    /** Rule enablement settings */
    rules: Record<string, boolean>;
    
    /** Severity level mappings */
    severity: Record<string, vscode.DiagnosticSeverity>;
    
    /** Supported frameworks */
    frameworks: SupportedFramework[];
    
    /** Auto-fix settings */
    autoFix: AutoFixSettings;
    
    /** File patterns to include/exclude */
    filePatterns: {
      include: string[];
      exclude: string[];
    };
  }

  /**
   * WCAG rule interface
   */
  interface IWCAGRule {
    /** Rule identifier */
    readonly id: string;
    
    /** WCAG criterion */
    readonly wcagCriterion: string;
    
    /** WCAG level */
    readonly level: 'A' | 'AA' | 'AAA';
    
    /** Rule category */
    readonly category: AccessibilityCategory;
    
    /** Rule description */
    readonly description: string;
    
    /**
     * Analyze an element for violations
     */
    analyze(element: ParsedElement, context: DocumentContext): Promise<RuleViolation[]>;
    
    /**
     * Generate quick fixes for a violation
     */
    generateFixes(violation: RuleViolation): Promise<QuickFix[]>;
    
    /**
     * Check if rule is enabled
     */
    isEnabled(): boolean;
  }

  /**
   * Framework parser interface
   */
  interface IFrameworkParser {
    /** Parser name */
    readonly name: string;
    
    /** Supported file extensions */
    readonly supportedExtensions: string[];
    
    /**
     * Parse document content
     */
    parse(content: string, fileName: string): Promise<ParsedDocument>;
    
    /**
     * Check if parser can handle the file
     */
    canParse(document: vscode.TextDocument): boolean;
  }

  /**
   * Supported framework types
   */
  type SupportedFramework = 'react' | 'angular' | 'vue' | 'html';

  /**
   * Accessibility categories
   */
  enum AccessibilityCategory {
    ALT_TEXT = 'alt-text',
    ARIA_LABELS = 'aria-labels',
    HEADER_HIERARCHY = 'header-hierarchy',
    BUTTON_LABELS = 'button-labels',
    INPUT_LABELS = 'input-labels',
    TITLE_METADATA = 'title-metadata',
    COLOR_CONTRAST = 'color-contrast',
    KEYBOARD_NAVIGATION = 'keyboard-navigation',
    FOCUS_MANAGEMENT = 'focus-management'
  }

  /**
   * Element location in source code
   */
  interface ElementLocation {
    /** Line number (1-based) */
    line: number;
    
    /** Column number (0-based) */
    column: number;
    
    /** Length of the element */
    length: number;
  }

  /**
   * Attribute value with type information
   */
  interface AttributeValue {
    /** Value type */
    type: 'string' | 'number' | 'boolean' | 'variable' | 'expression' | 'template';
    
    /** Actual value */
    value: any;
    
    /** Raw source code */
    raw: string;
    
    /** Whether value is dynamic */
    dynamic?: boolean;
    
    /** Variable scope information */
    scope?: VariableScope;
  }

  /**
   * Element children information
   */
  interface ElementChildren {
    /** Combined text content */
    textContent: string;
    
    /** Child elements */
    childElements: ParsedElement[];
    
    /** Whether any children are interactive */
    hasInteractiveChildren: boolean;
  }

  /**
   * Accessibility-specific element information
   */
  interface AccessibilityInfo {
    /** Whether element is interactive */
    isInteractive: boolean;
    
    /** Whether element has accessible name */
    hasAccessibleName: boolean;
    
    /** ARIA attributes */
    ariaAttributes: ReadonlyMap<string, AttributeValue>;
    
    /** Semantic role */
    semanticRole: string | null;
    
    /** Whether element is focusable */
    focusable: boolean;
  }

  /**
   * Events emitted by the extension
   */
  interface ExtensionEvents {
    /** Fired when analysis starts */
    onAnalysisStart: vscode.Event<vscode.TextDocument>;
    
    /** Fired when analysis completes */
    onAnalysisComplete: vscode.Event<AnalysisResult>;
    
    /** Fired when configuration changes */
    onConfigurationChanged: vscode.Event<IConfiguration>;
    
    /** Fired when quick fix is applied */
    onQuickFixApplied: vscode.Event<{ fix: QuickFix; document: vscode.TextDocument }>;
  }
}

/**
 * Extension commands available via Command Palette
 */
declare namespace AccessibilityCommands {
  /**
   * Analyze current document
   */
  function analyzeCurrentDocument(): Promise<void>;
  
  /**
   * Fix all auto-fixable issues in current file
   */
  function fixAllInFile(): Promise<void>;
  
  /**
   * Fix all auto-fixable issues in workspace
   */
  function fixAllInWorkspace(): Promise<void>;
  
  /**
   * Generate accessibility report
   */
  function generateReport(): Promise<void>;
  
  /**
   * Open extension settings
   */
  function openSettings(): Promise<void>;
  
  /**
   * Show accessibility help
   */
  function showHelp(): Promise<void>;
  
  /**
   * Toggle extension enabled/disabled
   */
  function toggleEnabled(): Promise<void>;
}

/**
 * Extension contribution points
 */
declare namespace AccessibilityContributions {
  /**
   * Available commands
   */
  interface Commands {
    'accessibility.analyzeDocument': typeof AccessibilityCommands.analyzeCurrentDocument;
    'accessibility.fixAllInFile': typeof AccessibilityCommands.fixAllInFile;
    'accessibility.fixAllInWorkspace': typeof AccessibilityCommands.fixAllInWorkspace;
    'accessibility.generateReport': typeof AccessibilityCommands.generateReport;
    'accessibility.openSettings': typeof AccessibilityCommands.openSettings;
    'accessibility.showHelp': typeof AccessibilityCommands.showHelp;
    'accessibility.toggleEnabled': typeof AccessibilityCommands.toggleEnabled;
  }

  /**
   * Configuration properties
   */
  interface ConfigurationProperties {
    'accessibility.enabled': boolean;
    'accessibility.wcagLevel': 'A' | 'AA' | 'AAA';
    'accessibility.rules': Record<string, boolean>;
    'accessibility.severity': Record<string, string>;
    'accessibility.frameworks': string[];
    'accessibility.autoFix': object;
    'accessibility.excludePatterns': string[];
    'accessibility.includePatterns': string[];
  }
}

/**
 * Extension activation function
 */
export function activate(context: vscode.ExtensionContext): AccessibilityExtension.IAccessibilityExtension;

/**
 * Extension deactivation function
 */
export function deactivate(): void;
```

This comprehensive specification provides everything needed to implement a production-ready VSCode accessibility extension. It includes detailed feature descriptions, user stories with acceptance criteria, complete technical implementation details, robust error handling, comprehensive testing strategies, and a realistic implementation plan. The specification is designed to be actionable by development teams while ensuring the resulting extension meets the highest standards for developer experience and accessibility compliance.