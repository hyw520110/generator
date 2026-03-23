import Mock from 'mockjs2'
import { builder } from '../util'

// 模拟用户资源（菜单）数据
// 注意：实际项目中，菜单数据应由后端 API 返回
// 此处仅作为开发环境 mock 数据示例
const getUserResources = () => {
  const resources = [
    {
      resourceKey: 'index',
      resourceName: '首页',
      resourceIcon: 'home',
      resourceUri: '/',
      resourceView: 'BasicLayout',
      resourceRedirect: '/dashboard/workplace',
      resourceType: 'M',
      childResources: [
        {
          resourceKey: 'dashboard',
          resourceName: '仪表盘',
          resourceIcon: 'dashboard',
          resourceUri: 'dashboard',
          resourceView: 'RouteView',
          resourceType: 'M',
          childResources: [
            {
              resourceKey: 'workplace',
              resourceName: '工作台',
              resourceIcon: 'none',
              resourceUri: 'workplace',
              resourceView: 'dashboard/Workplace',
              resourceType: 'M',
              childResources: []
            },
            {
              resourceKey: 'analysis',
              resourceName: '分析页',
              resourceIcon: 'none',
              resourceUri: 'analysis',
              resourceView: 'dashboard/Analysis',
              resourceType: 'M',
              childResources: []
            }
          ]
        }
        // 生成的页面菜单（如 SysUser, SysRole 等）应由后端 API 动态返回
        // 此处不包含生成页面的 mock 数据
      ]
    }
  ]

  return builder(resources)
}

Mock.mock(/\/resource\/.*\/list/, 'get', getUserResources)