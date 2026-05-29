import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // dev 时把 /api 和 /uploads 都转给本地后端：
      //   /api      — 业务接口
      //   /uploads  — 后端 ImageStorage 落盘后通过 Spring static handler 暴露的图床路径
      //              （前端 <img src="/uploads/xx.jpg"> 才能被代理到 8080 拉文件）
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
