import * as vscode from 'vscode';
import { AccessibilityRule, AccessibilityViolation, DocumentContext, ParsedElement, QuickFix, ViolationSeverity, WcagReference, FrameworkType } from '../types';

export abstract class BaseRule implements AccessibilityRule {
  abstract readonly id: string;
  abstract readonly name: string;
  abstract readonly description: string;
  abstract readonly severity: ViolationSeverity;
  abstract readonly wcagReference: WcagReference;
  abstract readonly frameworks: readonly FrameworkType[];

  abstract check(context: DocumentContext): Promise<AccessibilityViolation[]>;

  async generateFixes?(violation: AccessibilityViolation): Promise<QuickFix[]> {
    return [];
  }

  protected createViolation(
    element: ParsedElement,
    message: string,
    context: DocumentContext,
    fixes?: QuickFix[]
  ): AccessibilityViolation {
    return {
      id: `${this.id}-${Date.now()}`,
      ruleId: this.id,
      message,
      severity: this.severity,
      range: element.range,
      element,
      context,
      wcagReference: this.wcagReference,
      impact: this.getImpactDescription(),
      fixes: fixes || []
    };
  }

  protected getImpactDescription(): string {
    return `This violation affects users with disabilities who rely on ${this.getAffectedUserTypes().join(', ')}.`;
  }

  protected abstract getAffectedUserTypes(): string[];

  protected hasAttribute(element: ParsedElement, attrName: string): boolean {
    return element.attributes.hasOwnProperty(attrName);
  }

  protected getAttributeValue(element: ParsedElement, attrName: string): string | undefined {
    return element.attributes[attrName];
  }

  protected isEmptyAttribute(element: ParsedElement, attrName: string): boolean {
    const value = this.getAttributeValue(element, attrName);
    return !value || value.trim() === '';
  }

  protected isInteractiveElement(element: ParsedElement): boolean {
    const interactiveElements = [
      'a', 'button', 'input', 'select', 'textarea', 
      'audio', 'video', 'iframe', 'object', 'embed',
      'details', 'summary'
    ];
    return interactiveElements.includes(element.tagName.toLowerCase());
  }

  protected isFormElement(element: ParsedElement): boolean {
    const formElements = ['input', 'select', 'textarea', 'button', 'fieldset', 'legend'];
    return formElements.includes(element.tagName.toLowerCase());
  }

  protected isMediaElement(element: ParsedElement): boolean {
    const mediaElements = ['img', 'video', 'audio', 'canvas', 'svg', 'object', 'embed', 'iframe'];
    return mediaElements.includes(element.tagName.toLowerCase());
  }

  protected isHeadingElement(element: ParsedElement): boolean {
    return /^h[1-6]$/i.test(element.tagName);
  }

  protected getHeadingLevel(element: ParsedElement): number | null {
    const match = element.tagName.match(/^h([1-6])$/i);
    return match ? parseInt(match[1], 10) : null;
  }

  protected hasAriaLabel(element: ParsedElement): boolean {
    return this.hasAttribute(element, 'aria-label') && 
           !this.isEmptyAttribute(element, 'aria-label');
  }

  protected hasAriaLabelledBy(element: ParsedElement): boolean {
    return this.hasAttribute(element, 'aria-labelledby') && 
           !this.isEmptyAttribute(element, 'aria-labelledby');
  }

  protected hasAriaDescribedBy(element: ParsedElement): boolean {
    return this.hasAttribute(element, 'aria-describedby') && 
           !this.isEmptyAttribute(element, 'aria-describedby');
  }

  protected hasAccessibleName(element: ParsedElement): boolean {
    return this.hasAriaLabel(element) ||
           this.hasAriaLabelledBy(element) ||
           !!(element.textContent && element.textContent.trim().length > 0);
  }

  protected isDecorativeImage(element: ParsedElement): boolean {
    if (element.tagName.toLowerCase() !== 'img') return false;
    
    const alt = this.getAttributeValue(element, 'alt');
    const role = this.getAttributeValue(element, 'role');
    const ariaHidden = this.getAttributeValue(element, 'aria-hidden');
    
    return alt === '' || 
           role === 'presentation' || 
           role === 'none' || 
           ariaHidden === 'true';
  }

  protected createQuickFix(
    id: string,
    title: string,
    description: string,
    edit: vscode.WorkspaceEdit,
    priority: number = 0,
    isPreferred: boolean = false
  ): QuickFix {
    return {
      id,
      title,
      description,
      category: 'fix',
      edit,
      priority,
      isPreferred
    };
  }

