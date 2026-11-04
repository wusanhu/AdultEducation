import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://127.0.0.1:8080'
  const devPort = Number(env.VITE_DEV_PORT) || 5280

  return {
    base: env.VITE_BASE_PATH || '/admin/',
    plugins: [vue()],
    server: {
      host: '0.0.0.0',
      port: devPort,
      proxy: {
        '/api': { target: proxyTarget, changeOrigin: true },
        '/uploads': { target: proxyTarget, changeOrigin: true }
      }
    },
    build: {
      outDir: '../backend/src/main/resources/static/admin',
      emptyOutDir: true
    }
  }
})
