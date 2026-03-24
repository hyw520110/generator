import Mock from 'mockjs2'
import { builder } from '../util'

// 模拟用户资源（菜单）数据
// 修改：将首页下的菜单改为和首页同级别
const getUserResources = () => {
  // 返回匹配 store/modules/permission.js 期望格式的数据
  // 注意：resourceView 需要匹配实际的文件名（大小写敏感）
  const resources = [
    // 工作台 - 与首页同级别
    {
      resourceKey: 'workplace',
      resourceName: '工作台',
      resourceIcon: 'dashboard',
      resourceUri: 'dashboard/workplace',
      resourceView: 'dashboard/Workplace',
      resourceType: 'M',
      childResources: []
    },
    // 分析页 - 与首页同级别
    {
      resourceKey: 'analysis',
      resourceName: '分析页',
      resourceIcon: 'dashboard',
      resourceUri: 'dashboard/analysis',
      resourceView: 'dashboard/Analysis',
      resourceType: 'M',
      childResources: []
    },
    // 用户管理 - 与首页同级别
    {
      resourceKey: 'sysUser',
      resourceName: '用户管理',
      resourceIcon: 'user',
      resourceUri: 'sys/user',
      resourceView: 'SysUser/SysUserList',
      resourceType: 'M',
      childResources: []
    },
    // 角色管理 - 与首页同级别
    {
      resourceKey: 'sysRole',
      resourceName: '角色管理',
      resourceIcon: 'team',
      resourceUri: 'sys/role',
      resourceView: 'SysRole/SysRoleList',
      resourceType: 'M',
      childResources: []
    },
    // 菜单管理 - 与首页同级别
    {
      resourceKey: 'sysMenu',
      resourceName: '菜单管理',
      resourceIcon: 'menu',
      resourceUri: 'sys/menu',
      resourceView: 'SysMenu/SysMenuList',
      resourceType: 'M',
      childResources: []
    },
    // 部门管理 - 与首页同级别
    {
      resourceKey: 'sysDept',
      resourceName: '部门管理',
      resourceIcon: 'apartment',
      resourceUri: 'sys/dept',
      resourceView: 'SysDept/SysDeptList',
      resourceType: 'M',
      childResources: []
    },
    // 系统配置 - 与首页同级别
    {
      resourceKey: 'sysConfig',
      resourceName: '系统配置',
      resourceIcon: 'tool',
      resourceUri: 'sys/config',
      resourceView: 'SysConfig/SysConfigList',
      resourceType: 'M',
      childResources: []
    },
    // 字典类型 - 与首页同级别
    {
      resourceKey: 'sysDictType',
      resourceName: '字典类型',
      resourceIcon: 'book',
      resourceUri: 'sys/dict-type',
      resourceView: 'SysDictType/SysDictTypeList',
      resourceType: 'M',
      childResources: []
    },
    // 字典数据 - 与首页同级别
    {
      resourceKey: 'sysDictData',
      resourceName: '字典数据',
      resourceIcon: 'database',
      resourceUri: 'sys/dict-data',
      resourceView: 'SysDictData/SysDictDataList',
      resourceType: 'M',
      childResources: []
    },
    // 岗位管理 - 与首页同级别
    {
      resourceKey: 'sysPosition',
      resourceName: '岗位管理',
      resourceIcon: 'solution',
      resourceUri: 'sys/position',
      resourceView: 'SysPosition/SysPositionList',
      resourceType: 'M',
      childResources: []
    },
    // 定时任务 - 与首页同级别
    {
      resourceKey: 'sysJob',
      resourceName: '定时任务',
      resourceIcon: 'schedule',
      resourceUri: 'sys/job',
      resourceView: 'SysJob/SysJobList',
      resourceType: 'M',
      childResources: []
    },
    // 通知公告 - 与首页同级别
    {
      resourceKey: 'sysNotice',
      resourceName: '通知公告',
      resourceIcon: 'notification',
      resourceUri: 'sys/notice',
      resourceView: 'SysNotice/SysNoticeList',
      resourceType: 'M',
      childResources: []
    },
    // 操作日志 - 与首页同级别
    {
      resourceKey: 'sysOperLog',
      resourceName: '操作日志',
      resourceIcon: 'file-text',
      resourceUri: 'monitor/oper-log',
      resourceView: 'SysOperLog/SysOperLogList',
      resourceType: 'M',
      childResources: []
    },
    // 登录日志 - 与首页同级别
    {
      resourceKey: 'sysLoginLog',
      resourceName: '登录日志',
      resourceIcon: 'login',
      resourceUri: 'monitor/login-log',
      resourceView: 'SysLoginLog/SysLoginLogList',
      resourceType: 'M',
      childResources: []
    },
    // 任务日志 - 与首页同级别
    {
      resourceKey: 'sysJobLog',
      resourceName: '任务日志',
      resourceIcon: 'history',
      resourceUri: 'monitor/job-log',
      resourceView: 'SysJobLog/SysJobLogList',
      resourceType: 'M',
      childResources: []
    }
  ]

  return builder(resources)
}

Mock.mock(/\/resource\/.*\/list/, 'get', getUserResources)
