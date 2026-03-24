// eslint-disable-next-line
import { UserLayout, BasicLayout, RouteView } from '@/layouts'

/**
 * 基础路由
 * Vue Router 4 使用 pathMatch(.*)* 代替 *
 * @type { *[] }
 */
export const constantRouterMap = [
  {
    path: '/',
    name: 'index',
    component: BasicLayout,
    redirect: '/dashboard/workplace',
    hidden: true,
    children: [] // 动态路由将在这里添加
  },
  {
    path: '/user',
    component: UserLayout,
    redirect: '/user/login',
    hidden: true,
    children: [
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/user/Login.vue')
      }
    ]
  },
  {
    path: '/404',
    name: '404',
    hidden: true,
    component: () => import('@/views/exception/404.vue')
  }

  // 注意：catch-all 路由将在 permission.js 中动态添加，以确保动态路由优先匹配
]