import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * vite preview 的静态服务（sirv）会把所有响应设成 Cache-Control: no-cache，
 * 导致每次刷新都要重新下载全部资源。在 1Mbps 的穿透隧道上这是致命的
 * （首屏要等近 50 秒）。这里在响应写出前按路径改写缓存策略：
 *   /assets/**  —— Vite 产物文件名自带内容 hash，可以长期强缓存
 *   /images/**  —— 文件名不含 hash，给 7 天；换图需要换文件名
 *   index.html  —— 保持 no-cache，保证发版后能立刻拿到新的资源引用
 */
function previewCacheHeaders() {
  return {
    name: 'preview-cache-headers',
    configurePreviewServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = (req.url || '').split('?')[0]
        let value = ''
        if (url.startsWith('/assets/')) {
          value = 'public, max-age=31536000, immutable'
        } else if (url.startsWith('/images/')) {
          value = 'public, max-age=604800'
        }
        if (!value) return next()

        // sirv 会在内部通过 res.setHeader 或 res.writeHead(status, headers) 塞入
        // Cache-Control: no-cache。后者传入的 headers 优先级高于 setHeader，
        // 所以两条路径都要兜住，在真正写出响应头的那一刻改写。
        const originalWriteHead = res.writeHead
        res.writeHead = function (statusCode, statusMessage, headers) {
          // 兼容 writeHead(status, headers) 的两参重载
          if (statusMessage && typeof statusMessage === 'object') {
            headers = statusMessage
            statusMessage = undefined
          }
          let cleaned = headers
          if (cleaned && typeof cleaned === 'object') {
            cleaned = { ...cleaned }
            for (const k of Object.keys(cleaned)) {
              if (k.toLowerCase() === 'cache-control') delete cleaned[k]
            }
          }
          res.setHeader('Cache-Control', value)
          return statusMessage === undefined
            ? originalWriteHead.call(this, statusCode, cleaned)
            : originalWriteHead.call(this, statusCode, statusMessage, cleaned)
        }
        next()
      })
    }
  }
}

export default defineConfig({
  plugins: [vue(), previewCacheHeaders()],
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
    // Vite 5.4.12+ 默认只放行 localhost / IP，内网穿透域名会被拦（Blocked request）
    // 这里放行 natapp 免费隧道的域名后缀；换其他穿透工具时把后缀一起加上即可
    allowedHosts: ['.natappfree.cc'],
    proxy: {
      // 开发期通过代理访问后端，规避跨域
      // ws: true 让 WebSocket 握手（/api/ws/chat）也走这条代理
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  },
  // 生产/演示环境：用 vite preview 托管 dist，并复用上面的 server.proxy
  // （Vite 的 preview.proxy 默认继承 server.proxy，含 ws: true）。
  // 这样前端与 /api（含 WebSocket）共用一个端口，内网穿透只需暴露一个口。
  preview: {
    port: 5173,
    host: '127.0.0.1',
    strictPort: true,
    // 穿透域名无法预先枚举，这里放开 Host 校验。
    // 该校验只用于防 DNS 重绑定，本项目为内网演示部署，风险可接受。
    allowedHosts: true
  }
})
