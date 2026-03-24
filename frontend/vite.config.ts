import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendTarget = env.BACKEND_URL || 'http://localhost:8081'

  return {
    plugins: [vue()],
    server: {
      host: true,
      port: 5173,
      proxy: {
        '/xx': {
          target: backendTarget,
          changeOrigin: true
        },
        '/ws-chat': {
          target: backendTarget.replace(/^http/, 'ws'),
          ws: true,
          changeOrigin: true
        }
      }
    }
  }
})
