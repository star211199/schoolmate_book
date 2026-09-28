import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  optimizeDeps: {
    // 显式声明预构建依赖，避免运行时反复发现新依赖触发重新预构建
    include: [
      'vue',
      'vue-router',
      'pinia',
      'axios',
      'element-plus',
      '@element-plus/icons-vue'
    ]
  },
  server: {
    port: 5173,
    host: '127.0.0.1',
    // 端口被占用时直接报错，避免静默漂移到 5174 等端口造成困惑
    strictPort: true,
    open: false,
    proxy: {
      // 开发期通过代理访问后端，规避跨域
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
