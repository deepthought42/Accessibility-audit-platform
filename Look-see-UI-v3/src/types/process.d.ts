// Type declarations for process.env
// Used during build time - values are replaced by webpack/Angular build system
declare namespace NodeJS {
  interface ProcessEnv {
    [key: string]: string | undefined;
    NODE_ENV?: string;
    AUTH0_DOMAIN?: string;
    AUTH0_CLIENT_ID?: string;
    AUTH0_AUDIENCE?: string;
    AUTH0_API_URI?: string;
    AUTH0_APP_URI?: string;
    AUTH0_REDIRECT_URI?: string;
    AUTH0_ERROR_PATH?: string;
    PUSHER_APP_ID?: string;
    PUSHER_KEY?: string;
    PUSHER_SECRET?: string;
    PUSHER_CLUSTER?: string;
    SEGMENT_KEY?: string;
  }
}

declare let process: {
  env: NodeJS.ProcessEnv;
};
