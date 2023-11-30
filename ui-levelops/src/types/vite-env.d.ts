/// <reference types="vite/client" />

interface ImportMetaEnv extends APP_ENV.ENV {}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
