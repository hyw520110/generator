import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const apiHost = env.VITE_API_HOST || 'localhost'
  const apiPort = env.VITE_API_PORT || '8082'
  const apiBaseUrl = `http://${'$'}{apiHost}:${'$'}{apiPort}`

  return {
    root: path.resolve(__dirname),
    publicDir: 'public',
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
      include: /\.(jsx?|tsx?|vue)$/,
    },
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'pinia',
        'ant-design-vue',
        '@ant-design/icons-vue',
        'axios',
        'lodash',
        'lodash.get',
        'dayjs',
        'dayjs/plugin/advancedFormat',
        'dayjs/plugin/customParseFormat',
        'dayjs/plugin/dayOfYear',
        'dayjs/plugin/weekOfYear',
        'dayjs/plugin/quarterOfYear',
        'dayjs/plugin/localeData',
        'nprogress',
        'vue-i18n',
        '@antv/data-set',
        'store',
        'webpack-theme-color-replacer/client',
        'md5',
        'moment',
      ],
      esbuildOptions: {
        loader: {
          '.js': 'jsx'
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
      port: 8000,
      proxy: {
        '/v1': {
          target: apiBaseUrl,
          changeOrigin: true
        },
        '/auth': {
          target: apiBaseUrl,
          changeOrigin: true
        },
        '/user': {
          target: apiBaseUrl,
          changeOrigin: true
        },
        '/resource': {
          target: apiBaseUrl,
          changeOrigin: true
        }
      }
    }
  }
})
