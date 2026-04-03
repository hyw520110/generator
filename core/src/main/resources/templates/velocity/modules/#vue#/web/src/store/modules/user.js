import * as storageModule from 'store'
import { login, logout } from '@/api/login'
import { ACCESS_TOKEN } from '@/store/mutation-types'

// 获取 store 的默认导出
const storage = storageModule.default || storageModule

// 用户资源列表的 storage key
const USER_RESOURCES = 'user-resources'

const user = {
  state: {
    token: '',
    name: '',
    welcome: '',
    avatar: '',
    roles: [],
    resources: storage.get(USER_RESOURCES) || [],
    info: {}
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_NAME: (state, { name, welcome }) => {
      state.name = name
      state.welcome = welcome
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles
    },
    SET_RESOURCES: (state, resources) => {
      state.resources = resources
      storage.set(USER_RESOURCES, resources, 7 * 24 * 60 * 60 * 1000)
    },
    SET_INFO: (state, info) => {
      state.info = info
    }
  },

  actions: {
    // 登录
    Login ({ commit }, userInfo) {
      return new Promise((resolve, reject) => {
        login(userInfo).then(response => {
          const result = response.data
          storage.set(ACCESS_TOKEN, result.userToken, 7 * 24 * 60 * 60 * 1000)
          commit('SET_TOKEN', result.userToken)
          // 登录时直接保存资源列表
          if (result.userResources && result.userResources.length > 0) {
            commit('SET_RESOURCES', result.userResources)
          }
          resolve(result)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 登出
    Logout ({ commit }) {
      return new Promise((resolve, reject) => {
        logout()
          .then(() => {
            resolve()
          })
          .catch(error => {
            reject(error)
          })
          .finally(() => {
            commit('SET_TOKEN', '')
            commit('SET_ROLES', [])
            commit('SET_RESOURCES', [])
            storage.remove(ACCESS_TOKEN)
            storage.remove(USER_RESOURCES)
          })
      })
    },

    // 清除用户信息
    ClearUser ({ commit }) {
      return new Promise((resolve) => {
        commit('SET_TOKEN', '')
        commit('SET_ROLES', [])
        commit('SET_RESOURCES', [])
        storage.remove(ACCESS_TOKEN)
        storage.remove(USER_RESOURCES)
        resolve()
      })
    }
  }
}

export default user