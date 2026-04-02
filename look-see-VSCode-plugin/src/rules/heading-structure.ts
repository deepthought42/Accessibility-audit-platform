import * as vscode from 'vscode';
import { BaseRule } from './base';
import { AccessibilityViolation, DocumentContext, ParsedElement, QuickFix } from '../types';

interface DocumentHeading {
  element: ParsedElement;
  level: number;
  text: string;
  line: number;
}

export class HeadingStructureRule extends BaseRule {
  readonly id = 'heading-structure';
  readonly name = 'Heading Structure';
  readonly description = 'Headings must be used to convey document structure';
  readonly severity = 'warning' as const;
  readonly frameworks = ['react', 'angular', 'vue', 'html'] as const;
  readonly wcagReference = {
    level: 'AA' as const,
    criterion: '1.3.1',
    url: 'https://www.w3.org/WAI/WCAG22/Understanding/info-and-relationships.html',
    title: 'Info and Relationships'
  };

  async check(context: DocumentContext): Promise<AccessibilityViolation[]> {
    const violations: AccessibilityViolation[] = [];
    
    if (!context.elements) return violations;

    const headings = this.extractHeadings(context.elements);
    
    if (headings.length === 0) return violations;

    // Sort headings by line number
    headings.sort((a, b) => a.line - b.line);

    // Check for various heading structure issues
    violations.push(...this.checkSkippedLevels(headings, context));
    violations.push(...this.checkMissingH1(headings, context));
    violations.push(...this.checkMultipleH1s(headings, context));
    violations.push(...this.checkEmptyHeadings(headings, context));
    violations.push(...this.checkHeadingsUsedForStyling(headings, context));

    return violations;
  }

  protected getAffectedUserTypes(): string[] {
    return ['screen reader users', 'keyboard navigation users', 'users with cognitive disabilities'];
  }

  async generateFixes(violation: AccessibilityViolation): Promise<QuickFix[]> {
    if (!violation.element) return [];

    const violationId = violation.id.split('-').pop();
    
    switch (violationId) {
      case 'skipped':
        return this.generateSkippedLevelFixes(violation.element, violation.context);
      case 'empty':
        return this.generateEmptyHeadingFixes(violation.element, violation.context);
      case 'styling':
        return this.generateStylingToStructureFixes(violation.element, violation.context);
      default:
        return [];
    }
  }

  private extractHeadings(elements: ParsedElement[]): DocumentHeading[] {
    const headings: DocumentHeading[] = [];
    
    this.findElementsRecursive(elements, element => this.isHeadingElement(element))
      .forEach(element => {
        const level = this.getHeadingLevel(element);
        if (level) {
          headings.push({
            element,
            level,
            text: element.textContent?.trim() || '',
            line: element.range.start.line
          });
        }
      });
    
    return headings;
  }

  private checkSkippedLevels(headings: DocumentHeading[], context: DocumentContext): AccessibilityViolation[] {
    const violations: AccessibilityViolation[] = [];
    
    for (let i = 1; i < headings.length; i++) {
      const current = headings[i];
      const previous = headings[i - 1];
      
      if (current.level > previous.level + 1) {
        violations.push(this.createViolation(
          current.element,
          `Heading level ${current.level} follows level ${previous.level}, skipping intermediate levels. Use sequential heading levels.`,
          context,
          this.generateSkippedLevelFixes(current.element, context)
        ));
      }
    }
    
    return violations;
  }

  private checkMissingH1(headings: DocumentHeading[], context: DocumentContext): AccessibilityViolation[] {
    const violations: AccessibilityViolation[] = [];
    const hasH1 = headings.some(h => h.level === 1);
    
    if (!hasH1 && headings.length > 0) {
      const firstHeading = headings[0];
      violations.push(this.createViolation(
        firstHeading.element,
        'Document is missing a main heading (h1). The first heading should typically be h1.',
        context,
        [this.createHeadingLevelChangeFix('change-to-h1', 'Change to h1', firstHeading.element, 1, context)]
      ));
    }
    
    return violations;
  }

  private checkMultipleH1s(headings: DocumentHeading[], context: DocumentContext): AccessibilityViolation[] {
    const violations: AccessibilityViolation[] = [];
    const h1Headings = headings.filter(h => h.level === 1);
    
    if (h1Headings.length > 1) {
      for (let i = 1; i < h1Headings.length; i++) {
        const heading = h1Headings[i];
        violations.push(this.createViolation(
          heading.element,
          'Multiple h1 headings found. Use only one h1 per page for the main heading.',
          context,
          [
            this.createHeadingLevelChangeFix('change-to-h2', 'Change to h2', heading.element, 2, context),
            this.createHeadingLevelChangeFix('change-to-h3', 'Change to h3', heading.element, 3, context)
          ]
        ));
      }
    }
    
    return violations;
  }

  private checkEmptyHeadings(headings: DocumentHeading[], context: DocumentContext): AccessibilityViolation[] {
    const violations: AccessibilityViolation[] = [];
    
    for (const heading of headings) {
      if (!heading.text || heading.text.trim().length === 0) {
        violations.push(this.createViolation(
          heading.element,
          `Heading h${heading.level} is empty. Provide descriptive heading text.`,
          context,
          this.generateEmptyHeadingFixes(heading.element, context)
        ));
      }
    }
    
    return violations;
  }

