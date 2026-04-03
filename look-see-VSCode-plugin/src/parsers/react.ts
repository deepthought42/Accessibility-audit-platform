import * as vscode from 'vscode';
const { parse } = require('@typescript-eslint/parser');
import { BaseParser } from './base';
import { DocumentContext, ParsedElement, ImportInfo } from '../types';

export class ReactParser extends BaseParser {
  readonly framework = 'react' as const;
  readonly supportedExtensions = ['.jsx', '.tsx', '.js', '.ts'];

  canParse(document: vscode.TextDocument): boolean {
    if (!super.canParse(document)) {
      return false;
    }
    
    // Check if it's likely a React file by looking for JSX or React imports
    const text = document.getText();
    return this.isReactFile(text);
  }

  async parse(document: vscode.TextDocument): Promise<DocumentContext> {
    const context = this.createBaseContext(document);
    
    try {
      const ast = parse(document.getText(), {
        ecmaVersion: 2020,
        sourceType: 'module',
        ecmaFeatures: {
          jsx: true,
        },
        filePath: document.uri.fsPath
      });

      context.ast = ast;
      context.imports = this.extractImports(ast);
      context.elements = this.extractJSXElements(ast, document);
      
    } catch (error) {
      console.error('Failed to parse React document:', error);
      // Fallback to basic text parsing
      context.elements = this.fallbackParse(document);
    }

    return context;
  }

