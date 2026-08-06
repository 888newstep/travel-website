import crypto from 'node:crypto'

if (typeof crypto.getRandomValues !== 'function' && typeof crypto.webcrypto?.getRandomValues === 'function') {
  crypto.getRandomValues = crypto.webcrypto.getRandomValues.bind(crypto.webcrypto)
}

const host = process.env.VITE_APP_HOST || '0.0.0.0'
const port = Number(process.env.VITE_APP_PORT) || 3000

const { createServer } = await import('vite')

const server = await createServer({
  server: {
    host,
    port,
  },
})

await server.listen()
server.printUrls()
