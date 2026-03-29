import { isIE } from '@/utils/util'
import Mock from 'mockjs2'
import './services/auth'
import './services/user'
import './services/manage'
import './services/other'
import './services/tagCloud'
import './services/article'

// 判断环境不是 prod 或者 preview 是 true 时，加载 mock 服务
if (process.env.NODE_ENV !== 'production' || import.meta.env.VITE_APP_PREVIEW === 'true') {
  if (isIE()) {
    console.error('[antd-pro] ERROR: `mockjs` NOT SUPPORT `IE` PLEASE DO NOT USE IN `production` ENV.')
  }
  console.log('[antd-pro] mock mounting')

  Mock.setup({
    timeout: 800 // setter delay time
  })
  console.log('[antd-pro] mock mounted')
}