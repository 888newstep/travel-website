/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_API_BASE_URL: string
    readonly VITE_API_TIMEOUT: string
    readonly VITE_PROXY_TARGET: string
    readonly VITE_APP_PORT: string
    readonly VITE_APP_TITLE: string
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}
