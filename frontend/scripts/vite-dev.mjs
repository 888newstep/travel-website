import crypto from 'node:crypto'

if (typeof crypto.getRandomValues !== 'function' && typeof crypto.webcrypto?.getRandomValues === 'function') {
  crypto.getRandomValues = crypto.webcrypto.getRandomValues.bind(crypto.webcrypto)
}

import { loadEnv } from 'vite'

// 统一从 .env 读取环境变量（优先级：进程环境变量 > .env > 默认值）
const envVars = loadEnv('development', process.cwd(), 'VITE_')
const host = process.env.VITE_APP_HOST || envVars.VITE_APP_HOST || '0.0.0.0'
const port = Number(process.env.VITE_APP_PORT || envVars.VITE_APP_PORT) || 3000

const { createServer } = await import('vite')

// 加载 vite.config.ts（含 /api 代理到后端网关），并显式传入 host/port
const server = await createServer({
  configFile: 'vite.config.ts',
  server: {
    host,
    port,
  },
})

await server.listen()
server.printUrls()
