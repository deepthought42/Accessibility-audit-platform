#!/usr/bin/env node

const { spawn } = require('child_process');
const path = require('path');

// Load environment variables and generate environment.ts
require('./load-env.js');

// Get the Angular CLI path
const ngPath = path.join(__dirname, '..', 'node_modules', '@angular', 'cli', 'bin', 'ng.js');

// Start Angular dev server with environment variables
console.log('🚀 Starting Angular development server with environment variables...\n');

const ngServe = spawn('node', [ngPath, 'serve'], {
  stdio: 'inherit',
  shell: false,
  env: { ...process.env }
});

ngServe.on('error', (error) => {
  console.error('❌ Error starting Angular CLI:', error);
  process.exit(1);
});

ngServe.on('close', (code) => {
  if (code !== 0) {
    console.error(`\n❌ Angular CLI exited with code ${code}`);
  }
  process.exit(code);
});

// Handle process termination
process.on('SIGINT', () => {
  console.log('\n🛑 Stopping Angular development server...');
  ngServe.kill('SIGINT');
});

process.on('SIGTERM', () => {
  ngServe.kill('SIGTERM');
});

