import * as vscode from 'vscode';
import { BaseRule } from './base';
import { AccessibilityViolation, DocumentContext, ParsedElement, QuickFix } from '../types';

export class AriaLabelsRule extends BaseRule {
  readonly id = 'aria-labels';
  readonly name = 'ARIA Labels Required';
  readonly description = 'Interactive elements must have accessible names';
  readonly severity = 'warning' as const;
  readonly frameworks = ['react', 'angular', 'vue', 'html'] as const;
  readonly wcagReference = {
    level: 'A' as const,
    criterion: '4.1.2',
    url: 'https://www.w3.org/WAI/WCAG22/Understanding/name-role-value.html',
    title: 'Name, Role, Value'
  };

  private interactiveElements = [
    'button', 'a', 'input', 'select', 'textarea',
    'audio', 'video', 'iframe', 'object', 'embed',
    'details', 'summary'
  ];

  private inputTypesRequiringLabels = [
    'text', 'email', 'password', 'search', 'tel', 'url',
    'number', 'range', 'date', 'time', 'datetime-local',
    'month', 'week', 'color', 'file'
  ];

  async check(context: DocumentContext): Promise<AccessibilityViolation[]> {
    const violations: AccessibilityViolation[] = [];
    
    if (!context.elements) return violations;

    const interactiveElements = this.findInteractiveElements(context.elements);

    for (const element of interactiveElements) {
      if (!this.hasAccessibleName(element)) {
        const elementType = this.getElementTypeDescription(element);
        
        violations.push(this.createViolation(
          element,
          `${elementType} is missing an accessible name. Add aria-label, aria-labelledby, or descriptive text content.`,
          context,
          await this.generateAccessibleNameFixes(element, context)
        ));
      } else if (this.hasGenericAccessibleName(element)) {
        const elementType = this.getElementTypeDescription(element);
        const currentName = this.getCurrentAccessibleName(element);
        
        violations.push(this.createViolation(
          element,
          `${elementType} has a generic accessible name "${currentName}". Provide a more specific description.`,
          context,
          await this.generateImproveNameFixes(element, context)
        ));
      }
    }

    return violations;
  }

  protected getAffectedUserTypes(): string[] {
    return ['screen reader users', 'voice control users', 'switch navigation users'];
  }

  async generateFixes(violation: AccessibilityViolation): Promise<QuickFix[]> {
    if (!violation.element) return [];

    if (!this.hasAccessibleName(violation.element)) {
      return this.generateAccessibleNameFixes(violation.element, violation.context);
    } else {
      return this.generateImproveNameFixes(violation.element, violation.context);
    }
  }

  private findInteractiveElements(elements: ParsedElement[]): ParsedElement[] {
    return this.findElementsRecursive(elements, element => {
      // Standard interactive elements
      if (this.interactiveElements.includes(element.tagName.toLowerCase())) {
        return true;
      }
      
      // Elements with interactive roles
      const role = this.getAttributeValue(element, 'role');
      if (role && this.isInteractiveRole(role)) {
        return true;
      }
      
      // Elements with click handlers (framework-specific)
      if (this.hasClickHandler(element)) {
        return true;
      }
      
      return false;
    });
  }

  private isInteractiveRole(role: string): boolean {
    const interactiveRoles = [
      'button', 'link', 'checkbox', 'radio', 'slider', 'spinbutton',
      'textbox', 'combobox', 'listbox', 'menu', 'menuitem', 'tab',
      'tabpanel', 'treeitem', 'option', 'switch'
    ];
    return interactiveRoles.includes(role.toLowerCase());
  }

  private hasClickHandler(element: ParsedElement): boolean {
    const framework = element.framework;
    
    switch (framework) {
      case 'react':
        return this.hasAttribute(element, 'onClick');
      case 'angular':
        return this.hasAttribute(element, '(click)');
      case 'vue':
        return this.hasAttribute(element, '@click') || this.hasAttribute(element, 'v-on:click');
      default:
        return this.hasAttribute(element, 'onclick');
    }
  }

