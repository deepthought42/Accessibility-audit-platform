import * as vscode from 'vscode';
import { AccessibilityAnalyzer } from '../core/analyzer';
import { AccessibilityViolation, QuickFix, UserInputPrompt, ViolationFixedEvent } from '../types';

export class AccessibilityCodeActionProvider implements vscode.CodeActionProvider {
  private analyzer: AccessibilityAnalyzer;

  constructor(analyzer: AccessibilityAnalyzer) {
    this.analyzer = analyzer;
  }

  async provideCodeActions(
    document: vscode.TextDocument,
    range: vscode.Range | vscode.Selection,
    context: vscode.CodeActionContext,
    token: vscode.CancellationToken
  ): Promise<vscode.CodeAction[]> {
    const codeActions: vscode.CodeAction[] = [];
    
    // Get diagnostics for this range
    const diagnostics = context.diagnostics.filter(diagnostic => 
      diagnostic.source === 'Look-See Accessibility' && 
      range.intersection(diagnostic.range)
    );

    for (const diagnostic of diagnostics) {
      if (diagnostic.code && typeof diagnostic.code === 'string') {
        const actions = await this.createCodeActionsForDiagnostic(diagnostic, document);
        codeActions.push(...actions);
      }
    }

    // Add bulk fix actions
    if (diagnostics.length > 1) {
      const bulkActions = this.createBulkFixActions(diagnostics, document);
      codeActions.push(...bulkActions);
    }

    return codeActions;
  }

  private async createCodeActionsForDiagnostic(
    diagnostic: vscode.Diagnostic,
    document: vscode.TextDocument
  ): Promise<vscode.CodeAction[]> {
    const codeActions: vscode.CodeAction[] = [];
    const ruleId = diagnostic.code as string;
    const rule = this.analyzer.getRuleRegistry().getRule(ruleId);
    
    if (!rule || !rule.generateFixes) {
      return codeActions;
    }

    try {
      // Create a mock violation to get fixes
      const violation: AccessibilityViolation = {
        id: `${ruleId}-${Date.now()}`,
        ruleId,
        message: diagnostic.message,
        severity: this.mapDiagnosticSeverity(diagnostic.severity),
        range: diagnostic.range,
        context: await this.getDocumentContext(document)
      };

      const fixes = await rule.generateFixes(violation);
      
      for (const fix of fixes) {
        const action = this.createCodeActionFromFix(fix, diagnostic);
        if (action) {
          codeActions.push(action);
        }
      }
    } catch (error) {
      console.error(`Error generating fixes for rule ${ruleId}:`, error);
    }

    return codeActions;
  }

  private createCodeActionFromFix(fix: QuickFix, diagnostic: vscode.Diagnostic): vscode.CodeAction | null {
    const action = new vscode.CodeAction(fix.title, vscode.CodeActionKind.QuickFix);
    action.diagnostics = [diagnostic];
    action.isPreferred = fix.isPreferred || false;

    if (fix.userInput) {
      // Create a command that will prompt for user input
      action.command = {
        command: 'lookSee.applyFixWithInput',
        title: fix.title,
        arguments: [fix, diagnostic]
      };
    } else if (fix.edit) {
      action.edit = fix.edit;
    } else if (fix.command) {
      action.command = fix.command;
    }

    return action;
  }

