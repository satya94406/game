import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server proxies /api and /ws to the Spring Boot backend on :8080, so the
// browser talks to a single origin (no CORS) and SockJS upgrades cleanly.
export default defineConfig({
  plugins: [react()],
  define: {
    // sockjs-client references the Node-style `global`; map it to the browser global.
    global: 'globalThis',
  },
  server: {
    host: true, // bind 0.0.0.0 so other devices on the LAN can reach the dev server
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws': { target: 'http://localhost:8080', changeOrigin: true, ws: true },
    },
  },
})