  private hasGenericAccessibleName(element: ParsedElement): boolean {
    const name = this.getCurrentAccessibleName(element);
    if (!name) return false;
    
    const genericPatterns = [
      /^click$/i,
      /^button$/i,
      /^link$/i,
      /^submit$/i,
      /^next$/i,
      /^back$/i,
      /^more$/i,
      /^here$/i,
      /^read more$/i,
      /^click here$/i,
      /^learn more$/i,
      /^get started$/i,
      /^sign up$/i,
      /^log in$/i,
      /^continue$/i,
      /^\w{1,3}$/  // Very short labels (1-3 characters)
    ];
    
    return genericPatterns.some(pattern => pattern.test(name.trim()));
  }

  private getCurrentAccessibleName(element: ParsedElement): string {
    if (this.hasAriaLabel(element)) {
      return this.getAttributeValue(element, 'aria-label') || '';
    }
    
    if (element.textContent) {
      return element.textContent.trim();
    }
    
    // For form inputs, check associated label
    if (element.tagName.toLowerCase() === 'input') {
      const placeholder = this.getAttributeValue(element, 'placeholder');
      const title = this.getAttributeValue(element, 'title');
      return placeholder || title || '';
    }
    
    return '';
  }

  private async generateAccessibleNameFixes(element: ParsedElement, context?: DocumentContext): Promise<QuickFix[]> {
    const fixes: QuickFix[] = [];
    const elementType = this.getElementTypeDescription(element);

    // Option 1: Add aria-label
    fixes.push({
      id: 'add-aria-label',
      title: 'Add aria-label',
      description: `Add aria-label attribute to provide an accessible name for this ${elementType}`,
      category: 'fix',
      edit: this.createAttributeInsertEdit(element, 'aria-label', `Describe ${elementType}`, context!),
      priority: 1,
      isPreferred: true,
      userInput: {
        type: 'text',
        prompt: `What does this ${elementType} do?`,
        placeholder: `Describe the purpose of this ${elementType}...`,
        validation: {
          required: true,
          minLength: 3,
          maxLength: 100
        }
      }
    });

    // Option 2: Add text content (for buttons)
    if (element.tagName.toLowerCase() === 'button' && !element.textContent?.trim()) {
      fixes.push({
        id: 'add-button-text',
        title: 'Add button text',
        description: 'Add descriptive text content to the button',
        category: 'fix',
        edit: this.createTextContentEdit(element, 'Button text', context!),
        priority: 2,
        userInput: {
          type: 'text',
          prompt: 'What text should appear on this button?',
          placeholder: 'Button label...',
          validation: {
            required: true,
            minLength: 2,
            maxLength: 50
          }
        }
      });
    }

    // Option 3: Reference existing label (for form elements)
    if (this.isFormElement(element) && context) {
      const nearbyLabels = this.findNearbyLabels(element, context);
      for (const [index, labelId] of nearbyLabels.entries()) {
        fixes.push({
          id: `reference-label-${index}`,
          title: `Reference label "${labelId}"`,
          description: 'Reference the existing label element',
          category: 'fix',
          edit: this.createAttributeInsertEdit(element, 'aria-labelledby', labelId, context),
          priority: 3 + index
        });
      }
    }

    return fixes;
  }

  private async generateImproveNameFixes(element: ParsedElement, context?: DocumentContext): Promise<QuickFix[]> {
    const fixes: QuickFix[] = [];
    const currentName = this.getCurrentAccessibleName(element);
    const elementType = this.getElementTypeDescription(element);

    // Option 1: Improve aria-label
    if (this.hasAriaLabel(element)) {
      fixes.push({
        id: 'improve-aria-label',
        title: 'Improve aria-label',
        description: 'Replace with more specific description',
        category: 'improve',
        edit: this.createAttributeReplaceEdit(element, 'aria-label', `Improved ${elementType}`, context!),
        priority: 1,
        isPreferred: true,
        userInput: {
          type: 'text',
          prompt: `Provide a more specific description for this ${elementType}:`,
          placeholder: currentName,
          validation: {
            required: true,
            minLength: 5,
            maxLength: 100
          }
        }
      });
    }

    // Option 2: Improve text content
    if (element.textContent?.trim() && element.tagName.toLowerCase() === 'button') {
      fixes.push({
        id: 'improve-button-text',
        title: 'Improve button text',
        description: 'Make button text more specific',
        category: 'improve',
        edit: this.createTextContentReplaceEdit(element, `Specific ${elementType}`, context!),
        priority: 2,
        userInput: {
          type: 'text',
          prompt: 'What specific action does this button perform?',
          placeholder: currentName,
          validation: {
            required: true,
            minLength: 3,
            maxLength: 50
          }
        }
      });
    }

    return fixes;
  }

