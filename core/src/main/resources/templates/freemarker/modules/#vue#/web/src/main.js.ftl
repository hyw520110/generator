import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store/'
import i18n from './locales'
import Antd from 'ant-design-vue'
import * as Icons from '@ant-design/icons-vue'
import { VueAxios } from '@/utils/request'

// mock - 开发环境启用 mock
// import './mock'

import './permission' // permission control
import './global.less'

import filters from '@/utils/filter'

const app = createApp(App)

// 注册全局过滤器为全局属性
app.config.globalProperties.$filters = filters

// 注册所有图标
Object.keys(Icons).forEach((key) => {
  if (key !== 'default') {
    app.component(key, Icons[key])
  }
})

app.use(Antd)
app.use(store)
app.use(router)
app.use(i18n)
app.use(VueAxios) // 安装 axios 插件，使 $http 全局可用

app.mount('#app')