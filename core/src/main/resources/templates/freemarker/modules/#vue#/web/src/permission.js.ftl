import router from './router'
import store from './store'
import storage from 'store'
import NProgress from 'nprogress' // progress bar
import 'nprogress/nprogress.css' // progress bar style
import notification from 'ant-design-vue/es/notification'
import { setDocumentTitle, domTitle } from '@/utils/domUtil'
import { ACCESS_TOKEN } from '@/store/mutation-types'

NProgress.configure({ showSpinner: false }) // NProgress Configuration

const whiteList = ['login', 'register', 'registerResult'] // no redirect whitelist

// 递归获取第一个可用的路由路径
function getFirstAvailablePath (routes, parentPath = '') {
  if (!routes || routes.length === 0) {
    return '/index/dashboard' // 默认回退路径
  }
  for (const route of routes) {
    // 跳过隐藏路由
    if (route.hidden === true) {
      continue
    }
    // 构建完整路径：子路由的 path 是相对路径（没有前导斜杠）
    const routePath = route.path.startsWith('/') ? route.path : route.path
    const fullPath = parentPath ? `${r"${parentPath}"}/${r"${routePath}"}`.replace(/\/+/g, '/') : `/${r"${routePath}"}`
    
    // 如果有子路由，递归查找
    if (route.children && route.children.length > 0) {
      const childPath = getFirstAvailablePath(route.children, fullPath)
      if (childPath) {
        return childPath
      }
    }
    
    // 如果没有子路由且有 path（不是外部 URL），返回当前路径
    if (route.path && !route.children && !route.path.startsWith('http')) {
      return fullPath
    }
  }
  // 如果都没有，返回默认路径
  return parentPath ? `${r"${parentPath}"}/dashboard` : '/index/dashboard'
}

router.beforeEach(async (to, from, next) => {
  const userToken = storage.get(ACCESS_TOKEN)
  NProgress.start() // start progress bar
  to.meta && (typeof to.meta.title !== 'undefined' && setDocumentTitle(`${r"${to.meta.title}"} - ${r"${domTitle}"}`))

  if (userToken) {
    /* has token */
    // 判断当前用户是否资源列表为空，是则开始生成动态路由
    if (store.getters.resources.length === 0) {
      // 资源列表为空，说明登录失败或 token 失效，跳转到登录页
      notification.error({
        message: '错误',
        description: '登录状态已失效，请重新登录'
      })
      await store.dispatch('Logout')
      next({ path: '/user/login' })
    } else if (store.getters.addRouters.length === 0) {
      // 资源列表存在但动态路由未生成，生成动态路由
      try {
        await store.dispatch('GeneratorDynamicRouter', { data: store.getters.resources })
        // 动态添加可访问路由表 (Vue Router 4 使用 addRoute)
        const routes = store.getters.addRouters
        routes.forEach(route => {
          router.addRoute(route)
        })

        // 获取第一个可用的重定向路径
        const redirectPath = getFirstAvailablePath(routes)
        console.log('动态路由已添加，重定向到:', redirectPath)

        // 直接重定向到第一个可用路径
        next({ path: redirectPath, replace: true })
      } catch (e) {
        console.error('生成动态路由失败:', e)
        notification.error({
          message: '错误',
          description: '生成路由失败，请重试'
        })
        await store.dispatch('Logout')
        next({ path: '/user/login' })
      }
    } else {
      // 已有资源列表和动态路由，直接放行
      next()
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
