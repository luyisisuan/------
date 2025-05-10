// vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173, // 前端开发服务器将运行在此端口，你可以根据需要修改
    proxy: {
      // 当你的前端代码请求 '/api/...' 时，Vite 开发服务器会将其转发到 target 指定的地址
      '/api': {
        target: 'http://localhost:8080', // 你的后端 Spring Boot 服务地址和端口
        changeOrigin: true, // 必须设置为 true，否则代理可能无法正常工作
        // rewrite: (path) => path.replace(/^\/api/, ''), 
        // 上面这行 rewrite 通常不需要，因为你的后端API路径本身就包含 /api
        // 例如前端请求 /api/auth/login，代理后会请求后端的 http://localhost:8080/api/auth/login
      }
    }
  }
})