import * as vscode from 'vscode';
import { parseTemplate } from '@angular/compiler';
import { BaseParser } from './base';
import { DocumentContext, ParsedElement } from '../types';

export class AngularParser extends BaseParser {
  readonly framework = 'angular' as const;
  readonly supportedExtensions = ['.html', '.ts'];

  canParse(document: vscode.TextDocument): boolean {
    if (!super.canParse(document)) {
      return false;
    }
    
    const text = document.getText();
    return this.isAngularFile(text, document);
  }

  async parse(document: vscode.TextDocument): Promise<DocumentContext> {
    const context = this.createBaseContext(document);
    
    try {
      if (document.languageId === 'html') {
        // Parse Angular template file
        context.elements = await this.parseTemplate(document);
      } else if (document.languageId === 'typescript') {
        // Parse inline templates in TypeScript component files
        context.elements = await this.parseInlineTemplates(document);
      }
      
    } catch (error) {
      console.error('Failed to parse Angular document:', error);
      context.elements = this.fallbackParse(document);
    }

    return context;
  }

  private isAngularFile(text: string, document: vscode.TextDocument): boolean {
    // For HTML files, look for Angular template syntax
    if (document.languageId === 'html') {
      const angularPatterns = [
        /\*ngFor/,
        /\*ngIf/,
        /\[(ngModel|value|checked)\]/,
        /\(click|change|submit\)\)=/,
        /{{.*}}/,
        /\[attr\./,
        /\[class\./,
        /\[style\./
      ];
      
      return angularPatterns.some(pattern => pattern.test(text));
    }
    
    // For TypeScript files, look for Angular component decorators
    if (document.languageId === 'typescript') {
      const componentPatterns = [
        /@Component/,
        /from\s+['"]@angular\/core['"]/,
        /template\s*:/,
        /templateUrl\s*:/
      ];
      
      return componentPatterns.some(pattern => pattern.test(text));
    }
    
    return false;
  }

  private async parseTemplate(document: vscode.TextDocument): Promise<ParsedElement[]> {
    const template = document.getText();
    
    try {
      const parsed = parseTemplate(template, document.uri.fsPath, {
        preserveWhitespaces: true,
        interpolationConfig: { start: '{{', end: '}}' }
      });

      if (parsed.errors && parsed.errors.length > 0) {
        console.warn('Angular template parsing errors:', parsed.errors);
      }

      return this.convertAngularNodesToElements(parsed.nodes, document);
    } catch (error) {
      console.error('Angular template parsing failed:', error);
      return this.fallbackParse(document);
    }
  }

  private async parseInlineTemplates(document: vscode.TextDocument): Promise<ParsedElement[]> {
    const elements: ParsedElement[] = [];
    const text = document.getText();
    
    // Find template strings in @Component decorators
    const templateRegex = /template\s*:\s*`([^`]+)`/g;
    let match;
    
    while ((match = templateRegex.exec(text)) !== null) {
      const templateContent = match[1];
      const templateStart = match.index + match[0].indexOf('`') + 1;
      
      try {
        const parsed = parseTemplate(templateContent, document.uri.fsPath + '#inline', {
          preserveWhitespaces: true,
          interpolationConfig: { start: '{{', end: '}}' }
        });

        const templateElements = this.convertAngularNodesToElements(parsed.nodes, document, templateStart);
        elements.push(...templateElements);
      } catch (error) {
        console.error('Failed to parse inline Angular template:', error);
      }
    }
    
    return elements;
  }

  private convertAngularNodesToElements(
    nodes: any[], 
    document: vscode.TextDocument, 
    offset: number = 0
  ): ParsedElement[] {
    const elements: ParsedElement[] = [];
    
    for (const node of nodes) {
      const element = this.convertAngularNodeToElement(node, document, offset);
      if (element) {
        elements.push(element);
      }
    }
    
    return elements;
  }

  private convertAngularNodeToElement(
    node: any, 
    document: vscode.TextDocument, 
    offset: number = 0
  ): ParsedElement | null {
    if (!node) return null;
    
    try {
      if (node.type === 'Element') {
        return this.parseElementNode(node, document, offset);
      } else if (node.type === 'Template') {
        return this.parseTemplateNode(node, document, offset);
      }
      
      return null;
    } catch (error) {
      console.error('Error converting Angular node:', error);
      return null;
    }
  }

  private parseElementNode(node: any, document: vscode.TextDocument, offset: number): ParsedElement {
    const startPos = this.getPositionFromSourceSpan(node.sourceSpan, offset);
    const endPos = this.getPositionFromSourceSpan(node.endSourceSpan || node.sourceSpan, offset);
    
    const element: ParsedElement = {
      type: 'Element',
      tagName: node.name,
      attributes: this.parseAngularAttributes(node.attributes, node.inputs, node.outputs),
      textContent: this.extractAngularTextContent(node),
      children: [],
      range: new vscode.Range(startPos, endPos),
      framework: 'angular'
    };

    // Parse children
    if (node.children) {
      element.children = node.children
        .map((child: any) => this.convertAngularNodeToElement(child, document, offset))
        .filter((child: ParsedElement | null) => child !== null);
    }

    return element;
  }

