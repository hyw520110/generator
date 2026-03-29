// eslint-disable-next-line
import { UserLayout } from '@/layouts'

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
    path: '/404',
    hidden: true,
    component: () => import('@/views/exception/404.vue')
  },

  // Vue Router 4 catch-all route
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
    hidden: true
  }
]