  protected createTextEdit(range: vscode.Range, newText: string): vscode.TextEdit {
    return new vscode.TextEdit(range, newText);
  }

  protected createAttributeInsertEdit(
    element: ParsedElement, 
    attributeName: string, 
    attributeValue: string,
    context: DocumentContext
  ): vscode.WorkspaceEdit {
    const edit = new vscode.WorkspaceEdit();
    
    // Find the position to insert the attribute
    const elementText = context.text.substring(
      context.text.indexOf('\n', 0) * element.range.start.line + element.range.start.character,
      context.text.indexOf('\n', 0) * element.range.end.line + element.range.end.character
    );
    
    // Find the closing > or />
    const tagEndMatch = elementText.match(/(\s*\/?>)/);
    if (tagEndMatch) {
      const insertPos = element.range.start.translate(0, elementText.indexOf(tagEndMatch[1]));
      const attributeText = this.formatAttribute(attributeName, attributeValue, element.framework);
      
      edit.set(context.uri, [
        new vscode.TextEdit(new vscode.Range(insertPos, insertPos), ` ${attributeText}`)
      ]);
    }
    
    return edit;
  }

  protected formatAttribute(name: string, value: string, framework?: FrameworkType): string {
    switch (framework) {
      case 'react':
        // Handle JSX boolean attributes and expression syntax
        if (value === 'true' || value === 'false') {
          return value === 'true' ? name : `${name}={false}`;
        }
        return `${name}="${value}"`;
      
      case 'angular':
        // Handle Angular property binding
        if (name.startsWith('[') || name.startsWith('(') || name.startsWith('*')) {
          return `${name}="${value}"`;
        }
        return `${name}="${value}"`;
      
      case 'vue':
        // Handle Vue directives
        if (name.startsWith('v-') || name.startsWith(':') || name.startsWith('@')) {
          return `${name}="${value}"`;
        }
        return `${name}="${value}"`;
      
      default:
        return `${name}="${value}"`;
    }
  }

  protected findElementsWithTag(context: DocumentContext, tagName: string): ParsedElement[] {
    if (!context.elements) return [];
    
    return this.findElementsRecursive(context.elements, 
      element => element.tagName.toLowerCase() === tagName.toLowerCase()
    );
  }

  protected findElementsRecursive(
    elements: ParsedElement[], 
    predicate: (element: ParsedElement) => boolean
  ): ParsedElement[] {
    const results: ParsedElement[] = [];
    
    for (const element of elements) {
      if (predicate(element)) {
        results.push(element);
      }
      
      if (element.children && element.children.length > 0) {
        results.push(...this.findElementsRecursive(element.children, predicate));
      }
    }
    
    return results;
  }

  protected isGenericAltText(altText: string): boolean {
    const genericPatterns = [
      /^image$/i,
      /^picture$/i,
      /^photo$/i,
      /^graphic$/i,
      /^img$/i,
      /^icon$/i,
      /^logo$/i,
      /^banner$/i,
      /^placeholder$/i,
      /^untitled$/i,
      /^default$/i,
      /^\d+\.(jpg|jpeg|png|gif|svg)$/i,
      /^(image|img|photo)[\s_-]?\d*$/i
    ];
    
    return genericPatterns.some(pattern => pattern.test(altText.trim()));
  }

  protected suggestAltTextFromContext(element: ParsedElement, context: DocumentContext): string[] {
    const suggestions: string[] = [];
    
    // Look for nearby text content
    const parent = this.findParentElement(element, context);
    if (parent && parent.textContent) {
      const parentText = parent.textContent.trim();
      if (parentText && parentText.length < 100) {
        suggestions.push(parentText);
      }
    }
    
    // Look for src attribute patterns
    const src = this.getAttributeValue(element, 'src');
    if (src) {
      const filename = src.split('/').pop()?.replace(/\.[^.]+$/, '');
      if (filename && filename !== 'image' && filename !== 'img') {
        const readable = filename.replace(/[-_]/g, ' ').replace(/([a-z])([A-Z])/g, '$1 $2');
        suggestions.push(readable);
      }
    }
    
    return suggestions.slice(0, 3); // Limit to 3 suggestions
  }

  private findParentElement(element: ParsedElement, context: DocumentContext): ParsedElement | null {
    if (!context.elements) return null;
    
    return this.findParentRecursive(context.elements, element);
  }

  private findParentRecursive(elements: ParsedElement[], target: ParsedElement): ParsedElement | null {
    for (const element of elements) {
      if (element.children.includes(target)) {
        return element;
      }
      
      const found = this.findParentRecursive(element.children, target);
      if (found) return found;
    }
    
    return null;
  }
}