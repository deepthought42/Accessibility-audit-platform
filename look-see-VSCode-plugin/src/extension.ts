import * as vscode from 'vscode';
import { AccessibilityAnalyzer } from './core/analyzer';
import { AccessibilityCodeActionProvider } from './providers/quick-fix';
import { ConfigurationManager } from './utils/config';

let analyzer: AccessibilityAnalyzer;
let codeActionProvider: AccessibilityCodeActionProvider;

export async function activate(context: vscode.ExtensionContext) {
    console.log('Look-See Accessibility extension is now active');

    try {
        // Initialize core components
        analyzer = new AccessibilityAnalyzer(context);
        codeActionProvider = new AccessibilityCodeActionProvider(analyzer);

        // Register code action provider for supported languages
        const supportedLanguages = ['javascript', 'typescript', 'javascriptreact', 'typescriptreact', 'html', 'vue'];
        context.subscriptions.push(
            vscode.languages.registerCodeActionsProvider(
                supportedLanguages,
                codeActionProvider,
                {
                    providedCodeActionKinds: [
                        vscode.CodeActionKind.QuickFix,
                        vscode.CodeActionKind.SourceFixAll
                    ]
                }
            )
        );

        // Register commands
        registerCommands(context);

        // Set up configuration change handling
        context.subscriptions.push(
            ConfigurationManager.onConfigurationChanged(onConfigurationChanged)
        );

        // Analyze currently open documents
        await analyzeOpenDocuments();

        // Show welcome message if first time activation
        await showWelcomeMessage(context);

        console.log('Look-See Accessibility extension activated successfully');
    } catch (error) {
        console.error('Error activating Look-See Accessibility extension:', error);
        vscode.window.showErrorMessage('Failed to activate Look-See Accessibility extension');
    }
}

export function deactivate() {
    if (analyzer) {
        analyzer.dispose();
    }
    console.log('Look-See Accessibility extension deactivated');
}

function registerCommands(context: vscode.ExtensionContext) {
    // Command: Analyze current workspace
    context.subscriptions.push(
        vscode.commands.registerCommand('lookSee.analyzeWorkspace', async () => {
            await analyzeWorkspace();
        })
    );

    // Command: Fix all issues in current file
    context.subscriptions.push(
        vscode.commands.registerCommand('lookSee.fixAllInFile', async (ruleId?: string, uri?: vscode.Uri) => {
            await fixAllInFile(ruleId, uri);
        })
    );

    // Command: Generate accessibility report
    context.subscriptions.push(
        vscode.commands.registerCommand('lookSee.generateReport', async () => {
            await generateAccessibilityReport();
        })
    );

    // Command: Toggle analysis on/off
    context.subscriptions.push(
        vscode.commands.registerCommand('lookSee.toggleAnalysis', async () => {
            await toggleAnalysis();
        })
    );

    // Command: Apply fix with user input
    context.subscriptions.push(
        vscode.commands.registerCommand('lookSee.applyFixWithInput', async (fix, diagnostic) => {
            return await codeActionProvider.applyFixWithInput(fix, diagnostic);
        })
    );

    // Command: Configure extension settings
    context.subscriptions.push(
        vscode.commands.registerCommand('lookSee.openSettings', () => {
            vscode.commands.executeCommand('workbench.action.openSettings', 'lookSee');
        })
    );

    // Command: Show accessibility help
    context.subscriptions.push(
        vscode.commands.registerCommand('lookSee.showHelp', async () => {
            await showAccessibilityHelp();
        })
    );

    // Command: Reset configuration to defaults
    context.subscriptions.push(
        vscode.commands.registerCommand('lookSee.resetConfiguration', async () => {
            await resetConfiguration();
        })
    );

    // Command: Export configuration
    context.subscriptions.push(
        vscode.commands.registerCommand('lookSee.exportConfiguration', async () => {
            await exportConfiguration();
        })
    );

    // Command: Import configuration
    context.subscriptions.push(
        vscode.commands.registerCommand('lookSee.importConfiguration', async () => {
            await importConfiguration();
        })
    );
}

async function analyzeWorkspace(): Promise<void> {
    try {
        vscode.window.withProgress({
            location: vscode.ProgressLocation.Notification,
            title: "Analyzing workspace for accessibility issues...",
            cancellable: false
        }, async (progress) => {
            const results = await analyzer.analyzeWorkspace();
            
            let totalViolations = 0;
            results.forEach(result => {
                totalViolations += result.violations.length;
            });

            const message = totalViolations === 0 
                ? 'No accessibility issues found in workspace'
                : `Found ${totalViolations} accessibility issues across ${results.size} files`;
            
            vscode.window.showInformationMessage(message);
        });
    } catch (error) {
        console.error('Error analyzing workspace:', error);
        vscode.window.showErrorMessage('Failed to analyze workspace for accessibility issues');
    }
}

