import * as vscode from 'vscode';
import { FrameworkParser, DocumentContext, FrameworkType, ParsedElement } from '../types';

export abstract class BaseParser implements FrameworkParser {
  abstract readonly framework: FrameworkType;
  abstract readonly supportedExtensions: string[];

  canParse(document: vscode.TextDocument): boolean {
    const extension = this.getFileExtension(document.uri.fsPath);
    return this.supportedExtensions.includes(extension);
  }

  abstract parse(document: vscode.TextDocument): Promise<DocumentContext>;

  protected getFileExtension(filePath: string): string {
    const parts = filePath.split('.');
    return parts.length > 1 ? `.${parts[parts.length - 1]}` : '';
  }

  protected createBaseContext(document: vscode.TextDocument): DocumentContext {
    return {
      uri: document.uri,
      languageId: document.languageId,
      framework: this.framework,
      text: document.getText(),
      elements: [],
      imports: []
    };
  }

  protected createRange(startLine: number, startChar: number, endLine: number, endChar: number): vscode.Range {
    return new vscode.Range(
      new vscode.Position(startLine, startChar),
      new vscode.Position(endLine, endChar)
    );
  }

  protected extractTextContent(element: any): string {
    if (typeof element === 'string') {
      return element.trim();
    }
    
    if (element.children && Array.isArray(element.children)) {
      return element.children
        .map((child: any) => this.extractTextContent(child))
        .join('')
        .trim();
    }

    return element.value || element.text || '';
  }

  protected parseAttributes(attrs: any): Record<string, string | undefined> {
    if (!attrs) return {};
    
    const attributes: Record<string, string | undefined> = {};
    
    if (Array.isArray(attrs)) {
      attrs.forEach(attr => {
        if (attr.name) {
          attributes[attr.name] = attr.value || '';
        }
      });
    } else if (typeof attrs === 'object') {
      Object.keys(attrs).forEach(key => {
        attributes[key] = attrs[key];
      });
    }

    return attributes;
  }

  protected isInteractiveElement(tagName: string): boolean {
    const interactiveElements = [
      'a', 'button', 'input', 'select', 'textarea', 
      'audio', 'video', 'iframe', 'object', 'embed',
      'details', 'summary'
    ];
    return interactiveElements.includes(tagName.toLowerCase());
  }

  protected isFormElement(tagName: string): boolean {
    const formElements = ['input', 'select', 'textarea', 'button', 'fieldset', 'legend'];
    return formElements.includes(tagName.toLowerCase());
  }

  protected isMediaElement(tagName: string): boolean {
    const mediaElements = ['img', 'video', 'audio', 'canvas', 'svg', 'object', 'embed', 'iframe'];
    return mediaElements.includes(tagName.toLowerCase());
  }

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
}