  private createTextContentEdit(
    element: ParsedElement,
    textContent: string,
    context: DocumentContext
  ): vscode.WorkspaceEdit {
    const edit = new vscode.WorkspaceEdit();
    
    // Find the position between opening and closing tags
    const elementText = this.getElementText(element, context);
    const openingTagEnd = elementText.indexOf('>') + 1;
    
    if (openingTagEnd > 0) {
      const insertPos = element.range.start.translate(0, openingTagEnd);
      edit.set(context.uri, [
        new vscode.TextEdit(new vscode.Range(insertPos, insertPos), textContent)
      ]);
    }
    
    return edit;
  }

  private createTextContentReplaceEdit(
    element: ParsedElement,
    newTextContent: string,
    context: DocumentContext
  ): vscode.WorkspaceEdit {
    const edit = new vscode.WorkspaceEdit();
    
    if (element.textContent?.trim()) {
      // Find the text content position within the element
      const elementText = this.getElementText(element, context);
      const textStart = elementText.indexOf(element.textContent.trim());
      
      if (textStart >= 0) {
        const replaceStart = element.range.start.translate(0, textStart);
        const replaceEnd = element.range.start.translate(0, textStart + element.textContent.trim().length);
        
        edit.set(context.uri, [
          new vscode.TextEdit(new vscode.Range(replaceStart, replaceEnd), newTextContent)
        ]);
      }
    }
    
    return edit;
  }

  private createAttributeReplaceEdit(
    element: ParsedElement,
    attributeName: string,
    newValue: string,
    context: DocumentContext
  ): vscode.WorkspaceEdit {
    const edit = new vscode.WorkspaceEdit();
    
    const elementText = this.getElementText(element, context);
    const attrPattern = new RegExp(`${attributeName}\\s*=\\s*["']([^"']*?)["']`);
    const attrMatch = elementText.match(attrPattern);
    
    if (attrMatch) {
      const valueStart = elementText.indexOf(attrMatch[1], elementText.indexOf(attrMatch[0]));
      const replaceStart = element.range.start.translate(0, valueStart);
      const replaceEnd = element.range.start.translate(0, valueStart + attrMatch[1].length);
      
      edit.set(context.uri, [
        new vscode.TextEdit(new vscode.Range(replaceStart, replaceEnd), newValue)
      ]);
    }
    
    return edit;
  }

  private getElementText(element: ParsedElement, context: DocumentContext): string {
    const startLine = element.range.start.line;
    const endLine = element.range.end.line;
    const lines = context.text.split('\n');
    
    if (startLine === endLine) {
      return lines[startLine].substring(element.range.start.character, element.range.end.character);
    } else {
      const result = [lines[startLine].substring(element.range.start.character)];
      for (let i = startLine + 1; i < endLine; i++) {
        result.push(lines[i]);
      }
      result.push(lines[endLine].substring(0, element.range.end.character));
      return result.join('\n');
    }
  }

  private findNearbyLabels(element: ParsedElement, context: DocumentContext): string[] {
    if (!context.elements) return [];
    
    const labels = this.findElementsWithTag(context, 'label');
    const nearbyLabels: string[] = [];
    
    for (const label of labels) {
      const id = this.getAttributeValue(label, 'id');
      if (id && this.isNearby(element, label)) {
        nearbyLabels.push(id);
      }
    }
    
    return nearbyLabels;
  }

  private isNearby(element1: ParsedElement, element2: ParsedElement): boolean {
    const lineDiff = Math.abs(element1.range.start.line - element2.range.start.line);
    return lineDiff <= 3; // Consider elements within 3 lines as nearby
  }

  private getElementTypeDescription(element: ParsedElement): string {
    const tagName = element.tagName.toLowerCase();
    const type = this.getAttributeValue(element, 'type');
    const role = this.getAttributeValue(element, 'role');
    
    if (tagName === 'input' && type) {
      return `${type} input`;
    } else if (role) {
      return role;
    } else {
      return tagName;
    }
  }
}