async function fixAllInFile(ruleId?: string, uri?: vscode.Uri): Promise<void> {
    try {
        const targetUri = uri || vscode.window.activeTextEditor?.document.uri;
        if (!targetUri) {
            vscode.window.showWarningMessage('No active file to fix');
            return;
        }

        const fixedCount = await codeActionProvider.fixAllInFile(ruleId || null, targetUri);
        
        if (fixedCount > 0) {
            const ruleText = ruleId ? ` for rule ${ruleId}` : '';
            vscode.window.showInformationMessage(`Fixed ${fixedCount} accessibility issue${fixedCount === 1 ? '' : 's'}${ruleText}`);
        } else {
            vscode.window.showInformationMessage('No fixable accessibility issues found');
        }
    } catch (error) {
        console.error('Error fixing issues:', error);
        vscode.window.showErrorMessage('Failed to fix accessibility issues');
    }
}

async function generateAccessibilityReport(): Promise<void> {
    try {
        vscode.window.withProgress({
            location: vscode.ProgressLocation.Notification,
            title: "Generating accessibility report...",
            cancellable: false
        }, async (progress) => {
            const results = await analyzer.analyzeWorkspace();
            
            // Generate report content
            const reportContent = generateReportContent(results);
            
            // Create and show report document
            const reportDoc = await vscode.workspace.openTextDocument({
                content: reportContent,
                language: 'markdown'
            });
            
            await vscode.window.showTextDocument(reportDoc);
        });
    } catch (error) {
        console.error('Error generating report:', error);
        vscode.window.showErrorMessage('Failed to generate accessibility report');
    }
}

async function toggleAnalysis(): Promise<void> {
    try {
        const config = ConfigurationManager.getConfiguration();
        const newEnabledState = !config.enabled;
        
        await ConfigurationManager.updateConfiguration('enabled', newEnabledState);
        
        const message = newEnabledState 
            ? 'Look-See accessibility analysis enabled' 
            : 'Look-See accessibility analysis disabled';
        
        vscode.window.showInformationMessage(message);
        
        // Re-analyze open documents if enabled
        if (newEnabledState) {
            await analyzeOpenDocuments();
        } else {
            // Clear all diagnostics if disabled
            analyzer.getDiagnosticCollection().clear();
        }
    } catch (error) {
        console.error('Error toggling analysis:', error);
        vscode.window.showErrorMessage('Failed to toggle accessibility analysis');
    }
}

async function showAccessibilityHelp(): Promise<void> {
    const helpContent = `
# Look-See Accessibility Help

## Overview
Look-See provides real-time accessibility analysis for React, Angular, Vue.js, and HTML files based on WCAG 2.2 guidelines.

## Supported Rules
- **Alt Text**: Images must have alternative text
- **ARIA Labels**: Interactive elements must have accessible names  
- **Heading Structure**: Proper heading hierarchy and structure

## Quick Fixes
Most violations can be fixed automatically:
- Right-click on underlined violations
- Use Ctrl+. (Cmd+. on Mac) for quick fixes
- Use the Command Palette: "Look-See: Fix All Issues"

## Configuration
Access settings via:
- Command Palette: "Look-See: Open Settings"
- VS Code Settings: Search for "lookSee"

## Commands
- \`Look-See: Analyze Workspace\` - Scan all files
- \`Look-See: Generate Report\` - Create accessibility report
- \`Look-See: Toggle Analysis\` - Enable/disable extension

For more information, visit the WCAG guidelines: https://www.w3.org/WAI/WCAG22/
    `;

    const helpDoc = await vscode.workspace.openTextDocument({
        content: helpContent.trim(),
        language: 'markdown'
    });
    
    await vscode.window.showTextDocument(helpDoc);
}

async function resetConfiguration(): Promise<void> {
    const choice = await vscode.window.showWarningMessage(
        'This will reset all Look-See settings to their default values. Continue?',
        'Reset',
        'Cancel'
    );
    
    if (choice === 'Reset') {
        try {
            await ConfigurationManager.resetToDefaults();
            vscode.window.showInformationMessage('Look-See configuration reset to defaults');
            
            // Re-analyze with new settings
            await analyzeOpenDocuments();
        } catch (error) {
            console.error('Error resetting configuration:', error);
            vscode.window.showErrorMessage('Failed to reset configuration');
        }
    }
}

async function exportConfiguration(): Promise<void> {
    try {
        const config = ConfigurationManager.exportConfiguration();
        const configJson = JSON.stringify(config, null, 2);
        
        const doc = await vscode.workspace.openTextDocument({
            content: configJson,
            language: 'json'
        });
        
        await vscode.window.showTextDocument(doc);
        vscode.window.showInformationMessage('Configuration exported. Save this file to share your settings.');
    } catch (error) {
        console.error('Error exporting configuration:', error);
        vscode.window.showErrorMessage('Failed to export configuration');
    }
}

