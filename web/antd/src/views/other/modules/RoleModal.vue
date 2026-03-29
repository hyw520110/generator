<template>
  <a-modal
    title="操作"
    :width="800"
    :open="visible"
    :confirmLoading="confirmLoading"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-steps :current="1">
      <a-step>
        <template #title>
          Finished
        </template>
        <template #description>
          This is a description.
        </template>
      </a-step>
      <a-step title="In Progress" description="This is a description."/>
      <a-step title="Waiting" description="This is a description."/>
    </a-steps>
  </a-modal>
</template>

<script>
import { ref, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { getPermissions } from '@/api/manage'
import { actionToObject } from '@/utils/permissions'

export default {
  name: 'RoleModal',
  emits: ['close', 'ok'],
  setup (props, { emit }) {
    const visible = ref(false)
    const confirmLoading = ref(false)
    const mdl = ref({})
    const permissions = ref([])
    
    const formState = ref({
      id: '',
      name: '',
      status: undefined,
      describe: ''
    })
    
    const labelCol = { xs: { span: 24 }, sm: { span: 5 } }
    const wrapperCol = { xs: { span: 24 }, sm: { span: 16 } }
    
    const add = () => {
      edit({ id: 0 })
    }
    
    const edit = (record) => {
      mdl.value = Object.assign({}, record)
      visible.value = true
      
      if (mdl.value.permissions && permissions.value) {
        const permissionsAction = {}
        mdl.value.permissions.forEach(permission => {
          permissionsAction[permission.permissionId] = permission.actionEntitySet.map(entity => entity.action)
        })
        permissions.value.forEach(permission => {
          permission.selected = permissionsAction[permission.id] || []
        })
      }
      
      nextTick(() => {
        formState.value = {
          id: record.id,
          name: record.name,
          status: record.status,
          describe: record.describe
        }
      })
    }
    
    const close = () => {
      emit('close')
      visible.value = false
    }
    
    const handleOk = () => {
      confirmLoading.value = true
      new Promise((resolve) => {
        setTimeout(() => resolve(), 2000)
      }).then(() => {
        message.success('保存成功')
        emit('ok')
      }).catch(() => {
        // Do something
      }).finally(() => {
        confirmLoading.value = false
        close()
      })
    }
    
    const handleCancel = () => {
      close()
    }
    
    const loadPermissions = () => {
      getPermissions().then(res => {
        const result = res.result
        permissions.value = result.map(permission => {
          const options = actionToObject(permission.actionData)
          permission.checkedAll = false
          permission.selected = []
          permission.indeterminate = false
          permission.actionsOptions = options.map(option => {
            return {
              label: option.describe,
              value: option.action
            }
          })
          return permission
        })
      })
    }
    
    onMounted(() => {
      loadPermissions()
    })
    
    return {
      labelCol,
      wrapperCol,
      visible,
      confirmLoading,
      mdl,
      permissions,
      formState,
      add,
      edit,
      close,
      handleOk,
      handleCancel
    }
  }
}
</script>

<style scoped>

</style>