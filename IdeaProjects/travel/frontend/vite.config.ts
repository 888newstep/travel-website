import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';

const proxyTarget = process.env.VITE_PROXY_TARGET || 'http://localhost:8090';
const devPort = Number(process.env.VITE_APP_PORT) || 3000;

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: devPort,
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
        headers: {
          'Content-Type': 'application/json; charset=utf-8'
        }
      },
    },
  }});