module.exports = {
  root: true,
  env: {
    browser: true,
    es2020: true,
    node: true
  },
  extends: [
    'eslint:recommended',
    '@angular-eslint/recommended',
    '@angular-eslint/template/process-inline-templates'
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    ecmaVersion: 2020,
    sourceType: 'module',
    project: './tsconfig.json'
  },
  plugins: [
    '@typescript-eslint',
    '@angular-eslint'
  ],
  rules: {
    // Temporarily disable problematic rules
    'no-console': 'off',
    '@typescript-eslint/no-explicit-any': 'off',
    '@typescript-eslint/no-unused-vars': 'warn',
    '@angular-eslint/directive-selector': [
      'error',
      {
        type: 'attribute',
        prefix: 'app',
        style: 'camelCase'
      }
    ],
    '@angular-eslint/component-selector': [
      'error',
      {
        type: 'element',
        prefix: 'app',
        style: 'kebab-case'
      }
    ]
  },
  overrides: [
    {
      files: ['*.html'],
      extends: [
        '@angular-eslint/template/recommended',
        '@angular-eslint/template/accessibility'
      ],
      rules: {}
    }
  ]
}; 