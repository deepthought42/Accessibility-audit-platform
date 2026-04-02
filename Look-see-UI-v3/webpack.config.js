const webpack = require('webpack');

module.exports = (config, options) => {
  // Ensure plugins array exists
  if (!config.plugins) {
    config.plugins = [];
  }
  
  // Determine mode - handle both build and test contexts
  const mode = options?.mode || (process.env.NODE_ENV === 'production' ? 'production' : 'development');
  
  // Add DefinePlugin to replace process.env references with actual values from system environment
  // For tests, use empty/default values if env vars are not set
  const envVars = {
    'NODE_ENV': process.env.NODE_ENV || mode,
    'AUTH0_APP_URI': process.env.AUTH0_APP_URI || '',
    'AUTH0_API_URI': process.env.AUTH0_API_URI || '',
    'AUTH0_DOMAIN': process.env.AUTH0_DOMAIN || '',
    'AUTH0_CLIENT_ID': process.env.AUTH0_CLIENT_ID || '',
    'AUTH0_AUDIENCE': process.env.AUTH0_AUDIENCE || '',
    'AUTH0_REDIRECT_URI': process.env.AUTH0_REDIRECT_URI || '',
    'AUTH0_ERROR_PATH': process.env.AUTH0_ERROR_PATH || '/error',
    'PUSHER_APP_ID': process.env.PUSHER_APP_ID || '',
    'PUSHER_KEY': process.env.PUSHER_KEY || '',
    'PUSHER_SECRET': process.env.PUSHER_SECRET || '',
    'PUSHER_CLUSTER': process.env.PUSHER_CLUSTER || '',
    'SEGMENT_KEY': process.env.SEGMENT_KEY || '',
  };
  
  // Find existing DefinePlugin and replace it, or add new one
  const existingDefinePluginIndex = config.plugins.findIndex(
    plugin => plugin && plugin.constructor && plugin.constructor.name === 'DefinePlugin'
  );
  
  const definePlugin = new webpack.DefinePlugin({
    'process.env': JSON.stringify(envVars)
  });
  
  if (existingDefinePluginIndex >= 0) {
    config.plugins[existingDefinePluginIndex] = definePlugin;
  } else {
    config.plugins.push(definePlugin);
  }
  
  return config;
};