async function importConfiguration(): Promise<void> {
    try {
        const fileUri = await vscode.window.showOpenDialog({
            canSelectFiles: true,
            canSelectFolders: false,
            canSelectMany: false,
            filters: {
                'JSON files': ['json']
            },
            openLabel: 'Import Configuration'
        });
        
        if (fileUri && fileUri[0]) {
            const configData = await vscode.workspace.fs.readFile(fileUri[0]);
            const configJson = JSON.parse(configData.toString());
            
            const validation = ConfigurationManager.validateConfiguration(configJson);
            if (!validation.isValid) {
                vscode.window.showErrorMessage(`Invalid configuration: ${validation.errors.join(', ')}`);
                return;
            }
            
            await ConfigurationManager.importConfiguration(configJson);
            vscode.window.showInformationMessage('Configuration imported successfully');
            
            // Re-analyze with new settings
            await analyzeOpenDocuments();
        }
    } catch (error) {
        console.error('Error importing configuration:', error);
        vscode.window.showErrorMessage('Failed to import configuration');
    }
}

async function analyzeOpenDocuments(): Promise<void> {
    const config = ConfigurationManager.getConfiguration();
    if (!config.enabled) {
        return;
    }

    // Analyze all currently open text documents
    const openDocuments = vscode.workspace.textDocuments;
    for (const document of openDocuments) {
        if (document.uri.scheme === 'file') {
            await analyzer.analyzeDocument(document);
        }
    }
}

function onConfigurationChanged(event: vscode.ConfigurationChangeEvent): void {
    console.log('Look-See configuration changed');
    
    // Re-analyze open documents with new configuration
    analyzeOpenDocuments().catch(error => {
        console.error('Error re-analyzing documents after configuration change:', error);
    });
}

function generateReportContent(results: Map<vscode.Uri, any>): string {
    const now = new Date().toLocaleString();
    let totalViolations = 0;
    let totalFiles = 0;
    const violationsByRule = new Map<string, number>();
    const violationsBySeverity = new Map<string, number>();
    
    // Aggregate statistics
    results.forEach((result, uri) => {
        totalFiles++;
        totalViolations += result.violations.length;
        
        for (const violation of result.violations) {
            violationsByRule.set(violation.ruleId, (violationsByRule.get(violation.ruleId) || 0) + 1);
            violationsBySeverity.set(violation.severity, (violationsBySeverity.get(violation.severity) || 0) + 1);
        }
    });
    
    let report = `# Accessibility Analysis Report

Generated: ${now}

## Summary
- **Files analyzed**: ${totalFiles}
- **Total violations**: ${totalViolations}
- **Files with issues**: ${Array.from(results.values()).filter(r => r.violations.length > 0).length}

## Violations by Severity
`;
    
    for (const [severity, count] of violationsBySeverity) {
        report += `- **${severity.toUpperCase()}**: ${count}\n`;
    }
    
    report += '\n## Violations by Rule\n';
    
    for (const [rule, count] of violationsByRule) {
        report += `- **${rule}**: ${count}\n`;
    }
    
    report += '\n## File Details\n';
    
    // Add details for each file
    for (const [uri, result] of results) {
        if (result.violations.length > 0) {
            const relativePath = vscode.workspace.asRelativePath(uri);
            report += `\n### ${relativePath}\n`;
            report += `**${result.violations.length} issue${result.violations.length === 1 ? '' : 's'}**\n\n`;
            
            for (const violation of result.violations) {
                const line = violation.range.start.line + 1;
                const col = violation.range.start.character + 1;
                report += `- **Line ${line}:${col}** - ${violation.message}\n`;
                report += `  - Rule: \`${violation.ruleId}\`\n`;
                report += `  - Severity: ${violation.severity}\n`;
                if (violation.wcagReference) {
                    report += `  - WCAG: ${violation.wcagReference.criterion} (${violation.wcagReference.level})\n`;
                }
                report += '\n';
            }
        }
    }
    
    return report;
}

async function showWelcomeMessage(context: vscode.ExtensionContext): Promise<void> {
    const hasShownWelcome = context.globalState.get<boolean>('lookSee.hasShownWelcome', false);
    
    if (!hasShownWelcome) {
        const choice = await vscode.window.showInformationMessage(
            'Welcome to Look-See Accessibility! Would you like to see the help guide?',
            'Show Help',
            'Maybe Later'
        );
        
        if (choice === 'Show Help') {
            await showAccessibilityHelp();
        }
        
        await context.globalState.update('lookSee.hasShownWelcome', true);
    }
}