  private parseTemplateNode(node: any, document: vscode.TextDocument, offset: number): ParsedElement {
    const startPos = this.getPositionFromSourceSpan(node.sourceSpan, offset);
    const endPos = this.getPositionFromSourceSpan(node.sourceSpan, offset);
    
    const element: ParsedElement = {
      type: 'Template',
      tagName: 'ng-template',
      attributes: this.parseAngularAttributes(node.attributes, node.inputs, node.outputs),
      textContent: '',
      children: [],
      range: new vscode.Range(startPos, endPos),
      framework: 'angular'
    };

    // Parse template variables and directives
    if (node.variables) {
      for (const variable of node.variables) {
        element.attributes[`*${variable.name}`] = variable.value || '';
      }
    }

    if (node.templateAttrs) {
      for (const attr of node.templateAttrs) {
        element.attributes[attr.name] = attr.value || '';
      }
    }

    // Parse children
    if (node.children) {
      element.children = node.children
        .map((child: any) => this.convertAngularNodeToElement(child, document, offset))
        .filter((child: ParsedElement | null) => child !== null);
    }

    return element;
  }

  private parseAngularAttributes(
    attributes: any[] = [], 
    inputs: any[] = [], 
    outputs: any[] = []
  ): Record<string, string | undefined> {
    const attrs: Record<string, string | undefined> = {};
    
    // Regular attributes
    for (const attr of attributes) {
      attrs[attr.name] = attr.value;
    }
    
    // Property bindings
    for (const input of inputs) {
      const bindingName = `[${input.name}]`;
      attrs[bindingName] = this.getAngularBindingValue(input.value);
    }
    
    // Event bindings
    for (const output of outputs) {
      const eventName = `(${output.name})`;
      attrs[eventName] = this.getAngularBindingValue(output.handler);
    }
    
    return attrs;
  }

  private getAngularBindingValue(value: any): string {
    if (!value) return '';
    
    if (typeof value === 'string') {
      return value;
    }
    
    if (value.ast) {
      return this.stringifyAngularAST(value.ast);
    }
    
    return value.toString();
  }

  private stringifyAngularAST(ast: any): string {
    if (!ast) return '';
    
    switch (ast.constructor.name) {
      case 'LiteralPrimitive':
        return String(ast.value);
      case 'PropertyRead':
        return ast.name;
      case 'MethodCall':
        return `${ast.receiver ? this.stringifyAngularAST(ast.receiver) + '.' : ''}${ast.name}()`;
      case 'Binary':
        return `${this.stringifyAngularAST(ast.left)} ${ast.operation} ${this.stringifyAngularAST(ast.right)}`;
      case 'Interpolation':
        return ast.expressions.map((expr: any) => `{{${this.stringifyAngularAST(expr)}}}`).join('');
      default:
        return ast.toString();
    }
  }

  private extractAngularTextContent(node: any): string {
    if (node.type === 'Text') {
      return node.value;
    }
    
    if (node.children) {
      return node.children
        .filter((child: any) => child.type === 'Text')
        .map((child: any) => child.value)
        .join('');
    }
    
    return '';
  }

  private getPositionFromSourceSpan(sourceSpan: any, offset: number): vscode.Position {
    if (sourceSpan && sourceSpan.start) {
      const line = sourceSpan.start.line;
      const character = sourceSpan.start.col + offset;
      return new vscode.Position(line, character);
    }
    
    return new vscode.Position(0, 0);
  }

  private fallbackParse(document: vscode.TextDocument): ParsedElement[] {
    const elements: ParsedElement[] = [];
    const text = document.getText();
    const lines = text.split('\n');

    // Basic regex patterns for Angular template syntax
    const patterns = [
      // Standard HTML elements with Angular directives
      /<([a-zA-Z][a-zA-Z0-9-]*)\s*([^>]*?)>/g,
      // Angular structural directives
      /<ng-container\s*([^>]*?)>/g,
      /<ng-template\s*([^>]*?)>/g
    ];

    for (let lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      const line = lines[lineIndex];

      for (const pattern of patterns) {
        let match;
        while ((match = pattern.exec(line)) !== null) {
          const tagName = match[1] || 'ng-container';
          const attributesStr = match[2] || '';
          const startPos = match.index;
          const endPos = match.index + match[0].length;

          elements.push({
            type: 'Element',
            tagName,
            attributes: this.parseAngularAttributesFromString(attributesStr),
            textContent: '',
            children: [],
            range: this.createRange(lineIndex, startPos, lineIndex, endPos),
            framework: 'angular'
          });
        }
      }
    }

    return elements;
  }

  private parseAngularAttributesFromString(attributesStr: string): Record<string, string | undefined> {
    const attributes: Record<string, string | undefined> = {};
    
    // Angular-specific attribute patterns
    const patterns = [
      // Property binding: [property]="value"
      /\[([^\]]+)\]="([^"]*?)"/g,
      // Event binding: (event)="handler"
      /\(([^)]+)\)="([^"]*?)"/g,
      // Two-way binding: [(ngModel)]="value"
      /\[\(([^)]+)\)\]="([^"]*?)"/g,
      // Structural directive: *ngFor="let item of items"
      /\*([^=\s]+)="([^"]*?)"/g,
      // Regular attributes: attr="value"
      /([a-zA-Z][a-zA-Z0-9-]*?)="([^"]*?)"/g,
      // Boolean attributes: disabled
      /\b([a-zA-Z][a-zA-Z0-9-]*?)\b(?!=)/g
    ];

    for (const pattern of patterns) {
      let match;
      while ((match = pattern.exec(attributesStr)) !== null) {
        const name = match[1];
        const value = match[2] || '';
        
        // Format the attribute name based on the pattern
        if (pattern.source.includes('\\[') && pattern.source.includes('\\]')) {
          attributes[`[${name}]`] = value;
        } else if (pattern.source.includes('\\(') && pattern.source.includes('\\)')) {
          attributes[`(${name})`] = value;
        } else if (pattern.source.includes('\\*')) {
          attributes[`*${name}`] = value;
        } else {
          attributes[name] = value;
        }
      }
    }

    return attributes;
  }
}