// Vue 3 插件注册 - 在 main.js 中使用

// base library
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/antd.less'

// ext library
import MultiTab from '@/components/MultiTab'
import PageLoading from '@/components/PageLoading'
import PermissionHelper from '@/utils/helper/permission'

import './directives/action'

export default {
  install (app) {
    app.use(Antd)
    app.use(MultiTab)
    app.use(PageLoading)
    app.use(PermissionHelper)
  }
}
