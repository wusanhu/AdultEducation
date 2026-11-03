/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_SHORT_NAME: string
  readonly VITE_APP_ENV: string
  readonly VITE_BASE_PATH: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_UPLOAD_BASE_URL: string
  readonly VITE_PROXY_TARGET?: string
  readonly VITE_DEV_PORT?: string
  readonly VITE_ADMIN_SKIN?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

interface Window {
  /** 运行时外部注入的皮肤 key，需在应用脚本加载前赋值 */
  __ADMIN_SKIN__?: string
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}
