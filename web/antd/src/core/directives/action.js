import store from '@/store'

/**
 * Action 权限指令
 * 指令用法：
 *  - 在需要控制 action 级别权限的组件上使用 v-action:[method]
 *  - 例如:
 *    <a v-action:delete @click="delete(record)">删除</a>
 *    <a v-action:edit @click="edit(record)">修改</a>
 *
 *  - 当前用户没有权限时，组件上使用了该指令则会被隐藏
 */
const action = {
  mounted: function (el, binding) {
    const actionName = binding.arg
    const roles = store.getters.roles || {}
    const permissionId = binding.value || []
    
    // 如果不是数组，转换为数组
    const elVal = Array.isArray(permissionId) ? permissionId : [permissionId]
    
    // 检查权限
    if (roles.permissions && Array.isArray(roles.permissions)) {
      let hasPermission = false
      roles.permissions.forEach(p => {
        if (elVal.includes(p.permissionId)) {
          if (p.actionList && p.actionList.includes(actionName)) {
            hasPermission = true
          }
        }
      })
      
      if (!hasPermission) {
        el.style.display = 'none'
      }
    }
  }
}

export default action
