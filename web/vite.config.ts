import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          // Core Vue ecosystem - loaded on every page
          'vue-core': ['vue', 'vue-router', 'pinia'],
          // PrimeVue UI library - large, separate chunk
          'primevue': ['primevue', '@primeuix/themes'],
          // Internationalization
          'i18n': ['vue-i18n'],
          // Form validation (Zod is ~100KB)
          'validation': ['zod', 'vee-validate', '@vee-validate/zod'],
          // HTTP client
          'http': ['axios']
        }
      }
    },
    // Lower threshold to catch future regressions
    chunkSizeWarningLimit: 300
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    },
    headers: {
      'Content-Security-Policy': "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; img-src 'self' data: blob:; font-src 'self' data: https://fonts.gstatic.com; connect-src 'self' ws: wss:"
    }
  }
})