  private isReactFile(text: string): boolean {
    // Look for React imports or JSX syntax
    const reactPatterns = [
      /import.*React/,
      /from\s+['"]react['"]/,
      /<[A-Z][a-zA-Z0-9]*[\s>]/,  // JSX component tags
      /<[a-z]+[\s\/>]/,           // HTML-like tags in JS context
      /jsx/i,
      /\.tsx?$/
    ];

    return reactPatterns.some(pattern => pattern.test(text));
  }

  private extractImports(ast: any): ImportInfo[] {
    const imports: ImportInfo[] = [];
    
    if (!ast.body) return imports;

    for (const node of ast.body) {
      if (node.type === 'ImportDeclaration') {
        const source = node.source.value;
        
        if (node.specifiers) {
          for (const spec of node.specifiers) {
            imports.push({
              name: spec.local.name,
              path: source,
              isDefault: spec.type === 'ImportDefaultSpecifier',
              range: this.createRange(
                node.loc.start.line - 1,
                node.loc.start.column,
                node.loc.end.line - 1,
                node.loc.end.column
              )
            });
          }
        }
      }
    }

    return imports;
  }

  private extractJSXElements(ast: any, document: vscode.TextDocument): ParsedElement[] {
    const elements: ParsedElement[] = [];
    
    const walkNode = (node: any) => {
      if (!node) return;

      if (node.type === 'JSXElement' || node.type === 'JSXSelfClosingElement') {
        const element = this.parseJSXElement(node, document);
        if (element) {
          elements.push(element);
        }
      }

      // Recursively walk all properties
      for (const key in node) {
        if (node.hasOwnProperty(key) && typeof node[key] === 'object') {
          if (Array.isArray(node[key])) {
            node[key].forEach(walkNode);
          } else {
            walkNode(node[key]);
          }
        }
      }
    };

    walkNode(ast);
    return elements;
  }

  private parseJSXElement(node: any, document: vscode.TextDocument): ParsedElement | null {
    try {
      const tagName = this.getTagName(node);
      const range = this.createRange(
        node.loc.start.line - 1,
        node.loc.start.column,
        node.loc.end.line - 1,
        node.loc.end.column
      );

      const element: ParsedElement = {
        type: node.type,
        tagName,
        attributes: this.parseJSXAttributes(node),
        textContent: this.extractJSXTextContent(node),
        children: [],
        range,
        framework: 'react',
        isComponent: this.isReactComponent(tagName),
        props: this.parseJSXProps(node)
      };

      // Parse children
      if (node.children) {
        element.children = node.children
          .map((child: any) => this.parseJSXElement(child, document))
          .filter((child: ParsedElement | null) => child !== null);
      }

      return element;
    } catch (error) {
      console.error('Error parsing JSX element:', error);
      return null;
    }
  }

  private getTagName(node: any): string {
    if (node.type === 'JSXSelfClosingElement') {
      return this.getJSXElementName(node.name);
    } else if (node.type === 'JSXElement') {
      return this.getJSXElementName(node.openingElement.name);
    }
    return '';
  }

  private getJSXElementName(nameNode: any): string {
    if (nameNode.type === 'JSXIdentifier') {
      return nameNode.name;
    } else if (nameNode.type === 'JSXMemberExpression') {
      return `${this.getJSXElementName(nameNode.object)}.${nameNode.property.name}`;
    } else if (nameNode.type === 'JSXNamespacedName') {
      return `${nameNode.namespace.name}:${nameNode.name.name}`;
    }
    return '';
  }

  private parseJSXAttributes(node: any): Record<string, string | undefined> {
    const attributes: Record<string, string | undefined> = {};
    
    const attributeNodes = node.type === 'JSXSelfClosingElement' 
      ? node.attributes 
      : node.openingElement?.attributes;

    if (!attributeNodes) return attributes;

    for (const attr of attributeNodes) {
      if (attr.type === 'JSXAttribute') {
        const name = attr.name.name;
        let value: string | undefined;

        if (attr.value) {
          if (attr.value.type === 'Literal') {
            value = attr.value.value;
          } else if (attr.value.type === 'JSXExpressionContainer') {
            value = this.evaluateJSXExpression(attr.value.expression);
          }
        } else {
          // Boolean attribute (e.g., <input disabled />)
          value = '';
        }

        attributes[name] = value;
      }
    }

    return attributes;
  }

  private parseJSXProps(node: any): Record<string, any> {
    const props: Record<string, any> = {};
    
    const attributeNodes = node.type === 'JSXSelfClosingElement' 
      ? node.attributes 
      : node.openingElement?.attributes;

    if (!attributeNodes) return props;

    for (const attr of attributeNodes) {
      if (attr.type === 'JSXAttribute') {
        const name = attr.name.name;
        
        if (attr.value) {
          if (attr.value.type === 'JSXExpressionContainer') {
            props[name] = {
              type: 'expression',
              value: attr.value.expression,
              static: this.isStaticExpression(attr.value.expression)
            };
          } else if (attr.value.type === 'Literal') {
            props[name] = {
              type: 'literal',
              value: attr.value.value,
              static: true
            };
          }
        } else {
          props[name] = {
            type: 'boolean',
            value: true,
            static: true
          };
        }
      }
    }

    return props;
  }

  private evaluateJSXExpression(expression: any): string | undefined {
    if (!expression) return undefined;

    switch (expression.type) {
      case 'Literal':
        return String(expression.value);
      case 'Identifier':
        return `{${expression.name}}`;
      case 'MemberExpression':
        return `{${this.getMemberExpressionName(expression)}}`;
      case 'TemplateLiteral':
        return this.evaluateTemplateLiteral(expression);
      case 'BinaryExpression':
        return `{${this.getBinaryExpressionName(expression)}}`;
      default:
        return `{${expression.type}}`;
    }
  }

  private getMemberExpressionName(expression: any): string {
    let result = '';
    if (expression.object) {
      if (expression.object.type === 'Identifier') {
        result += expression.object.name;
      } else if (expression.object.type === 'MemberExpression') {
        result += this.getMemberExpressionName(expression.object);
      }
    }
    
    if (expression.property) {
      result += `.${expression.property.name}`;
    }
    
    return result;
  }

  private getBinaryExpressionName(expression: any): string {
    const left = expression.left?.type === 'Literal' ? expression.left.value : 'expr';
    const right = expression.right?.type === 'Literal' ? expression.right.value : 'expr';
    return `${left} ${expression.operator} ${right}`;
  }

  private evaluateTemplateLiteral(expression: any): string {
    let result = '';
    
    if (expression.quasis && expression.expressions) {
      for (let i = 0; i < expression.quasis.length; i++) {
        result += expression.quasis[i].value.raw;
        if (i < expression.expressions.length) {
          result += '${...}';
        }
      }
    }
    
    return result;
  }

  private isStaticExpression(expression: any): boolean {
    return expression && (expression.type === 'Literal' || 
           (expression.type === 'TemplateLiteral' && 
            (!expression.expressions || expression.expressions.length === 0)));
  }

  private extractJSXTextContent(node: any): string {
    if (node.type === 'JSXText') {
      return node.value.trim();
    }

    if (node.children) {
      return node.children
        .map((child: any) => this.extractJSXTextContent(child))
        .join('')
        .trim();
    }

    return '';
  }

  private isReactComponent(tagName: string): boolean {
    // React components start with uppercase letter
    return /^[A-Z]/.test(tagName);
  }

  private fallbackParse(document: vscode.TextDocument): ParsedElement[] {
    // Basic regex-based parsing as fallback
    const elements: ParsedElement[] = [];
    const text = document.getText();
    const lines = text.split('\n');

    // Simple JSX element regex
    const jsxRegex = /<([a-zA-Z][a-zA-Z0-9]*)\s*([^>]*?)(\s*\/?>)/g;
    
    for (let lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      const line = lines[lineIndex];
      let match;

      while ((match = jsxRegex.exec(line)) !== null) {
        const tagName = match[1];
        const attributesStr = match[2];
        const startPos = match.index;
        const endPos = match.index + match[0].length;

        elements.push({
          type: 'JSXElement',
          tagName,
          attributes: this.parseAttributesFromString(attributesStr),
          textContent: '',
          children: [],
          range: this.createRange(lineIndex, startPos, lineIndex, endPos),
          framework: 'react',
          isComponent: this.isReactComponent(tagName)
        });
      }
    }

    return elements;
  }

  private parseAttributesFromString(attributesStr: string): Record<string, string | undefined> {
    const attributes: Record<string, string | undefined> = {};
    
    // Simple attribute parsing regex
    const attrRegex = /(\w+)(?:=(?:{([^}]+)}|"([^"]+)"|'([^']+)'))?/g;
    let match;

    while ((match = attrRegex.exec(attributesStr)) !== null) {
      const name = match[1];
      const value = match[2] || match[3] || match[4] || '';
      attributes[name] = value;
    }

    return attributes;
  }
}