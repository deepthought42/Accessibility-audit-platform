import * as vscode from 'vscode';
import { ExtensionConfig, RuleConfig, FrameworkConfig, QuickFixConfig, EducationConfig } from '../types';

export class ConfigurationManager {
  private static readonly CONFIG_SECTION = 'lookSee';
  
  static getConfiguration(): ExtensionConfig {
    const config = vscode.workspace.getConfiguration(this.CONFIG_SECTION);
    
    return {
      enabled: config.get<boolean>('enabled', true),
      severity: config.get<'error' | 'warning' | 'info'>('severity', 'warning'),
      rules: config.get<Record<string, RuleConfig>>('rules', this.getDefaultRulesConfig()),
      frameworks: config.get<Record<string, FrameworkConfig>>('frameworks', this.getDefaultFrameworksConfig()),
      quickFix: config.get<QuickFixConfig>('quickFix', this.getDefaultQuickFixConfig()),
      education: config.get<EducationConfig>('education', this.getDefaultEducationConfig())
    };
  }

  static async updateConfiguration(section: string, value: any, target?: vscode.ConfigurationTarget): Promise<void> {
    const config = vscode.workspace.getConfiguration(this.CONFIG_SECTION);
    await config.update(section, value, target);
  }

  static isRuleEnabled(ruleId: string): boolean {
    const config = this.getConfiguration();
    const ruleConfig = config.rules[ruleId];
    return ruleConfig ? ruleConfig.enabled : true;
  }

  static getRuleSeverity(ruleId: string): 'error' | 'warning' | 'info' {
    const config = this.getConfiguration();
    const ruleConfig = config.rules[ruleId];
    return ruleConfig?.severity || config.severity;
  }

  static isFrameworkEnabled(framework: string): boolean {
    const config = this.getConfiguration();
    const frameworkConfig = config.frameworks[framework];
    return frameworkConfig ? frameworkConfig.enabled : true;
  }

  static async enableRule(ruleId: string, enabled: boolean): Promise<void> {
    const config = this.getConfiguration();
    const newRuleConfig: RuleConfig = {
      ...config.rules[ruleId],
      enabled
    };
    
    await this.updateConfiguration(`rules.${ruleId}`, newRuleConfig);
  }

  static async setRuleSeverity(ruleId: string, severity: 'error' | 'warning' | 'info'): Promise<void> {
    const config = this.getConfiguration();
    const newRuleConfig: RuleConfig = {
      ...config.rules[ruleId],
      severity
    };
    
    await this.updateConfiguration(`rules.${ruleId}`, newRuleConfig);
  }

  static async enableFramework(framework: string, enabled: boolean): Promise<void> {
    const frameworkConfig: FrameworkConfig = { enabled };
    await this.updateConfiguration(`frameworks.${framework}`, frameworkConfig);
  }

  static async resetToDefaults(): Promise<void> {
    const config = vscode.workspace.getConfiguration(this.CONFIG_SECTION);
    
    await config.update('enabled', undefined);
    await config.update('severity', undefined);
    await config.update('rules', undefined);
    await config.update('frameworks', undefined);
    await config.update('quickFix', undefined);
    await config.update('education', undefined);
  }

  static async importConfiguration(configData: Partial<ExtensionConfig>): Promise<void> {
    const config = vscode.workspace.getConfiguration(this.CONFIG_SECTION);
    
    if (configData.enabled !== undefined) {
      await config.update('enabled', configData.enabled);
    }
    
    if (configData.severity !== undefined) {
      await config.update('severity', configData.severity);
    }
    
    if (configData.rules !== undefined) {
      await config.update('rules', configData.rules);
    }
    
    if (configData.frameworks !== undefined) {
      await config.update('frameworks', configData.frameworks);
    }
    
    if (configData.quickFix !== undefined) {
      await config.update('quickFix', configData.quickFix);
    }
    
    if (configData.education !== undefined) {
      await config.update('education', configData.education);
    }
  }

  static exportConfiguration(): ExtensionConfig {
    return this.getConfiguration();
  }

  static getWorkspaceConfiguration(): ExtensionConfig | null {
    // Try to load workspace-specific configuration from .vscode/settings.json
    const workspaceConfig = vscode.workspace.getConfiguration(this.CONFIG_SECTION, vscode.ConfigurationTarget.Workspace);
    
    const inspect = workspaceConfig.inspect('enabled');
    if (inspect?.workspaceValue !== undefined) {
      return this.getConfiguration();
    }
    
    return null;
  }

