import { markRaw, defineAsyncComponent } from 'vue'
import { constantRouterMap } from '@/config/router.config'
// eslint-disable-next-line
import { BasicLayout, RouteView, BlankLayout, PageView } from '@/layouts'

// 动态组件加载映射 - Vite 的 import.meta.glob 返回的 key 格式为相对路径
// 使用 eager: false 保持懒加载特性
const modules = import.meta.glob('/src/views/**/*.vue')

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
  PageView: markRaw(PageView)
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
        // 使用传入的 parentPath（已生成的完整路径）而不是原始数据的 resourceUri
        path = `${parentPath}/${item.resourceUri}`

        // 为了防止出现后端返回结果不规范，处理有可能出现拼接出多个反斜杠
        path = path.replace(/\/+/g, '/')
      }

      let routerComponent = constantRouterComponents[item.resourceView]
      if (!routerComponent && item.resourceView) {
        // Vite 动态导入 - 尝试多种可能的路径格式
        const possiblePaths = [
          `/src/views/${item.resourceView}.vue`,
          `/src/views/${item.resourceView}/index.vue`,
          `/src/views/${item.resourceView.toLowerCase()}.vue`,
          `/src/views/${item.resourceView.toLowerCase()}/index.vue`
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

      const currentRouter = {
        // 路由地址 动态拼接生成
        path: path,
        // 路由名称，建议唯一
        name: item.resourceKey,
        // 该路由对应页面的 组件
        component: routerComponent,
        // meta: 页面标题，菜单图标，页面权限 (供指令权限用，可去掉)
        meta: { title: item.resourceName, icon: item.resourceIcon || undefined, permission: item.resourceKey && [ item.resourceKey ] || null, url: reURL.test(item.resourceRedirect) ? item.resourceRedirect : undefined }
      }
      // 重定向
      item.resourceRedirect && !reURL.test(item.resourceRedirect) && (currentRouter.redirect = item.resourceRedirect)
      // 是否有子菜单，并递归处理
      if (item.childResources && item.childResources.length > 0) {
        // Recursion - 传递当前生成的完整路径给子路由
        currentRouter.children = generator(item.childResources, path)
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