  private checkHeadingsUsedForStyling(headings: DocumentHeading[], context: DocumentContext): AccessibilityViolation[] {
    const violations: AccessibilityViolation[] = [];
    
    for (const heading of headings) {
      if (this.appearsToBeUsedForStyling(heading)) {
        violations.push(this.createViolation(
          heading.element,
          `Heading appears to be used for styling rather than structure. Use CSS for visual formatting.`,
          context,
          this.generateStylingToStructureFixes(heading.element, context)
        ));
      }
    }
    
    return violations;
  }

  private appearsToBeUsedForStyling(heading: DocumentHeading): boolean {
    const stylingPatterns = [
      /^(bold|large|big|small|tiny)$/i,
      /^(red|blue|green|yellow|black|white)$/i,
      /^(left|center|right|justify)$/i,
      /^(\s*[-=]{3,}\s*)$/,  // ASCII decorations
      /^(\s*[*#]{3,}\s*)$/,  // Markdown-style decorations
      /^\s*\.{3,}\s*$/       // Dots
    ];
    
    return stylingPatterns.some(pattern => pattern.test(heading.text.trim()));
  }

  private generateSkippedLevelFixes(element: ParsedElement, context?: DocumentContext): QuickFix[] {
    const fixes: QuickFix[] = [];
    const currentLevel = this.getHeadingLevel(element);
    
    if (!currentLevel || !context) return fixes;

    // Suggest changing to appropriate levels
    for (let level = Math.max(1, currentLevel - 2); level < currentLevel; level++) {
      fixes.push(this.createHeadingLevelChangeFix(
        `change-to-h${level}`,
        `Change to h${level}`,
        element,
        level,
        context,
        level === currentLevel - 1 // Prefer the immediate previous level
      ));
    }

    return fixes;
  }

  private generateEmptyHeadingFixes(element: ParsedElement, context?: DocumentContext): QuickFix[] {
    if (!context) return [];

    const headingLevel = this.getHeadingLevel(element);
    
    return [{
      id: 'add-heading-text',
      title: 'Add heading text',
      description: `Add descriptive text to the h${headingLevel} heading`,
      category: 'fix',
      edit: this.createTextContentEdit(element, 'Heading text', context),
      priority: 1,
      isPreferred: true,
      userInput: {
        type: 'text',
        prompt: `What should this h${headingLevel} heading say?`,
        placeholder: 'Descriptive heading text...',
        validation: {
          required: true,
          minLength: 3,
          maxLength: 100
        }
      }
    }];
  }

  private generateStylingToStructureFixes(element: ParsedElement, context?: DocumentContext): QuickFix[] {
    if (!context) return [];

    const fixes: QuickFix[] = [];

    // Option 1: Change to paragraph
    fixes.push({
      id: 'change-to-paragraph',
      title: 'Change to paragraph',
      description: 'Change this heading to a paragraph with appropriate styling',
      category: 'fix',
      edit: this.createElementTagChangeFix(element, 'p', context),
      priority: 1,
      isPreferred: true
    });

    // Option 2: Change to strong emphasis
    fixes.push({
      id: 'change-to-strong',
      title: 'Change to <strong>',
      description: 'Change to <strong> for emphasis without heading semantics',
      category: 'alternative',
      edit: this.createElementTagChangeFix(element, 'strong', context),
      priority: 2
    });

    return fixes;
  }

  private createHeadingLevelChangeFix(
    id: string,
    title: string,
    element: ParsedElement,
    newLevel: number,
    context: DocumentContext,
    isPreferred: boolean = false
  ): QuickFix {
    return {
      id,
      title,
      description: `Change heading to level ${newLevel}`,
      category: 'fix',
      edit: this.createElementTagChangeFix(element, `h${newLevel}`, context),
      priority: isPreferred ? 1 : 2,
      isPreferred
    };
  }

  private createTextContentEdit(
    element: ParsedElement,
    textContent: string,
    context: DocumentContext
  ): vscode.WorkspaceEdit {
    const edit = new vscode.WorkspaceEdit();
    
    // Find the position between opening and closing tags
    const elementText = this.getElementText(element, context);
    const openingTagMatch = elementText.match(/^<[^>]+>/);
    
    if (openingTagMatch) {
      const insertPos = element.range.start.translate(0, openingTagMatch[0].length);
      edit.set(context.uri, [
        new vscode.TextEdit(new vscode.Range(insertPos, insertPos), textContent)
      ]);
    }
    
    return edit;
  }

  private createElementTagChangeFix(
    element: ParsedElement,
    newTag: string,
    context: DocumentContext
  ): vscode.WorkspaceEdit {
    const edit = new vscode.WorkspaceEdit();
    const elementText = this.getElementText(element, context);
    
    // Replace opening tag
    const openingTagMatch = elementText.match(/^<([^>\s]+)([^>]*)>/);
    if (openingTagMatch) {
      const openingTagEnd = element.range.start.translate(0, openingTagMatch[0].length);
      edit.set(context.uri, [
        new vscode.TextEdit(
          new vscode.Range(element.range.start, openingTagEnd),
          `<${newTag}${openingTagMatch[2]}>`
        )
      ]);
    }
    
    // Replace closing tag if it exists
    const closingTagMatch = elementText.match(/<\/([^>]+)>$/);
    if (closingTagMatch) {
      const closingTagStart = element.range.end.translate(0, -closingTagMatch[0].length);
      edit.set(context.uri, [
        new vscode.TextEdit(
          new vscode.Range(closingTagStart, element.range.end),
          `</${newTag}>`
        )
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
}