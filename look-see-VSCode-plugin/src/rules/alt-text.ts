import * as vscode from 'vscode';
import { BaseRule } from './base';
import { AccessibilityViolation, DocumentContext, ParsedElement, QuickFix, UserInputPrompt } from '../types';

export class AltTextRule extends BaseRule {
  readonly id = 'alt-text';
  readonly name = 'Alt Text Required';
  readonly description = 'Images must have alternative text that serves the equivalent purpose';
  readonly severity = 'error' as const;
  readonly frameworks = ['react', 'angular', 'vue', 'html'] as const;
  readonly wcagReference = {
    level: 'A' as const,
    criterion: '1.1.1',
    url: 'https://www.w3.org/WAI/WCAG22/Understanding/non-text-content.html',
    title: 'Non-text Content'
  };

  private decorativeImagePatterns = [
    /background/i,
    /decoration/i,
    /ornament/i,
    /divider/i,
    /spacer/i,
    /separator/i,
    /border/i,
    /frame/i
  ];

  private genericAltTextPatterns = [
    /^image$/i,
    /^picture$/i,
    /^photo$/i,
    /^graphic$/i,
    /^img$/i,
    /^icon$/i,
    /^\d+\.(jpg|jpeg|png|gif|svg)$/i
  ];

  async check(context: DocumentContext): Promise<AccessibilityViolation[]> {
    const violations: AccessibilityViolation[] = [];
    const images = this.findElementsWithTag(context, 'img');

    for (const image of images) {
      const altAttribute = this.getAttributeValue(image, 'alt');
      
      if (altAttribute === undefined) {
        // Missing alt attribute
        violations.push(this.createViolation(
          image,
          'Image is missing alt attribute. Add alt="" for decorative images or provide descriptive text.',
          context,
          await this.generateMissingAltFixes(image, context)
        ));
      } else if (altAttribute && this.isGenericAltText(altAttribute)) {
        // Generic/meaningless alt text
        violations.push(this.createViolation(
          image,
          `Alt text "${altAttribute}" is too generic. Provide more descriptive alternative text.`,
          context,
          await this.generateGenericAltFixes(image, context)
        ));
      }
    }

    return violations;
  }

  protected getAffectedUserTypes(): string[] {
    return ['screen reader users', 'users with slow internet connections', 'users with images disabled'];
  }

  async generateFixes(violation: AccessibilityViolation): Promise<QuickFix[]> {
    if (!violation.element) return [];

    const altValue = this.getAttributeValue(violation.element, 'alt');
    
    if (altValue === undefined) {
      return this.generateMissingAltFixes(violation.element, violation.context);
    } else {
      return this.generateGenericAltFixes(violation.element, violation.context);
    }
  }

  private async generateMissingAltFixes(element: ParsedElement, context?: DocumentContext): Promise<QuickFix[]> {
    const fixes: QuickFix[] = [];

    // Option 1: Mark as decorative
    fixes.push({
      id: 'add-empty-alt',
      title: 'Mark as decorative image',
      description: 'Add empty alt attribute to indicate this image is decorative',
      category: 'fix',
      edit: this.createAttributeInsertEdit(element, 'alt', '', context!),
      priority: 1,
      isPreferred: this.isLikelyDecorative(element)
    });

    // Option 2: Add descriptive alt text
    fixes.push({
      id: 'add-descriptive-alt',
      title: 'Add descriptive alt text',
      description: 'Add alternative text that describes the image content',
      category: 'fix',
      edit: this.createAttributeInsertEdit(element, 'alt', 'Describe this image', context!),
      priority: 2,
      isPreferred: !this.isLikelyDecorative(element),
      userInput: {
        type: 'text',
        prompt: 'Enter a description for this image:',
        placeholder: 'Brief description of the image content...',
        validation: {
          required: true,
          minLength: 3,
          maxLength: 250
        }
      }
    });

    // Option 3: Context-based suggestions
    if (context) {
      const suggestions = this.suggestAltTextFromContext(element, context);
      for (const [index, suggestion] of suggestions.entries()) {
        fixes.push({
          id: `add-suggested-alt-${index}`,
          title: `Use: "${suggestion}"`,
          description: 'Add descriptive alternative text for this content image',
          category: 'fix',
          edit: this.createAttributeInsertEdit(element, 'alt', suggestion, context),
          priority: 3 + index
        });
      }
    }

    return fixes;
  }

