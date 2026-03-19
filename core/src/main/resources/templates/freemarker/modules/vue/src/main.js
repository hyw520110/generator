// ie polyfill
import '@babel/polyfill'

import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store/'
import { VueAxios } from './utils/request'
// import vueRouter from 'vue-router'
// mock
// import './mock'

import bootstrap from './core/bootstrap'
import './core/use'
import './permission' // permission control
import './utils/filter' // global filter

Vue.config.productionTip = false

// mount axios Vue.$http and this.$http
Vue.use(VueAxios)

new Vue({
  router,
  store,
  created () {
    bootstrap()
  },
  render: h => h(App)
}).$mount('#app')

// const originalPush = vueRouter.prototype.push
// vueRouter.prototype.push = function push (location, onResolve, onReject) {
//  if (onResolve || onReject) return originalPush.call(this, location, onResolve, onReject)
//  return originalPush.call(this, location).catch(err => console.log('catch error ', err))
// }
