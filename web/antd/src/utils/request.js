import axios from 'axios'
import store from '@/store'
import storage from 'store'
import notification from 'ant-design-vue/es/notification'
import { VueAxios } from './axios'
import { ACCESS_TOKEN } from '@/store/mutation-types'

// 开发环境使用相对路径，通过 Vite 代理转发；生产环境使用完整 URL
const isDev = import.meta.env.DEV
const baseHost = isDev ? '/api' : (import.meta.env.VITE_API_BASE_URL || '')
const generatorClientIdKey = 'GENERATOR_CLIENT_ID'

const createClientId = () => {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') {
    return window.crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

const getGeneratorClientId = () => {
  let clientId = window.localStorage.getItem(generatorClientIdKey)
  if (!clientId) {
    clientId = createClientId()
    window.localStorage.setItem(generatorClientIdKey, clientId)
  }
  document.cookie = `${generatorClientIdKey}=${encodeURIComponent(clientId)}; path=/; max-age=31536000; SameSite=Lax`
  return clientId
}

// 创建 axios 实例
const request = axios.create({
  // API 请求的默认前缀
  baseURL: baseHost, // process.env.VUE_APP_API_BASE_URL,
  timeout: 6000 // 请求超时时间
})

// 异常拦截处理器
const errorHandler = (error) => {
  if (error.response) {
    const data = error.response.data
    // 从 localstorage 获取 token
    const token = storage.get(ACCESS_TOKEN)
    if (error.response.status === 403) {
      notification.error({
        message: 'Forbidden',
        description: data.message
      })
    }
    if (error.response.status === 401 && !(data.result && data.result.isLogin)) {
      notification.error({
        message: 'Unauthorized',
        description: 'Authorization verification failed'
      })
      if (token) {
        store.dispatch('Logout').then(() => {
          setTimeout(() => {
            window.location.reload()
          }, 1500)
        })
      }
    }
  }
  return Promise.reject(error)
}

// request interceptor
request.interceptors.request.use(config => {
  const token = storage.get(ACCESS_TOKEN)
  // 如果 token 存在
  // 让每个请求携带自定义 token 请根据实际情况自行修改
  if (token) {
    config.headers['X-USER-TOKEN'] = token
  }
  config.headers['X-Generator-Client-Id'] = getGeneratorClientId()
  return config
}, errorHandler)

// response interceptor
request.interceptors.response.use((response) => {
  return response.data
}, errorHandler)

const installer = {
  vm: {},
  install (Vue) {
    Vue.use(VueAxios, request)
  }
}

export default request

export {
  installer as VueAxios,
  request as axios
}
