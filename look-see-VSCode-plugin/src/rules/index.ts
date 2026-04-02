export { BaseRule } from './base';
export { AltTextRule } from './alt-text';
export { AriaLabelsRule } from './aria-labels';
export { HeadingStructureRule } from './heading-structure';

import { AccessibilityRule } from '../types';
import { AltTextRule } from './alt-text';
import { AriaLabelsRule } from './aria-labels';
import { HeadingStructureRule } from './heading-structure';

export class RuleRegistry {
  private rules: Map<string, AccessibilityRule> = new Map();

  constructor() {
    this.registerRule(new AltTextRule());
    this.registerRule(new AriaLabelsRule());
    this.registerRule(new HeadingStructureRule());
  }

  registerRule(rule: AccessibilityRule): void {
    this.rules.set(rule.id, rule);
  }

  getRule(ruleId: string): AccessibilityRule | undefined {
    return this.rules.get(ruleId);
  }

  getAllRules(): AccessibilityRule[] {
    return Array.from(this.rules.values());
  }

  getRulesForFramework(framework: string): AccessibilityRule[] {
    return this.getAllRules().filter(rule => 
      rule.frameworks.includes(framework as any)
    );
  }

  getEnabledRules(config: Record<string, { enabled: boolean }>): AccessibilityRule[] {
    return this.getAllRules().filter(rule => {
      const ruleConfig = config[rule.id];
      return !ruleConfig || ruleConfig.enabled !== false;
    });
  }

  getRuleIds(): string[] {
    return Array.from(this.rules.keys());
  }
}