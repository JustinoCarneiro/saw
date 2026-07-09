import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      // M07 — login com Google: /oauth2/authorization/google (início do fluxo) e
      // /login/oauth2/code/google (callback) são servidos pelo Spring Security, não pelo
      // apiClient — precisam do mesmo proxy pra front e back continuarem na mesma origem em dev.
      '/oauth2': 'http://localhost:8080',
      '/login/oauth2': 'http://localhost:8080',
    },
  },
})