  static async loadESLintIntegration(): Promise<boolean> {
    try {
      const workspaceFolders = vscode.workspace.workspaceFolders;
      if (!workspaceFolders || workspaceFolders.length === 0) {
        return false;
      }

      for (const folder of workspaceFolders) {
        const eslintConfigFiles = ['.eslintrc.json', '.eslintrc.js', '.eslintrc.yaml', '.eslintrc.yml'];
        
        for (const configFile of eslintConfigFiles) {
          try {
            const eslintPath = vscode.Uri.joinPath(folder.uri, configFile);
            const eslintData = await vscode.workspace.fs.readFile(eslintPath);
            const eslintConfig = JSON.parse(eslintData.toString());
            
            // Check if accessibility plugins are configured
            const plugins = eslintConfig.plugins || [];
            const hasA11yPlugin = plugins.includes('jsx-a11y') || plugins.includes('@typescript-eslint/eslint-plugin');
            
            if (hasA11yPlugin) {
              // Could potentially sync settings with ESLint config
              return true;
            }
          } catch (error) {
            // File doesn't exist or is not valid JSON
            continue;
          }
        }
      }
      
      return false;
    } catch (error) {
      console.error('Error loading ESLint integration:', error);
      return false;
    }
  }

  static onConfigurationChanged(callback: (event: vscode.ConfigurationChangeEvent) => void): vscode.Disposable {
    return vscode.workspace.onDidChangeConfiguration(event => {
      if (event.affectsConfiguration(this.CONFIG_SECTION)) {
        callback(event);
      }
    });
  }

  private static getDefaultRulesConfig(): Record<string, RuleConfig> {
    return {
      'alt-text': { enabled: true, severity: 'error' },
      'aria-labels': { enabled: true, severity: 'warning' },
      'heading-structure': { enabled: true, severity: 'warning' },
      'color-contrast': { enabled: true, severity: 'info' },
      'focus-order': { enabled: true, severity: 'warning' }
    };
  }

  private static getDefaultFrameworksConfig(): Record<string, FrameworkConfig> {
    return {
      react: { enabled: true },
      angular: { enabled: true },
      vue: { enabled: true },
      html: { enabled: true }
    };
  }

  private static getDefaultQuickFixConfig(): QuickFixConfig {
    return {
      enabled: true,
      showPreview: true,
      autoApply: false
    };
  }

  private static getDefaultEducationConfig(): EducationConfig {
    return {
      showImpactExplanations: true,
      showWcagReferences: true,
      showExamples: true
    };
  }

  static validateConfiguration(config: Partial<ExtensionConfig>): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (config.severity && !['error', 'warning', 'info'].includes(config.severity)) {
      errors.push('Invalid severity level. Must be "error", "warning", or "info"');
    }

    if (config.rules) {
      for (const [ruleId, ruleConfig] of Object.entries(config.rules)) {
        if (typeof ruleConfig.enabled !== 'boolean') {
          errors.push(`Rule ${ruleId}: enabled must be a boolean`);
        }
        
        if (ruleConfig.severity && !['error', 'warning', 'info'].includes(ruleConfig.severity)) {
          errors.push(`Rule ${ruleId}: invalid severity level`);
        }
      }
    }

    if (config.frameworks) {
      const validFrameworks = ['react', 'angular', 'vue', 'html'];
      for (const [framework, frameworkConfig] of Object.entries(config.frameworks)) {
        if (!validFrameworks.includes(framework)) {
          errors.push(`Unknown framework: ${framework}`);
        }
        
        if (typeof frameworkConfig.enabled !== 'boolean') {
          errors.push(`Framework ${framework}: enabled must be a boolean`);
        }
      }
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  }

  static getConfigurationSchema(): any {
    return {
      type: 'object',
      properties: {
        enabled: {
          type: 'boolean',
          default: true,
          description: 'Enable/disable Look-See accessibility analysis'
        },
        severity: {
          type: 'string',
          enum: ['error', 'warning', 'info'],
          default: 'warning',
          description: 'Default severity level for accessibility violations'
        },
        rules: {
          type: 'object',
          description: 'Configure individual accessibility rules',
          additionalProperties: {
            type: 'object',
            properties: {
              enabled: { type: 'boolean' },
              severity: { 
                type: 'string', 
                enum: ['error', 'warning', 'info'] 
              }
            }
          }
        },
        frameworks: {
          type: 'object',
          description: 'Enable/disable framework-specific analysis',
          additionalProperties: {
            type: 'object',
            properties: {
              enabled: { type: 'boolean' }
            }
          }
        },
        quickFix: {
          type: 'object',
          description: 'Quick fix behavior configuration',
          properties: {
            enabled: { type: 'boolean' },
            showPreview: { type: 'boolean' },
            autoApply: { type: 'boolean' }
          }
        },
        education: {
          type: 'object',
          description: 'Educational feature configuration',
          properties: {
            showImpactExplanations: { type: 'boolean' },
            showWcagReferences: { type: 'boolean' },
            showExamples: { type: 'boolean' }
          }
        }
      }
    };
  }
}