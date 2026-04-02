import * as vscode from 'vscode';
import { compileTemplate, parse } from '@vue/compiler-sfc';
import { BaseParser } from './base';
import { DocumentContext, ParsedElement } from '../types';

export class VueParser extends BaseParser {
  readonly framework = 'vue' as const;
  readonly supportedExtensions = ['.vue'];

  canParse(document: vscode.TextDocument): boolean {
    return super.canParse(document) && this.isVueFile(document.getText());
  }

  async parse(document: vscode.TextDocument): Promise<DocumentContext> {
    const context = this.createBaseContext(document);
    
    try {
      const { descriptor, errors } = parse(document.getText(), {
        filename: document.uri.fsPath
      });

      if (errors && errors.length > 0) {
        console.warn('Vue SFC parsing errors:', errors);
      }

      context.ast = descriptor;
      
      if (descriptor.template) {
        context.elements = await this.parseVueTemplate(descriptor.template, document);
      } else {
        context.elements = [];
      }
      
    } catch (error) {
      console.error('Failed to parse Vue SFC:', error);
      context.elements = this.fallbackParse(document);
    }

    return context;
  }

  private isVueFile(text: string): boolean {
    // Check for Vue SFC structure
    const vuePatterns = [
      /<template>/,
      /<script.*setup.*>/,
      /<script.*lang=["']ts["'].*>/,
      /<style.*scoped.*>/,
      /v-if=/,
      /v-for=/,
      /v-model=/,
      /@click=/
    ];

    return vuePatterns.some(pattern => pattern.test(text));
  }

  private async parseVueTemplate(templateBlock: any, document: vscode.TextDocument): Promise<ParsedElement[]> {
    if (!templateBlock || !templateBlock.content) {
      return [];
    }

    try {
      const { ast, code } = compileTemplate({
        id: document.uri.fsPath,
        source: templateBlock.content,
        filename: document.uri.fsPath,
        compilerOptions: {
          mode: 'module'
        }
      });

      if (ast) {
        return this.convertVueASTToElements(ast, document, templateBlock.loc?.start || { line: 1, column: 0 });
      }
      
      return this.parseVueTemplateContent(templateBlock.content, document, templateBlock.loc?.start || { line: 1, column: 0 });
    } catch (error) {
      console.error('Failed to compile Vue template:', error);
      return this.parseVueTemplateContent(templateBlock.content, document, templateBlock.loc?.start || { line: 1, column: 0 });
    }
  }

  private convertVueASTToElements(ast: any, document: vscode.TextDocument, offset: any): ParsedElement[] {
    if (!ast || !ast.children) {
      return [];
    }

    const elements: ParsedElement[] = [];
    
    for (const child of ast.children) {
      const element = this.convertVueNodeToElement(child, document, offset);
      if (element) {
        elements.push(element);
      }
    }

    return elements;
  }

  private convertVueNodeToElement(node: any, document: vscode.TextDocument, offset: any): ParsedElement | null {
    if (!node || node.type !== 1) { // ELEMENT type
      return null;
    }

    try {
      const startPos = this.getVuePosition(node.loc?.start, offset);
      const endPos = this.getVuePosition(node.loc?.end, offset);

      const element: ParsedElement = {
        type: 'VueElement',
        tagName: node.tag,
        attributes: this.parseVueAttributes(node.props),
        textContent: this.extractVueTextContent(node),
        children: [],
        range: new vscode.Range(startPos, endPos),
        framework: 'vue'
      };

      // Parse children
      if (node.children) {
        element.children = node.children
          .map((child: any) => this.convertVueNodeToElement(child, document, offset))
          .filter((child: ParsedElement | null) => child !== null);
      }

      return element;
    } catch (error) {
      console.error('Error converting Vue node:', error);
      return null;
    }
  }

  private parseVueAttributes(props: any[]): Record<string, string | undefined> {
    const attributes: Record<string, string | undefined> = {};
    
    if (!props) return attributes;

    for (const prop of props) {
      let name = '';
      let value = '';

      switch (prop.type) {
        case 6: // ATTRIBUTE
          name = prop.name;
          value = prop.value?.content || '';
          break;
        case 7: // DIRECTIVE
          name = this.getVueDirectiveName(prop);
          value = this.getVueDirectiveValue(prop);
          break;
        default:
          continue;
      }

      attributes[name] = value;
    }

    return attributes;
  }

  private getVueDirectiveName(directive: any): string {
    let name = directive.name;
    
    if (directive.arg) {
      if (typeof directive.arg === 'string') {
        name += `:${directive.arg}`;
      } else if (directive.arg.content) {
        name += `:${directive.arg.content}`;
      }
    }

    if (directive.modifiers && directive.modifiers.length > 0) {
      name += '.' + directive.modifiers.join('.');
    }

    return `v-${name}`;
  }

  private getVueDirectiveValue(directive: any): string {
    if (!directive.exp) return '';
    
    if (typeof directive.exp === 'string') {
      return directive.exp;
    }
    
    if (directive.exp.content) {
      return directive.exp.content;
    }

    return this.stringifyVueExpression(directive.exp);
  }

  private stringifyVueExpression(expression: any): string {
    if (!expression) return '';
    
    switch (expression.type) {
      case 4: // SIMPLE_EXPRESSION
        return expression.content;
      case 5: // INTERPOLATION
        return `{{${this.stringifyVueExpression(expression.content)}}}`;
      case 8: // COMPOUND_EXPRESSION
        return expression.children.map((child: any) => 
          typeof child === 'string' ? child : this.stringifyVueExpression(child)
        ).join('');
      default:
        return expression.toString();
    }
  }

  private extractVueTextContent(node: any): string {
    if (!node.children) return '';
    
    return node.children
      .filter((child: any) => child.type === 2) // TEXT type
      .map((child: any) => child.content)
      .join('');
  }

  private getVuePosition(loc: any, offset: any): vscode.Position {
    if (!loc) {
      return new vscode.Position(0, 0);
    }

    const line = (loc.line - 1) + (offset.line - 1);
    const character = loc.column + (loc.line === 1 ? offset.column : 0);
    
    return new vscode.Position(line, character);
  }

  private parseVueTemplateContent(
    templateContent: string, 
    document: vscode.TextDocument, 
    offset: any
  ): ParsedElement[] {
    const elements: ParsedElement[] = [];
    const lines = templateContent.split('\n');

    // Basic regex patterns for Vue template syntax
    const vueElementRegex = /<([a-zA-Z][a-zA-Z0-9-]*)\s*([^>]*?)(\s*\/?>)/g;
    const offsetLine = offset.line - 1;

    for (let lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      const line = lines[lineIndex];
      let match;

      while ((match = vueElementRegex.exec(line)) !== null) {
        const tagName = match[1];
        const attributesStr = match[2];
        const startPos = match.index;
        const endPos = match.index + match[0].length;

        elements.push({
          type: 'VueElement',
          tagName,
          attributes: this.parseVueAttributesFromString(attributesStr),
          textContent: '',
          children: [],
          range: this.createRange(
            lineIndex + offsetLine, 
            startPos, 
            lineIndex + offsetLine, 
            endPos
          ),
          framework: 'vue'
        });
      }
    }

    return elements;
  }

  private parseVueAttributesFromString(attributesStr: string): Record<string, string | undefined> {
    const attributes: Record<string, string | undefined> = {};
    
    // Vue-specific attribute patterns
    const patterns = [
      // Directives: v-if="condition", v-for="item in items"
      /v-([a-zA-Z][a-zA-Z0-9-]*(?::[a-zA-Z][a-zA-Z0-9-]*)?(?:\.[a-zA-Z]+)*)="([^"]*?)"/g,
      // Event listeners: @click="handler"
      /@([a-zA-Z][a-zA-Z0-9-]*(?:\.[a-zA-Z]+)*)="([^"]*?)"/g,
      // Property binding: :href="url"
      /:([a-zA-Z][a-zA-Z0-9-]*)="([^"]*?)"/g,
      // Regular attributes: id="value"
      /([a-zA-Z][a-zA-Z0-9-]*?)="([^"]*?)"/g,
      // Boolean attributes: disabled, selected
      /\b([a-zA-Z][a-zA-Z0-9-]*?)\b(?!=)/g
    ];

    for (const pattern of patterns) {
      let match;
      while ((match = pattern.exec(attributesStr)) !== null) {
        const name = match[1];
        const value = match[2] || '';
        
        // Format the attribute name based on the pattern
        if (pattern.source.startsWith('v-')) {
          attributes[`v-${name}`] = value;
        } else if (pattern.source.startsWith('@')) {
          attributes[`@${name}`] = value;
        } else if (pattern.source.startsWith(':')) {
          attributes[`:${name}`] = value;
        } else {
          attributes[name] = value;
        }
      }
    }

    return attributes;
  }

  private fallbackParse(document: vscode.TextDocument): ParsedElement[] {
    const elements: ParsedElement[] = [];
    const text = document.getText();
    
    // Find template section
    const templateMatch = text.match(/<template[^>]*>([\s\S]*?)<\/template>/);
    if (!templateMatch) return elements;
    
    const templateContent = templateMatch[1];
    const templateStart = text.indexOf(templateMatch[0]) + templateMatch[0].indexOf('>') + 1;
    
    // Calculate line offset for template content
    const beforeTemplate = text.substring(0, templateStart);
    const offsetLine = beforeTemplate.split('\n').length - 1;
    
    return this.parseVueTemplateContent(templateContent, document, { line: offsetLine + 1, column: 0 });
  }
}