import * as vscode from 'vscode';

// Core types for accessibility analysis
export interface AccessibilityViolation {
  id: string;
  ruleId: string;
  message: string;
  severity: ViolationSeverity;
  range: vscode.Range;
  element?: ParsedElement;
  context?: DocumentContext;
  wcagReference?: WcagReference;
  impact?: string;
  fixes?: QuickFix[];
}

export type ViolationSeverity = 'error' | 'warning' | 'info';

export interface WcagReference {
  level: 'A' | 'AA' | 'AAA';
  criterion: string;
  url: string;
  title: string;
}

// Document parsing types
export interface DocumentContext {
  uri: vscode.Uri;
  languageId: string;
  framework?: FrameworkType;
  text: string;
  ast?: any;
  elements?: ParsedElement[];
  imports?: ImportInfo[];
}

export type FrameworkType = 'react' | 'angular' | 'vue' | 'html';

export interface ParsedElement {
  type: string;
  tagName: string;
  attributes: Record<string, string | undefined>;
  textContent: string;
  children: ParsedElement[];
  range: vscode.Range;
  framework?: FrameworkType;
  isComponent?: boolean;
  props?: Record<string, any>;
}

export interface ImportInfo {
  name: string;
  path: string;
  isDefault: boolean;
  range: vscode.Range;
}

// Parser interfaces
export interface FrameworkParser {
  readonly framework: FrameworkType;
  readonly supportedExtensions: string[];
  canParse(document: vscode.TextDocument): boolean;
  parse(document: vscode.TextDocument): Promise<DocumentContext>;
}

// Rule engine types
export interface AccessibilityRule {
  readonly id: string;
  readonly name: string;
  readonly description: string;
  readonly severity: ViolationSeverity;
  readonly wcagReference: WcagReference;
  readonly frameworks: readonly FrameworkType[];
  
  check(context: DocumentContext): Promise<AccessibilityViolation[]>;
  generateFixes?(violation: AccessibilityViolation): Promise<QuickFix[]>;
}

// Quick fix types
export interface QuickFix {
  id: string;
  title: string;
  description: string;
  category: 'fix' | 'improve' | 'alternative';
  edit?: vscode.WorkspaceEdit;
  command?: vscode.Command;
  userInput?: UserInputPrompt;
  priority: number;
  isPreferred?: boolean;
}

export interface UserInputPrompt {
  type: 'text' | 'select' | 'multiselect';
  prompt: string;
  placeholder?: string;
  options?: string[];
  validation?: {
    required?: boolean;
    minLength?: number;
    maxLength?: number;
    pattern?: RegExp;
  };
}

// Configuration types
export interface ExtensionConfig {
  enabled: boolean;
  severity: ViolationSeverity;
  rules: Record<string, RuleConfig>;
  frameworks: Record<FrameworkType, FrameworkConfig>;
  quickFix: QuickFixConfig;
  education: EducationConfig;
}

export interface RuleConfig {
  enabled: boolean;
  severity: ViolationSeverity;
}

export interface FrameworkConfig {
  enabled: boolean;
}

export interface QuickFixConfig {
  enabled: boolean;
  showPreview: boolean;
  autoApply: boolean;
}

export interface EducationConfig {
  showImpactExplanations: boolean;
  showWcagReferences: boolean;
  showExamples: boolean;
}

// Analysis types
export interface AnalysisResult {
  violations: AccessibilityViolation[];
  summary: AnalysisSummary;
  context: DocumentContext;
}

export interface AnalysisSummary {
  totalViolations: number;
  byFramework: Record<FrameworkType, number>;
  bySeverity: Record<ViolationSeverity, number>;
  byRule: Record<string, number>;
}

// Event types
export interface AnalysisCompleteEvent {
  uri: vscode.Uri;
  result: AnalysisResult;
  duration: number;
}

export interface ViolationFixedEvent {
  violation: AccessibilityViolation;
  fix: QuickFix;
  success: boolean;
}