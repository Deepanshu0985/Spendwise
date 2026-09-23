import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Local dev only: forwards /api to the backend so the browser sees one origin
    // and no CORS configuration is needed on the backend for this.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
