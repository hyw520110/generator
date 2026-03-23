import Mock from 'mockjs2'
import { builder } from '../util'

// 模拟用户资源（菜单）数据
const getUserResources = () => {
  // 返回匹配 store/modules/permission.js 期望格式的数据
  // 注意：resourceView 需要匹配实际的文件名（大小写敏感）
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
      ]
    }
  ]

  return builder(resources)
}

Mock.mock(/\/resource\/.*\/list/, 'get', getUserResources)