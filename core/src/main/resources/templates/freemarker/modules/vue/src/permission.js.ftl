import router from './router'
import store from './store'
import * as storageModule from 'store'
import NProgress from 'nprogress' // progress bar
import 'nprogress/nprogress.css' // progress bar style
import notification from 'ant-design-vue/es/notification'
import { setDocumentTitle, domTitle } from '@/utils/domUtil'
import { ACCESS_TOKEN, USER_ID } from '@/store/mutation-types'
import { refreshUserAuthCache } from '@/api/login'

// 获取 store 的默认导出
const storage = storageModule.default || storageModule

NProgress.configure({ showSpinner: false }) // NProgress Configuration

const whiteList = ['login', 'register', 'registerResult'] // no redirect whitelist

router.beforeEach(async (to, from, next) => {
  const userToken = storage.get(ACCESS_TOKEN)
  const userId = storage.get(USER_ID)
  NProgress.start() // start progress bar
  to.meta && (typeof to.meta.title !== 'undefined' && setDocumentTitle(`${'$'}{to.meta.title} - ${'$'}{domTitle}`))

  if (userToken && userId) {
    /* has token */
    // 判断当前用户是否资源列表为空，是则开始获取资源列表 (即权限列表)
    if (store.getters.resources.length === 0) {
      try {
        const res = await store.dispatch('GetUserResources', userId)
        await store.dispatch('GeneratorDynamicRouter', res)
        // 动态添加可访问路由表 (Vue Router 4 使用 addRoute)
        store.getters.addRouters.forEach(route => {
          router.addRoute(route)
        })
        // 刷新用户权限相关缓存
        refreshUserAuthCache(userId)

        // 如果是访问登录页或根路径，跳转到首页，否则重新导航到目标路由
        if (to.path === '/user/login' || to.path === '/') {
          next({ path: '/dashboard/workplace', replace: true })
        } else {
          // hack 方法：重新导航到目标路由，确保动态路由已生效
          next({ ...to, replace: true })
        }
      } catch (e) {
        notification.error({
          message: '错误',
          description: '请求用户信息失败，请重试'
        })
        await store.dispatch('Logout')
        next({ path: '/user/login', query: { redirect: to.fullPath } })
      }
    } else {
      // 已有资源列表，如果访问根路径或登录页则跳转到首页
      if (to.path === '/' || to.path === '/user/login') {
        next({ path: '/dashboard/workplace', replace: true })
      } else {
        next()
      }
    }
  } else {
    // 未登录
    if (whiteList.includes(to.name)) {
      // 在免登录白名单，直接进入
      next()
    } else {
      next({ path: '/user/login' })
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done() // finish progress bar
})