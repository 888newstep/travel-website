import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

const proxyTarget = process.env.VITE_PROXY_TARGET || 'http://localhost:8090'
const devPort = Number(process.env.VITE_APP_PORT) || 3000
const devHost = process.env.VITE_APP_HOST || '0.0.0.0'

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
