export { BaseParser } from './base';
export { ReactParser } from './react';
export { AngularParser } from './angular';
export { VueParser } from './vue';

import { FrameworkParser, FrameworkType } from '../types';
import { ReactParser } from './react';
import { AngularParser } from './angular';
import { VueParser } from './vue';

export class ParserRegistry {
  private parsers: Map<FrameworkType, FrameworkParser> = new Map();

  constructor() {
    this.registerParser(new ReactParser());
    this.registerParser(new AngularParser());
    this.registerParser(new VueParser());
  }

  registerParser(parser: FrameworkParser): void {
    this.parsers.set(parser.framework, parser);
  }

  getParser(framework: FrameworkType): FrameworkParser | undefined {
    return this.parsers.get(framework);
  }

  getParserForDocument(document: import('vscode').TextDocument): FrameworkParser | undefined {
    for (const parser of this.parsers.values()) {
      if (parser.canParse(document)) {
        return parser;
      }
    }
    return undefined;
  }

  getAllParsers(): FrameworkParser[] {
    return Array.from(this.parsers.values());
  }

  getSupportedFrameworks(): FrameworkType[] {
    return Array.from(this.parsers.keys());
  }
}