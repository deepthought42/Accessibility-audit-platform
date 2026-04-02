import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { provideAuth0 } from '@auth0/auth0-angular';

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

// Load config.json from server before bootstrapping Angular
async function loadConfigAndBootstrap() {
  try {
    const response = await fetch('/config.json');
    if (response.ok) {
      const config = await response.json();
      // Merge config into environment
      Object.assign(environment, {
        ...environment,
        ...config,
        auth: {
          ...environment.auth,
          ...config.auth,
          authorizationParams: {
            ...environment.auth.authorizationParams,
            ...config.auth?.authorizationParams,
          }
        },
        httpInterceptor: {
          ...environment.httpInterceptor,
          ...config.httpInterceptor,
        },
        pusher: {
          ...environment.pusher,
          ...config.pusher,
        }
      });
      console.log('✅ Loaded configuration from config.json');
    } else {
      console.warn('⚠️  config.json not found, using environment defaults');
    }
  } catch (error) {
    console.warn('⚠️  Failed to load config.json, using environment defaults:', error);
  }

  if (environment.production) {
    enableProdMode();
  }

  platformBrowserDynamic().bootstrapModule(AppModule, {
    providers: [
      provideAuth0({
        domain: environment.auth.domain,
        clientId: environment.auth.clientId,
        authorizationParams: {
          redirect_uri: window.location.origin,
          audience: environment.auth.authorizationParams.audience,
        }
      }),
    ]
  })
    .catch(err => console.error(err));
}

loadConfigAndBootstrap();
