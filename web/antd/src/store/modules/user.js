import storage from 'store'
import { login, getInfo, logout } from '@/api/login'
import { ACCESS_TOKEN } from '@/store/mutation-types'
import { welcome } from '@/utils/util'

const user = {
  state: {
    token: '',
    name: '',
    welcome: '',
    avatar: '',
    roles: [],
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
    SET_INFO: (state, info) => {
      state.info = info
    }
  },

  actions: {
    // 登录
    Login ({ commit }, userInfo) {
      return new Promise((resolve, reject) => {
        login(userInfo).then(response => {
          const data = response.data
          const token = data.userToken || data.token
          storage.set(ACCESS_TOKEN, token, 7 * 24 * 60 * 60 * 1000)
          commit('SET_TOKEN', token)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 获取用户信息
    GetInfo ({ commit }) {
      return new Promise((resolve, reject) => {
        getInfo().then(response => {
          const data = response.data

          // 适配后端返回的用户信息格式
          const userInfo = data.userInfo || data
          
          // 处理角色信息，将 roleKey 转换为 permissionId
          const userRoles = userInfo.userRoles || []
          const permissions = userRoles.map(role => role.roleKey || role.roleCode).filter(Boolean)
          
          // 如果有权限数据，设置角色信息
          if (permissions.length > 0 || userRoles.length > 0) {
            const role = {
              permissions: permissions.map(p => ({ permissionId: p, actionList: [] })),
              permissionList: permissions
            }
            commit('SET_ROLES', role)
          } else {
            // 如果没有权限，设置默认角色
            commit('SET_ROLES', { permissions: [], permissionList: [] })
          }
          
          commit('SET_INFO', userInfo)
          commit('SET_NAME', { name: userInfo.userName || userInfo.name, welcome: welcome() })
          commit('SET_AVATAR', userInfo.avatar || '')

          resolve(response)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 登出
    Logout ({ commit, state }) {
      return new Promise((resolve) => {
        logout(state.token).then(() => {
          resolve()
        }).catch(() => {
          resolve()
        }).finally(() => {
          commit('SET_TOKEN', '')
          commit('SET_ROLES', [])
          storage.remove(ACCESS_TOKEN)
        })
      })
    }

  }
}

export default user
