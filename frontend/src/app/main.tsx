import React from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { AppProviders } from './providers'
import '../index.css'

const rootElement = document.getElementById('root')

if (!rootElement) {
  throw new Error('Root element #root was not found')
}

createRoot(rootElement).render(
  <React.StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </React.StrictMode>,
)
