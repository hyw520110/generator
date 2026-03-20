import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store/'
import i18n from './locales'
import Antd from 'ant-design-vue'
import * as Icons from '@ant-design/icons-vue'

// mock
import './mock'

import './permission' // permission control
import './global.less'

const app = createApp(App)

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

app.mount('#app')