  private createBulkFixActions(
    diagnostics: vscode.Diagnostic[],
    document: vscode.TextDocument
  ): vscode.CodeAction[] {
    const actions: vscode.CodeAction[] = [];
    
    // Group diagnostics by rule
    const ruleGroups = new Map<string, vscode.Diagnostic[]>();
    for (const diagnostic of diagnostics) {
      if (diagnostic.code && typeof diagnostic.code === 'string') {
        const ruleId = diagnostic.code;
        if (!ruleGroups.has(ruleId)) {
          ruleGroups.set(ruleId, []);
        }
        ruleGroups.get(ruleId)!.push(diagnostic);
      }
    }

    // Create bulk fix actions for each rule
    for (const [ruleId, ruleDiagnostics] of ruleGroups) {
      if (ruleDiagnostics.length > 1) {
        const rule = this.analyzer.getRuleRegistry().getRule(ruleId);
        if (rule) {
          const action = new vscode.CodeAction(
            `Fix all ${rule.description.toLowerCase()} issues (${ruleDiagnostics.length})`,
            vscode.CodeActionKind.QuickFix
          );
          action.diagnostics = ruleDiagnostics;
          action.command = {
            command: 'lookSee.fixAllInFile',
            title: action.title,
            arguments: [ruleId, document.uri]
          };
          actions.push(action);
        }
      }
    }

    // Create fix-all action
    if (diagnostics.length > 1) {
      const action = new vscode.CodeAction(
        `Fix all accessibility issues (${diagnostics.length})`,
        vscode.CodeActionKind.SourceFixAll
      );
      action.diagnostics = diagnostics;
      action.command = {
        command: 'lookSee.fixAllInFile',
        title: action.title,
        arguments: [null, document.uri] // null means all rules
      };
      actions.push(action);
    }

    return actions;
  }

  async applyFixWithInput(fix: QuickFix, diagnostic: vscode.Diagnostic): Promise<boolean> {
    if (!fix.userInput) {
      return false;
    }

    try {
      const userValue = await this.promptUserInput(fix.userInput);
      if (userValue === undefined) {
        return false; // User cancelled
      }

      // Apply the fix with user input
      let edit = fix.edit;
      if (edit && fix.userInput) {
        edit = this.substituteUserInputInEdit(edit, userValue);
      }

      if (edit) {
        const success = await vscode.workspace.applyEdit(edit);
        
        if (success) {
          this.fireViolationFixedEvent(fix, true);
        }
        
        return success;
      }

      return false;
    } catch (error) {
      console.error('Error applying fix with input:', error);
      this.fireViolationFixedEvent(fix, false);
      return false;
    }
  }

  async fixAllInFile(ruleId: string | null, uri: vscode.Uri): Promise<number> {
    try {
      const document = await vscode.workspace.openTextDocument(uri);
      const diagnostics = this.analyzer.getDiagnosticCollection().get(uri);
      
      if (!diagnostics || diagnostics.length === 0) {
        return 0;
      }

      let targetDiagnostics = diagnostics;
      if (ruleId) {
        targetDiagnostics = diagnostics.filter(d => d.code === ruleId);
      }

      let fixedCount = 0;
      const workspaceEdit = new vscode.WorkspaceEdit();

      for (const diagnostic of targetDiagnostics) {
        const rule = this.analyzer.getRuleRegistry().getRule(diagnostic.code as string);
        if (!rule || !rule.generateFixes) {
          continue;
        }

        try {
          const violation: AccessibilityViolation = {
            id: `${rule.id}-${Date.now()}`,
            ruleId: rule.id,
            message: diagnostic.message,
            severity: this.mapDiagnosticSeverity(diagnostic.severity),
            range: diagnostic.range,
            context: await this.getDocumentContext(document)
          };

          const fixes = await rule.generateFixes(violation);
          const preferredFix = fixes.find(f => f.isPreferred) || fixes[0];
          
          if (preferredFix && preferredFix.edit && !preferredFix.userInput) {
            // Only apply fixes that don't require user input
            this.mergeWorkspaceEdit(workspaceEdit, preferredFix.edit);
            fixedCount++;
          }
        } catch (error) {
          console.error(`Error generating fix for ${rule.id}:`, error);
        }
      }

      if (fixedCount > 0) {
        await vscode.workspace.applyEdit(workspaceEdit);
      }

      return fixedCount;
    } catch (error) {
      console.error('Error in fixAllInFile:', error);
      return 0;
    }
  }

  private async promptUserInput(input: UserInputPrompt): Promise<string | undefined> {
    switch (input.type) {
      case 'text':
        return this.promptTextInput(input);
      case 'select':
        return this.promptSelectInput(input);
      case 'multiselect':
        return this.promptMultiSelectInput(input);
      default:
        return undefined;
    }
  }

