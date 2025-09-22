import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const enableDevtools = mode === 'development' && process.env.VITE_VUE_DEVTOOLS === 'true'
  return {
    plugins: [
      vue(),
      ...(enableDevtools ? [vueDevTools()] as const : []),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
  }
})