  private async generateGenericAltFixes(element: ParsedElement, context?: DocumentContext): Promise<QuickFix[]> {
    const fixes: QuickFix[] = [];
    const currentAlt = this.getAttributeValue(element, 'alt') || '';

    // Option 1: Improve current alt text
    fixes.push({
      id: 'improve-alt-text',
      title: 'Improve alt text',
      description: 'Replace with more specific description',
      category: 'improve',
      edit: this.createAttributeReplaceEdit(element, 'alt', 'Describe this image specifically', context!),
      priority: 1,
      isPreferred: true,
      userInput: {
        type: 'text',
        prompt: 'Enter a more specific description:',
        placeholder: currentAlt,
        validation: {
          required: true,
          minLength: 5,
          maxLength: 250
        }
      }
    });

    // Option 2: Mark as decorative if likely decorative
    if (this.isLikelyDecorative(element)) {
      fixes.push({
        id: 'mark-decorative',
        title: 'Mark as decorative',
        description: 'Keep empty alt text to mark as decorative',
        category: 'alternative',
        edit: this.createAttributeReplaceEdit(element, 'alt', '', context!),
        priority: 2
      });
    }

    // Option 3: Context-based suggestions
    if (context) {
      const suggestions = this.suggestAltTextFromContext(element, context);
      for (const [index, suggestion] of suggestions.entries()) {
        fixes.push({
          id: `replace-with-suggestion-${index}`,
          title: `Use: "${suggestion}"`,
          description: 'Replace with contextual description',
          category: 'improve',
          edit: this.createAttributeReplaceEdit(element, 'alt', suggestion, context),
          priority: 3 + index
        });
      }
    }

    return fixes;
  }

  private createAttributeReplaceEdit(
    element: ParsedElement,
    attributeName: string,
    newValue: string,
    context: DocumentContext
  ): vscode.WorkspaceEdit {
    const edit = new vscode.WorkspaceEdit();
    
    // Find the current alt attribute position
    const elementStart = element.range.start;
    const elementEnd = element.range.end;
    const elementText = context.text.split('\n').slice(elementStart.line, elementEnd.line + 1).join('\n');
    
    // Find alt attribute pattern
    const altMatch = elementText.match(/alt\s*=\s*["']([^"']*?)["']/);
    if (altMatch) {
      const matchStart = elementText.indexOf(altMatch[0]);
      const valueStart = elementText.indexOf(altMatch[1], matchStart);
      
      const replaceStart = new vscode.Position(
        elementStart.line,
        elementStart.character + valueStart
      );
      const replaceEnd = new vscode.Position(
        elementStart.line,
        elementStart.character + valueStart + altMatch[1].length
      );
      
      edit.set(context.uri, [
        new vscode.TextEdit(new vscode.Range(replaceStart, replaceEnd), newValue)
      ]);
    }
    
    return edit;
  }

  private isLikelyDecorative(element: ParsedElement): boolean {
    const src = this.getAttributeValue(element, 'src') || '';
    const className = this.getAttributeValue(element, 'class') || '';
    const id = this.getAttributeValue(element, 'id') || '';
    
    // Check filename patterns
    if (this.decorativeImagePatterns.some(pattern => pattern.test(src))) {
      return true;
    }
    
    // Check class names
    if (/\b(decoration|ornament|divider|spacer|bg|background)\b/i.test(className)) {
      return true;
    }
    
    // Check ID
    if (/\b(decoration|ornament|divider|spacer|bg|background)\b/i.test(id)) {
      return true;
    }
    
    // Check parent context
    const role = this.getAttributeValue(element, 'role');
    if (role === 'presentation' || role === 'none') {
      return true;
    }
    
    return false;
  }

  protected isGenericAltText(altText: string): boolean {
    return this.genericAltTextPatterns.some(pattern => pattern.test(altText.trim()));
  }
}