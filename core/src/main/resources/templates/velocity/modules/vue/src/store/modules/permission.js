import { constantRouterMap } from '@/config/router.config'
// eslint-disable-next-line
import { BasicLayout, RouteView, BlankLayout, PageView } from '@/layouts'

// 动态组件加载映射 - Vite 的 import.meta.glob 返回的 key 格式为相对路径
const modules = import.meta.glob('/src/views/**/*.vue')

// 调试：打印可用的组件路径
console.log('[permission] 可用组件:', Object.keys(modules))

// 前端路由表
const constantRouterComponents = {
  // 基础页面 layout 必须引入
  BasicLayout: BasicLayout,
  BlankLayout: BlankLayout,
  RouteView: RouteView,
  PageView: PageView
  // ...more
}

// 前端未找到页面路由（Vue Router 4 语法）
const notFoundRouter = {
  path: '/:pathMatch(.*)*', redirect: '/404', hidden: true
}

export const routerFilter = (routerMap) => {
  return routerMap
    .filter(item => {
      if (item.childResources && item.childResources.length > 0) {
        item.childResources = routerFilter(item.childResources)
      }

      // 剔除掉按钮级的资源
      if (item.resourceType !== 'O') {
        return true
      }
    })
}

export const generator = (routerMap, parent) => {
  return routerFilter(routerMap)
    .map(item => {
      let path
      const reURL = /^(http|https|ftp|file):\/\/.+$/

      if (reURL.test(item.resourceUri)) {
        path = item.resourceUri
      } else {
        path = `${parent && parent.resourceUri || ''}/${item.resourceUri}`

        // 为了防止出现后端返回结果不规范，处理有可能出现拼接出多个反斜杠
        path = path.replace(/\/+/g, '/')
      }

      let routerComponent = constantRouterComponents[item.resourceView]
      if (!routerComponent && item.resourceView) {
        // Vite 动态导入 - 使用绝对路径格式
        const viewPath = `/src/views/${item.resourceView}.vue`
        const indexPath = `/src/views/${item.resourceView}/index.vue`
        
        // 直接匹配
        routerComponent = modules[viewPath] || modules[indexPath]
        
        // 如果没找到，尝试大小写不敏感匹配
        if (!routerComponent) {
          const viewPathLower = viewPath.toLowerCase()
          const indexPathLower = indexPath.toLowerCase()
          const key = Object.keys(modules).find(k => 
            k.toLowerCase() === viewPathLower || k.toLowerCase() === indexPathLower
          )
          if (key) {
            routerComponent = modules[key]
          }
        }

        if (!routerComponent) {
          console.error(`组件不存在: ${item.resourceView}，尝试路径: ${viewPath}`)
        }
      }

      const currentRouter = {
        // 路由地址 动态拼接生成
        path: path,
        // 路由名称，建议唯一
        name: item.resourceKey,
        // 该路由对应页面的 组件
        component: routerComponent,
        // meta: 页面标题, 菜单图标, 页面权限(供指令权限用，可去掉)
        meta: { title: item.resourceName, icon: item.resourceIcon || undefined, permission: item.resourceKey && [ item.resourceKey ] || null, url: reURL.test(item.resourceRedirect) ? item.resourceRedirect : undefined }
      }
      // 重定向
      item.resourceRedirect && !reURL.test(item.resourceRedirect) && (currentRouter.redirect = item.resourceRedirect)
      // 是否有子菜单，并递归处理
      if (item.childResources && item.childResources.length > 0) {
        // Recursion
        currentRouter.children = generator(item.childResources, item)
      }
      return currentRouter
    })
}

const permission = {
  state: {
    routers: constantRouterMap,
    addRouters: []
  },
  mutations: {
    SET_ROUTERS: (state, routers) => {
      state.addRouters = routers
      state.routers = constantRouterMap.concat(routers)
    }
  },
  actions: {
    GeneratorDynamicRouter ({ commit }, res) {
      return new Promise(resolve => {
        const resources = res.data
        if (resources != null && resources.length > 0) {
          const assign = JSON.parse(JSON.stringify(resources))
          const routers = generator(assign)
          routers.push(notFoundRouter)
          commit('SET_ROUTERS', routers)
        }

        resolve()
      })
    }
  }
}

export default permission
