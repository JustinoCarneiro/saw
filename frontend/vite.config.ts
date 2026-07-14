import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// BACKEND_PORT: scripts/e2e-up.sh sobe um backend isolado (porta 8090, banco
// sawhub_db_e2e) e este mesmo Vite numa segunda instância (porta 5183) apontando pra ele —
// scripts/dev-up.sh não define a env, então continua caindo no 8080 de sempre.
const backendPort = process.env.BACKEND_PORT ?? '8080'
const backendTarget = `http://localhost:${backendPort}`

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': backendTarget,
      // M07 — login com Google: /oauth2/authorization/google (início do fluxo) e
      // /login/oauth2/code/google (callback) são servidos pelo Spring Security, não pelo
      // apiClient — precisam do mesmo proxy pra front e back continuarem na mesma origem em dev.
      '/oauth2': backendTarget,
      '/login/oauth2': backendTarget,
    },
  },
})