  private async promptTextInput(input: UserInputPrompt): Promise<string | undefined> {
    const options: vscode.InputBoxOptions = {
      prompt: input.prompt,
      placeHolder: input.placeholder,
      validateInput: (value: string) => {
        if (input.validation?.required && !value.trim()) {
          return 'This field is required';
        }
        
        if (input.validation?.minLength && value.length < input.validation.minLength) {
          return `Minimum length is ${input.validation.minLength} characters`;
        }
        
        if (input.validation?.maxLength && value.length > input.validation.maxLength) {
          return `Maximum length is ${input.validation.maxLength} characters`;
        }
        
        if (input.validation?.pattern && !input.validation.pattern.test(value)) {
          return 'Invalid format';
        }
        
        return null;
      }
    };

    return await vscode.window.showInputBox(options);
  }

  private async promptSelectInput(input: UserInputPrompt): Promise<string | undefined> {
    if (!input.options || input.options.length === 0) {
      return undefined;
    }

    const selected = await vscode.window.showQuickPick(input.options, {
      placeHolder: input.prompt
    });

    return selected;
  }

  private async promptMultiSelectInput(input: UserInputPrompt): Promise<string | undefined> {
    if (!input.options || input.options.length === 0) {
      return undefined;
    }

    const items = input.options.map(option => ({
      label: option,
      picked: false
    }));

    const selected = await vscode.window.showQuickPick(items, {
      placeHolder: input.prompt,
      canPickMany: true
    });

    if (!selected || selected.length === 0) {
      return undefined;
    }

    return selected.map(item => item.label).join(', ');
  }

  private substituteUserInputInEdit(edit: vscode.WorkspaceEdit, userValue: string): vscode.WorkspaceEdit {
    const newEdit = new vscode.WorkspaceEdit();
    
    // Iterate through all text edits and replace placeholder values
    edit.entries().forEach(([uri, textEdits]) => {
      const newTextEdits = textEdits.map(textEdit => {
        let newText = textEdit.newText;
        
        // Replace common placeholders
        newText = newText.replace(/Describe this image/g, userValue);
        newText = newText.replace(/Describe [^"']*/g, userValue);
        newText = newText.replace(/Button text/g, userValue);
        newText = newText.replace(/Heading text/g, userValue);
        newText = newText.replace(/Improved [^"']*/g, userValue);
        newText = newText.replace(/Specific [^"']*/g, userValue);
        
        return new vscode.TextEdit(textEdit.range, newText);
      });
      
      newEdit.set(uri, newTextEdits);
    });
    
    return newEdit;
  }

  private mergeWorkspaceEdit(target: vscode.WorkspaceEdit, source: vscode.WorkspaceEdit): void {
    source.entries().forEach(([uri, textEdits]) => {
      const existingEdits = target.get(uri) || [];
      target.set(uri, [...existingEdits, ...textEdits]);
    });
  }

  private async getDocumentContext(document: vscode.TextDocument): Promise<any> {
    const parser = this.analyzer.getParserRegistry().getParserForDocument(document);
    if (parser) {
      return await parser.parse(document);
    }
    return null;
  }

  private mapDiagnosticSeverity(severity: vscode.DiagnosticSeverity | undefined): 'error' | 'warning' | 'info' {
    switch (severity) {
      case vscode.DiagnosticSeverity.Error:
        return 'error';
      case vscode.DiagnosticSeverity.Warning:
        return 'warning';
      case vscode.DiagnosticSeverity.Information:
      case vscode.DiagnosticSeverity.Hint:
        return 'info';
      default:
        return 'warning';
    }
  }

  private fireViolationFixedEvent(fix: QuickFix, success: boolean): void {
    // Could emit events here for telemetry or other extension parts
    console.log(`Fix ${fix.id} applied: ${success ? 'success' : 'failed'}`);
  }
}