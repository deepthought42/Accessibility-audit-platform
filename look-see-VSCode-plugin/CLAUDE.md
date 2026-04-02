# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a VSCode extension project for accessibility analysis called "Look-see". The project is in the specification/planning phase - the main implementation has not been started yet.

**Current State**: The repository contains only documentation and specifications:
- `README.md` - Basic project information (minimal content)
- `vscode_accessibility_extension_spec.md` - Comprehensive technical specification (159KB)
- `LICENSE` - Apache License 2.0

**No source code, configuration files, or build setup exists yet.**

## Architecture (From Specification)

The extension will be structured as:

### Core Components
- **AccessibilityAnalyzer**: Main coordinator for real-time WCAG 2.2 analysis
- **Framework Parsers**: Specialized parsers for React/JSX, Angular templates, and Vue SFCs
- **WCAG Rule Engine**: Extensible rule system implementing accessibility checks
- **Quick Fix Provider**: Intelligent code actions with context-aware suggestions

### Supported Technologies
- React/JSX (.jsx, .tsx files)
- Angular templates 
- Vue.js Single File Components (.vue)
- TypeScript and JavaScript
- HTML files

### Key Features (Planned)
- Real-time accessibility violation detection
- Framework-specific syntax understanding
- Intelligent quick fixes with multiple options
- Educational tooltips with WCAG guidance
- Project-wide analysis and reporting
- Integration with ESLint/Prettier workflows

## Development Setup (Not Yet Implemented)

Based on the specification, the project will likely use:
- TypeScript for main implementation
- VSCode Extension API
- Parsers for framework-specific syntax (likely @typescript-eslint/parser, @angular/compiler, @vue/compiler-sfc)
- Testing framework (Jest mentioned in spec)
- Standard VSCode extension build pipeline

## Implementation Status

**Current Phase**: Specification and planning
**Next Steps**: 
1. Set up basic VSCode extension structure with package.json
2. Implement core AccessibilityAnalyzer component
3. Add framework parsers starting with React/JSX
4. Build WCAG rule engine with basic rules
5. Add quick fix providers

## Specification Reference

The complete technical specification is in `vscode_accessibility_extension_spec.md` and includes:
- Detailed user stories and acceptance criteria
- Component architecture with TypeScript interfaces
- WCAG 2.2 rule implementations
- Performance requirements
- Testing strategy
- API reference documentation

Refer to this specification when implementing any component of the extension.