import router from './router'
import store from './store'
import storage from 'store'
import NProgress from 'nprogress' // progress bar
import 'nprogress/nprogress.css' // progress bar style
import notification from 'ant-design-vue/es/notification'
import { setDocumentTitle, domTitle } from '@/utils/domUtil'
import { ACCESS_TOKEN, USER_ID } from '@/store/mutation-types'
import { refreshUserAuthCache } from '@/api/login'

NProgress.configure({ showSpinner: false }) // NProgress Configuration

const whiteList = ['login', 'register', 'registerResult'] // no redirect whitelist

// 递归获取第一个可用的路由路径
function getFirstAvailablePath (routes) {
  if (!routes || routes.length === 0) {
    return '/dashboard/workplace' // 默认回退路径
  }
  for (const route of routes) {
    // 如果有重定向配置，使用重定向路径
    if (route.redirect) {
      return route.redirect
    }
    // 如果有子路由，递归查找
    if (route.children && route.children.length > 0) {
      const childPath = getFirstAvailablePath(route.children)
      if (childPath) {
        return childPath
      }
    }
    // 如果没有子路由且有 path，返回当前路径
    if (route.path && !route.children) {
      return route.path
    }
  }
  return '/dashboard/workplace' // 默认回退路径
}

router.beforeEach(async (to, from, next) => {
  const userToken = storage.get(ACCESS_TOKEN)
  const userId = storage.get(USER_ID)
  NProgress.start() // start progress bar
  to.meta && (typeof to.meta.title !== 'undefined' && setDocumentTitle(`${to.meta.title} - ${domTitle}`))

  if (userToken && userId) {
    /* has token */
    // 判断当前用户是否资源列表为空，是则开始获取资源列表 (即权限列表)
    if (store.getters.resources.length === 0) {
      try {
        const res = await store.dispatch('GetUserResources', userId)
        await store.dispatch('GeneratorDynamicRouter', res)
        // 动态添加可访问路由表 (Vue Router 4 使用 addRoute)
        // Vue Router 4: 直接添加顶级路由，而不是作为子路由添加
        // 因为 path 以 / 开头的路由会被视为顶级路由
        const routes = store.getters.addRouters
        routes.forEach(route => {
          router.addRoute(route)
        })
        // 刷新用户权限相关缓存
        refreshUserAuthCache(userId)

        // 获取第一个可用的重定向路径（从动态路由中获取）
        const redirectPath = getFirstAvailablePath(routes)

        // 如果是访问登录页或根路径，跳转到首页，否则重新导航到目标路由
        if (to.path === '/user/login' || to.path === '/') {
          next({ path: redirectPath, replace: true })
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
