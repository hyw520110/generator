// Vue 3 插件注册 - 在 main.js 中使用

// base library
import Antd from 'ant-design-vue'
// import Viser from 'viser-vue' // Vue 2 only, need replacement for Vue 3
// import VueCropper from 'vue-cropper' // Vue 2 only, need replacement for Vue 3
import 'ant-design-vue/dist/antd.less'

// ext library
// import VueClipboard from 'vue-clipboard2' // Vue 2 only, need replacement for Vue 3
import MultiTab from '@/components/MultiTab'
import PageLoading from '@/components/PageLoading'
import PermissionHelper from '@/utils/helper/permission'

// import '@/components/use'
import './directives/action'

export default {
  install (app) {
    app.use(Antd)
    // app.use(Viser) // Vue 2 only
    app.use(MultiTab)
    app.use(PageLoading)
    // app.use(VueClipboard) // Vue 2 only
    app.use(PermissionHelper)
    // app.use(VueCropper) // Vue 2 only
  }
}

process.env.NODE_ENV !== 'production' && console.warn('[antd-pro] WARNING: Antd now use fulled imported.')