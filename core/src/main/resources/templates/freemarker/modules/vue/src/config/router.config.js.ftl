// eslint-disable-next-line
import { UserLayout, BasicLayout } from '@/layouts'

/**
 * 基础路由
 * Vue Router 4 使用 pathMatch(.*)* 代替 *
 * @type { *[] }
 */
export const constantRouterMap = [
  {
    path: '/',
    redirect: '/user/login',
    hidden: true
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
    path: '/dashboard',
    component: BasicLayout,
    redirect: '/dashboard/workplace',
    children: [
      {
        path: 'workplace',
        name: 'Workplace',
        component: () => import('@/views/dashboard/Workplace.vue'),
        meta: { title: '工作台' }
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