import { fileURLToPath } from 'node:url'
import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import type { UserConfig } from 'vite'
import viteConfig from './vite.config'

export default mergeConfig(
  (typeof viteConfig === 'function' ? (viteConfig as (env: { mode: string }) => UserConfig)({ mode: process.env.NODE_ENV || 'test' }) : (viteConfig as UserConfig)),
  defineConfig({
    test: {
      environment: 'jsdom',
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url)),
    },
  }),
)
