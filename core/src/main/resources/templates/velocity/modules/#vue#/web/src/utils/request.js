import axios from 'axios'
import store from '@/store'
import { VueAxios } from './axios'
import notification from 'ant-design-vue/es/notification'
import { ACCESS_TOKEN, USER_ID } from '@/store/mutation-types'

// 从环境变量读取后端服务器配置，动态拼接
const apiHost = import.meta.env.VITE_API_HOST || 'localhost'
const apiPort = import.meta.env.VITE_API_PORT || '8082'
const baseHost =  `http://${apiHost}:${apiPort}`
const uploadUrl = baseHost + '/v1/common/upload'

// 创建axios实例，设置请求超时时间(正式环境时长根据实际调整)
const service = axios.create({
  baseURL: baseHost,
  timeout: 25000
})

const err = (error) => {
  const data = error.data || (error.response && error.response.data)

  if (data) {
    switch (data.status) {
      case 90000:
      case 90001:
      case 90002:
      case 90003:
        store.dispatch('ClearUser').then(() => {
          setTimeout(() => {
            window.location.reload()
          }, 500)
        })
    }
    notification.error({
      message: '操作失败',
      description: data.message || '未知错误'
    })
  } else {
    notification.error({
      key: 'NETWORK_ERROR',
      message: '连接服务器失败',
      description: '请检查您的网络连接配置'
    })
  }
  return Promise.reject(error)
}

// request interceptor
service.interceptors.request.use(config => {
  const token = localStorage.getItem(ACCESS_TOKEN)
  const userId = localStorage.getItem(USER_ID)
  if (token) {
    config.headers['X-USER-TOKEN'] = token // 让每个请求携带自定义 token 请根据实际情况自行修改
  }
  if (userId) {
    config.headers['X-USER-ID'] = userId
  }
  config.headers['X-SOURCE'] = 'web'
  return config
}, err)

// response interceptor
service.interceptors.response.use((response) => {
  const data = response.data
  // 增加对 0 和 200 状态码的支持 (常用 Mock 和 标准 HTTP 状态码)
  if (data && (data.status === 10000 || data.status === 0 || data.status === 200)) {
    // 兼容处理：如果返回的是 data 字段但代码期望 result，则添加 result 字段
    if (data.data !== undefined && data.result === undefined) {
      data.result = data.data
    }
    return data
  } else {
    return err(response)
  }
}, err)

const installer = {
  vm: {},
  install (app) {
    app.use(VueAxios, service)
  }
}

export {
  installer as VueAxios,
  service as axios,
  uploadUrl,
  baseHost
}
