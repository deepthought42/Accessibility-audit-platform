// Non-secret defaults committed to source control.
// Values are overridden at runtime by loading /config.json.

export const environment = {
  production: false,
  base_url: '',
  api_url: '',
  auth: {
    domain: '',
    clientId: '',
    authorizationParams: {
      audience: undefined,
      redirect_uri: '',
    },
    errorPath: '/error',
  },
  httpInterceptor: {
    allowedList: [],
  },
  pusher: {
    app_id: '',
    key: '',
    secret: '',
    cluster: ''
  },
  segment_key: ''
};
