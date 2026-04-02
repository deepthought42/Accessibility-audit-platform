// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.
import authConfig from '../../credentials/auth_config.json';
import pusherCredentials from '../../credentials/pusher_config.json';
import segmentCredentials from '../../credentials/segment_config.json';

export const environment = {
  production: false,
  base_url: authConfig.appUri,
  api_url: authConfig.apiUri,
  auth: {
    domain: authConfig.domain,
    clientId: authConfig.clientId,
    authorizationParams: {
      audience: (authConfig.authorizationParams.audience && authConfig.authorizationParams.audience !== 'YOUR_API_IDENTIFIER' ? authConfig.authorizationParams.audience : undefined),
      redirect_uri: authConfig.redirect_uri,
    },
    errorPath: authConfig.errorPath,
  },
  httpInterceptor: {
    allowedList: [authConfig.apiUri+"/*"],
    /*
          // Attach access tokens to any calls to '/api' (exact match)
          {
            uri: '"/audits',
            tokenOptions: {
              authorizationParams: {
                audience: authConfig.authorizationParams.audience,
              }
            },
          },
          {
            uri: '/audits/start-individual',
            tokenOptions: {
              authorizationParams: {
                audience: authConfig.authorizationParams.audience,
              }
            },
          },
          {
            uri: '/auditor/start-individual',
            tokenOptions: {
              authorizationParams: {
                audience: authConfig.authorizationParams.audience
              }
            },
          },
          {
            uri: 'https://api-stage.look-see.com"/audits/*',
            tokenOptions: {
              authorizationParams: {
                audience: authConfig.authorizationParams.audience,
              }
            },
          },
        ],
        */
  },

  pusher: {
    app_id: pusherCredentials.app_id,
    key: pusherCredentials.key,
    secret: pusherCredentials.secret,
    cluster: pusherCredentials.cluster
  },
  segment_key : segmentCredentials.key
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
