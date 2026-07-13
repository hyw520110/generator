import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

function vendorChunk (id) {
  if (!id.includes('node_modules')) return undefined
  if (id.includes('/vue/') || id.includes('/vue-router/') || id.includes('/pinia/') || id.includes('/vue-i18n/')) {
    return 'vue'
  }
  if (id.includes('/@ant-design/icons-vue/')) return 'antd-icons'
  if (id.includes('/ant-design-vue/')) return 'antd'
  return 'vendor'
}

export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    Components({
      globs: ['src/components/**/*.vue', '!src/components/Charts/Trend.vue'],
      resolvers: [
        AntDesignVueResolver({
          importStyle: false, // Ant Design Vue 4.x 默认使用 CSS-in-JS
        }),
      ],
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    },
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
  },
  esbuild: {
    jsx: 'automatic',
    loader: 'jsx',
  },
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'pinia',
      'ant-design-vue',
      '@ant-design/icons-vue',
      'axios',
      'lodash-es',
      'dayjs',
      'dayjs/plugin/advancedFormat > dayjs',
      'dayjs/plugin/customParseFormat > dayjs',
      'dayjs/plugin/localeData > dayjs',
      'dayjs/plugin/quarterOfYear > dayjs',
      'dayjs/plugin/weekOfYear > dayjs',
      'dayjs/plugin/weekYear > dayjs',
      'dayjs/plugin/weekday > dayjs',
      'moment',
      'nprogress',
      'vue-i18n',
      'md5',
      'mockjs2',
    ],
    exclude: ['webpack-theme-color-replacer'],
  },
  build: {
    chunkSizeWarningLimit: 2500,
    rollupOptions: {
      output: {
        manualChunks: vendorChunk
      }
    }
  },
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true
      }
    }
  },
  server: {
    port: 9000,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/v1': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
