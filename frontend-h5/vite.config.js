import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      // 后端 context-path 自带 /api 前缀，因此这里【不做 rewrite】
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})