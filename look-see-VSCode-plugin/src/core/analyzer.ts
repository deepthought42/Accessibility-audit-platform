import * as vscode from 'vscode';
import { ParserRegistry } from '../parsers';
import { RuleRegistry } from '../rules';
import { 
  AnalysisResult, 
  AnalysisSummary, 
  DocumentContext, 
  AccessibilityViolation,
  FrameworkType,
  ViolationSeverity,
  AnalysisCompleteEvent
} from '../types';

export class AccessibilityAnalyzer {
  private diagnosticCollection: vscode.DiagnosticCollection;
  private parserRegistry: ParserRegistry;
  private ruleRegistry: RuleRegistry;
  private analysisCache: Map<string, { result: AnalysisResult; timestamp: number }> = new Map();
  private readonly cacheTimeout = 30000; // 30 seconds

  constructor(context: vscode.ExtensionContext) {
    this.diagnosticCollection = vscode.languages.createDiagnosticCollection('lookSeeAccessibility');
    this.parserRegistry = new ParserRegistry();
    this.ruleRegistry = new RuleRegistry();
    
    context.subscriptions.push(this.diagnosticCollection);
    context.subscriptions.push(
      vscode.workspace.onDidChangeTextDocument(e => this.onDocumentChange(e)),
      vscode.workspace.onDidOpenTextDocument(doc => this.analyzeDocument(doc)),
      vscode.workspace.onDidCloseTextDocument(doc => this.clearDiagnostics(doc.uri))
    );
  }

  async analyzeDocument(document: vscode.TextDocument): Promise<AnalysisResult | null> {
    if (!this.shouldAnalyzeDocument(document)) {
      return null;
    }

    const startTime = Date.now();
    
    try {
      // Check cache first
      const cached = this.getCachedResult(document.uri.toString());
      if (cached) {
        this.updateDiagnostics(document, cached.violations);
        return cached;
      }

      // Parse document
      const parser = this.parserRegistry.getParserForDocument(document);
      if (!parser) {
        console.warn(`No parser found for document: ${document.uri.fsPath}`);
        return null;
      }

      const context = await parser.parse(document);
      
      // Run accessibility rules
      const violations = await this.runAccessibilityRules(context);
      
      // Create analysis result
      const result: AnalysisResult = {
        violations,
        summary: this.createAnalysisSummary(violations, context),
        context
      };

      // Cache result
      this.cacheResult(document.uri.toString(), result);
      
      // Update diagnostics
      this.updateDiagnostics(document, violations);
      
      // Fire analysis complete event
      const event: AnalysisCompleteEvent = {
        uri: document.uri,
        result,
        duration: Date.now() - startTime
      };
      
      this.fireAnalysisCompleteEvent(event);
      
      return result;
      
    } catch (error) {
      console.error('Error analyzing document:', error);
      return null;
    }
  }

  async analyzeWorkspace(): Promise<Map<vscode.Uri, AnalysisResult>> {
    const results = new Map<vscode.Uri, AnalysisResult>();
    
    // Find all supported files in workspace
    const supportedFiles = await this.findSupportedFiles();
    
    // Analyze each file
    for (const file of supportedFiles) {
      try {
        const document = await vscode.workspace.openTextDocument(file);
        const result = await this.analyzeDocument(document);
        
        if (result) {
          results.set(file, result);
        }
      } catch (error) {
        console.error(`Error analyzing file ${file.fsPath}:`, error);
      }
    }
    
    return results;
  }

  private async onDocumentChange(event: vscode.TextDocumentChangeEvent): Promise<void> {
    // Invalidate cache for changed document
    this.invalidateCache(event.document.uri.toString());
    
    // Debounce analysis to avoid excessive processing
    setTimeout(() => {
      this.analyzeDocument(event.document);
    }, 500);
  }

  private shouldAnalyzeDocument(document: vscode.TextDocument): boolean {
    // Check if document is supported
    const parser = this.parserRegistry.getParserForDocument(document);
    if (!parser) {
      return false;
    }

    // Check if analysis is enabled in configuration
    const config = vscode.workspace.getConfiguration('lookSee');
    if (!config.get<boolean>('enabled', true)) {
      return false;
    }

    // Check if specific framework is enabled
    const frameworkConfig = config.get<Record<string, { enabled: boolean }>>('frameworks', {});
    const frameworkEnabled = frameworkConfig[parser.framework]?.enabled !== false;
    
    return frameworkEnabled;
  }

