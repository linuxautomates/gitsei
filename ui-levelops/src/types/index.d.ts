export {};

declare global {
  interface Window {
    _env_: APP_ENV.ENV;
  }
}
