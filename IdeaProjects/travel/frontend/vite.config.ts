import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

// 统一从 .env 读取代理目标（优先级：进程环境变量 > .env > 默认值）
const proxyTarget = process.env.VITE_PROXY_TARGET || loadEnv('development', process.cwd(), 'VITE_').VITE_PROXY_TARGET || 'http://localhost:8090'
const devPort = Number(process.env.VITE_APP_PORT || loadEnv('development', process.cwd(), 'VITE_').VITE_APP_PORT) || 3000
const devHost = process.env.VITE_APP_HOST || loadEnv('development', process.cwd(), 'VITE_').VITE_APP_HOST || '0.0.0.0'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    host: devHost,
    port: devPort,
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
        headers: {
          'Content-Type': 'application/json; charset=utf-8',
        },
      },
    },
  },
})
