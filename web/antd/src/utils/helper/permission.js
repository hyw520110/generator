export const PERMISSION_ENUM = {
  'add': { key: 'add', label: '新增' },
  'delete': { key: 'delete', label: '删除' },
  'edit': { key: 'edit', label: '修改' },
  'query': { key: 'query', label: '查询' },
  'get': { key: 'get', label: '详情' },
  'enable': { key: 'enable', label: '启用' },
  'disable': { key: 'disable', label: '禁用' },
  'import': { key: 'import', label: '导入' },
  'export': { key: 'export', label: '导出' }
}

export default {
  install (app) {
    // $auth 权限检查方法
    app.config.globalProperties.$auth = function (permissions) {
      const [permission, action] = permissions.split('.')
      const permissionList = this.$store?.getters?.roles?.permissions || []
      const found = permissionList.find((val) => {
        return val.permissionId === permission
      })
      if (!found) return false
      return found.actionList?.findIndex((val) => {
        return val === action
      }) > -1
    }

    // $enum 枚举获取方法
    app.config.globalProperties.$enum = function (val) {
      let result = PERMISSION_ENUM
      val && val.split('.').forEach(v => {
        result = result && result[v] || null
      })
      return result
    }
  }
}