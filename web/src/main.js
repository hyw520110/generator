import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store/'
import i18n from './locales'
import Antd from 'ant-design-vue'

// mock
import './mock'

import './permission' // permission control
import './global.less'

const app = createApp(App)

app.use(Antd)
app.use(store)
app.use(router)
app.use(i18n)

app.mount('#app')
