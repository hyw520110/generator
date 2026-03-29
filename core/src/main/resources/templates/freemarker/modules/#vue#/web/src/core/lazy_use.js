// Vue 3 兼容层 - viser-vue 暂不兼容 Vue 3，使用空对象替代
// import Viser from 'viser-vue'

// base library
import '@/core/lazy_lib/components_use'

// ext library
import VueClipboard from 'vue-clipboard2'
import PermissionHelper from '@/utils/helper/permission'
import './directives/action'
import config from '@/config/defaultSettings'
import { createApp } from 'vue'

VueClipboard.config.autoSetContainer = true

// Viser 暂不兼容 Vue 3，创建空插件
const Viser = {
  install (app) {
    // viser-vue 暂不支持 Vue 3
    console.warn('viser-vue is not compatible with Vue 3, chart components will not work')
  }
}

// Vue 3 安装函数
export function setupLazyUse (app) {
  app.use(Viser)
  app.use(VueClipboard)
  app.use(PermissionHelper)
}

export default Viser