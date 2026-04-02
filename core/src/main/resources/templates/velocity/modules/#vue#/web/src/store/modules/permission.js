import { markRaw, defineAsyncComponent } from 'vue'
import { constantRouterMap } from '@/config/router.config'
// eslint-disable-next-line
import { BasicLayout, RouteView, BlankLayout, PageView, IframeView } from '@/layouts'

// 动态组件加载映射 - Vite 的 import.meta.glob 返回的 key 格式为相对路径
// 使用 eager: false 保持懒加载特性
const modules = import.meta.glob('/src/views/**/*.vue')

// 从环境变量获取后端服务器地址
const apiHost = import.meta.env.VITE_API_HOST || 'localhost'
const apiPort = import.meta.env.VITE_API_PORT || '8082'
const baseHost = `http://\${apiHost}:\${apiPort}`

// 包装动态组件，确保 Vue Router 能正确处理
const wrapDynamicComponent = (importFn) => {
  // 返回一个异步组件定义，并使用 markRaw 防止被 Vuex 响应式包装
  return markRaw(defineAsyncComponent({
    loader: importFn,
    loadingComponent: null,
    errorComponent: null,
    delay: 0
  }))
}

// 前端路由表 - 使用 markRaw 防止组件被 Vuex 的响应式系统包装
const constantRouterComponents = {
  // 基础页面 layout 必须引入
  BasicLayout: markRaw(BasicLayout),
  BlankLayout: markRaw(BlankLayout),
  RouteView: markRaw(RouteView),
  PageView: markRaw(PageView),
  IframeView: markRaw(IframeView)
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

export const generator = (routerMap, parentPath = '') => {
  return routerFilter(routerMap)
    .map(item => {
      let path
      const reURL = /^(http|https|ftp|file):\/\/.+$/

      if (reURL.test(item.resourceUri)) {
        path = item.resourceUri
      } else {
        // 构建完整路径用于重定向和子路由递归
        const fullPath = `${parentPath}/${item.resourceUri}`.replace(/\/+/g, '/')
        
        // Vue Router 4: 子路由的 path 不应该以 / 开头
        // 如果有父路径，使用相对路径；否则使用绝对路径
        if (parentPath) {
          // 提取 resourceUri 中的最后一段作为相对路径
          path = item.resourceUri.replace(/^\//, '')
        } else {
          path = fullPath
        }
      }

      // 特殊处理：IframeView 可能因后端数据空格或大小写问题导致匹配失败
      const resourceView = item.resourceView ? item.resourceView.trim() : null
      
      let routerComponent = constantRouterComponents[resourceView]
      // 特殊处理：IframeView 可能因后端数据空格或大小写问题导致匹配失败
      if (!routerComponent && resourceView) {
        // 先尝试精确匹配
        routerComponent = constantRouterComponents[resourceView]
        
        // 如果精确匹配失败，尝试大小写不敏感匹配
        if (!routerComponent) {
          const viewKey = Object.keys(constantRouterComponents).find(key => 
            key.toLowerCase() === resourceView.toLowerCase()
          )
          if (viewKey) {
            routerComponent = constantRouterComponents[viewKey]
          }
        }
      }
      
      if (!routerComponent && resourceView) {
        // Vite 动态导入 - 尝试多种可能的路径格式
        const possiblePaths = [
          `/src/views/${resourceView}.vue`,
          `/src/views/${resourceView}/index.vue`,
          `/src/views/${resourceView.toLowerCase()}.vue`,
          `/src/views/${resourceView.toLowerCase()}/index.vue`
        ]

        // 直接匹配 - 获取动态导入函数
        let importFn = null
        for (const p of possiblePaths) {
          if (modules[p]) {
            importFn = modules[p]
            break
          }
        }

        // 如果没找到，尝试模糊匹配
        if (!importFn) {
          const resourceViewLower = item.resourceView.toLowerCase()
          const key = Object.keys(modules).find(k =>
            k.toLowerCase().includes(resourceViewLower)
          )
          if (key) {
            importFn = modules[key]
          }
        }

        if (importFn) {
          // 使用 defineAsyncComponent 包装动态组件
          routerComponent = wrapDynamicComponent(importFn)
        } else {
          console.error(`组件不存在：${item.resourceView}`, {
            possiblePaths,
            availableModules: Object.keys(modules).filter(k => k.toLowerCase().includes(item.resourceKey?.toLowerCase())).slice(0, 5)
          })
        }
      }

      // 计算完整路径用于重定向和子路由递归
      const fullPath = `${parentPath}/${item.resourceUri}`.replace(/\/+/g, '/')
      
      const currentRouter = {
        // 路由地址 动态拼接生成
        path: path,
        // 路由名称，建议唯一
        name: item.resourceKey,
        // 该路由对应页面的 组件
        component: routerComponent,
        // meta: 页面标题，菜单图标，页面权限 (供指令权限用，可去掉)
        meta: {
          title: item.resourceName,
          icon: item.resourceIcon || undefined,
          permission: item.resourceKey && [ item.resourceKey ] || null,
          url: item.resourceRedirect || undefined
        }
      }
      // 重定向：只有内部相对路径才设置为 redirect
      // 特殊处理：如果资源视图是 IframeView，不能设置路由重定向，否则会因为前端不存在该路径而跳转 404
      if (item.resourceRedirect && !reURL.test(item.resourceRedirect) && resourceView !== 'IframeView') {
        currentRouter.redirect = item.resourceRedirect
      }
      // 是否有子菜单，并递归处理
      if (item.childResources && item.childResources.length > 0) {
        // Recursion - 传递当前生成的完整路径给子路由
        currentRouter.children = generator(item.childResources, fullPath)
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

          // 添加 404 路由
          routers.push(notFoundRouter)

          // 提交时保存动态路由列表
          commit('SET_ROUTERS', routers)
        }

        resolve()
      })
    }
  }
}

export default permission