  private async runAccessibilityRules(context: DocumentContext): Promise<AccessibilityViolation[]> {
    const violations: AccessibilityViolation[] = [];
    
    // Get enabled rules for this framework
    const config = vscode.workspace.getConfiguration('lookSee');
    const rulesConfig = config.get<Record<string, { enabled: boolean }>>('rules', {});
    const enabledRules = this.ruleRegistry.getEnabledRules(rulesConfig);
    
    // Filter rules by framework support
    const applicableRules = enabledRules.filter(rule => 
      context.framework && rule.frameworks.includes(context.framework)
    );

    // Run each rule
    for (const rule of applicableRules) {
      try {
        const ruleViolations = await rule.check(context);
        violations.push(...ruleViolations);
      } catch (error) {
        console.error(`Error running rule ${rule.id}:`, error);
      }
    }

    return violations;
  }

  private createAnalysisSummary(violations: AccessibilityViolation[], context: DocumentContext): AnalysisSummary {
    const summary: AnalysisSummary = {
      totalViolations: violations.length,
      byFramework: {} as Record<FrameworkType, number>,
      bySeverity: {} as Record<ViolationSeverity, number>,
      byRule: {}
    };

    // Count by framework
    if (context.framework) {
      summary.byFramework[context.framework] = violations.length;
    }

    // Count by severity
    for (const violation of violations) {
      summary.bySeverity[violation.severity] = (summary.bySeverity[violation.severity] || 0) + 1;
      summary.byRule[violation.ruleId] = (summary.byRule[violation.ruleId] || 0) + 1;
    }

    return summary;
  }

  private updateDiagnostics(document: vscode.TextDocument, violations: AccessibilityViolation[]): void {
    const diagnostics: vscode.Diagnostic[] = violations.map(violation => {
      const diagnostic = new vscode.Diagnostic(
        violation.range,
        violation.message,
        this.mapSeverityToDiagnosticSeverity(violation.severity)
      );
      
      diagnostic.source = 'Look-See Accessibility';
      diagnostic.code = violation.ruleId;
      
      if (violation.wcagReference) {
        diagnostic.relatedInformation = [{
          location: new vscode.Location(document.uri, violation.range),
          message: `WCAG ${violation.wcagReference.level} ${violation.wcagReference.criterion}: ${violation.wcagReference.title}`
        }];
      }

      return diagnostic;
    });

    this.diagnosticCollection.set(document.uri, diagnostics);
  }

  private mapSeverityToDiagnosticSeverity(severity: ViolationSeverity): vscode.DiagnosticSeverity {
    switch (severity) {
      case 'error':
        return vscode.DiagnosticSeverity.Error;
      case 'warning':
        return vscode.DiagnosticSeverity.Warning;
      case 'info':
        return vscode.DiagnosticSeverity.Information;
      default:
        return vscode.DiagnosticSeverity.Warning;
    }
  }

  private clearDiagnostics(uri: vscode.Uri): void {
    this.diagnosticCollection.delete(uri);
    this.invalidateCache(uri.toString());
  }

  private async findSupportedFiles(): Promise<vscode.Uri[]> {
    const supportedExtensions = new Set<string>();
    
    // Collect all supported extensions from parsers
    for (const parser of this.parserRegistry.getAllParsers()) {
      parser.supportedExtensions.forEach(ext => supportedExtensions.add(ext));
    }
    
    // Create glob pattern
    const patterns = Array.from(supportedExtensions).map(ext => `**/*${ext}`);
    const files: vscode.Uri[] = [];
    
    for (const pattern of patterns) {
      const foundFiles = await vscode.workspace.findFiles(pattern, '**/node_modules/**');
      files.push(...foundFiles);
    }
    
    return files;
  }

  private getCachedResult(key: string): AnalysisResult | null {
    const cached = this.analysisCache.get(key);
    if (!cached) {
      return null;
    }
    
    // Check if cache is still valid
    if (Date.now() - cached.timestamp > this.cacheTimeout) {
      this.analysisCache.delete(key);
      return null;
    }
    
    return cached.result;
  }

  private cacheResult(key: string, result: AnalysisResult): void {
    this.analysisCache.set(key, {
      result,
      timestamp: Date.now()
    });
  }

  private invalidateCache(key: string): void {
    this.analysisCache.delete(key);
  }

  private fireAnalysisCompleteEvent(event: AnalysisCompleteEvent): void {
    // Could emit events here for other parts of the extension to listen to
    console.log(`Analysis complete for ${event.uri.fsPath}: ${event.result.violations.length} violations found in ${event.duration}ms`);
  }

  getDiagnosticCollection(): vscode.DiagnosticCollection {
    return this.diagnosticCollection;
  }

  getParserRegistry(): ParserRegistry {
    return this.parserRegistry;
  }

  getRuleRegistry(): RuleRegistry {
    return this.ruleRegistry;
  }

  dispose(): void {
    this.diagnosticCollection.dispose();
    this.analysisCache.clear();
  }
}