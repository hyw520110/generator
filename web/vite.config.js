import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    Components({
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
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true
      }
    }
  },
  server: {
    port: 9000,
    proxy: {
      '/v1': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})