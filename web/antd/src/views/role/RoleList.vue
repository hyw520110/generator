<template>
  <a-card :bordered="false" :style="{ height: '100%' }">
    <a-row :gutter="24">
      <a-col :md="4">
        <a-list itemLayout="vertical" :dataSource="roles">
          <template #renderItem="{ item }">
            <a-list-item :key="item.id">
              <a-list-item-meta :style="{ marginBottom: '0' }">
                <template #description>
                  <span style="text-align: center; display: block">{{ item.describe }}</span>
                </template>
                <template #title>
                  <a style="text-align: center; display: block" @click="edit(item)">{{ item.name }}</a>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
      </a-col>
      <a-col :md="20">
        <div style="max-width: 800px">
          <a-divider v-if="isMobile()"/>
          <div v-if="mdl.id">
            <h3>角色：{{ mdl.name }}</h3>
          </div>
          <a-form :model="formState" :layout="isMobile() ? 'vertical' : 'horizontal'" ref="formRef">
            <a-form-item label="唯一键" name="id">
              <a-input v-model:value="formState.id" placeholder="请填写唯一键"/>
            </a-form-item>

            <a-form-item label="角色名称" name="name">
              <a-input v-model:value="formState.name" placeholder="请填写角色名称"/>
            </a-form-item>

            <a-form-item label="状态" name="status">
              <a-select v-model:value="formState.status">
                <a-select-option :value="1">正常</a-select-option>
                <a-select-option :value="2">禁用</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="备注说明" name="describe">
              <a-textarea :row="3" v-model:value="formState.describe" placeholder="请填写角色名称"/>
            </a-form-item>

            <a-form-item label="拥有权限">
              <a-row :gutter="16" v-for="(permission, index) in permissions" :key="index">
                <a-col :xl="4" :lg="24">
                  {{ permission.name }}：
                </a-col>
                <a-col :xl="20" :lg="24">
                  <a-checkbox
                    v-if="permission.actionsOptions.length > 0"
                    :indeterminate="permission.indeterminate"
                    :checked="permission.checkedAll"
                    @change="onChangeCheckAll($event, permission)">
                    全选
                  </a-checkbox>
                  <a-checkbox-group :options="permission.actionsOptions" v-model:value="permission.selected" @change="onChangeCheck(permission)"/>
                </a-col>
              </a-row>
            </a-form-item>
          </a-form>
        </div>
      </a-col>
    </a-row>
  </a-card>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { getRoleList, getPermissions } from '@/api/manage'
import { actionToObject } from '@/utils/permissions'

export default {
  name: 'RoleList',
  setup () {
    const formRef = ref()
    const roles = ref([])
    const permissions = ref([])
    
    const mdl = reactive({
      id: undefined,
      name: '',
      status: undefined,
      describe: '',
      permissions: []
    })
    
    const formState = reactive({
      id: '',
      name: '',
      status: undefined,
      describe: ''
    })
    
    const isMobile = () => {
      return false // Simplified - can be enhanced with actual mobile detection
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
    
    const edit = (record) => {
      Object.assign(mdl, record)
      formState.id = record.id
      formState.name = record.name
      formState.status = record.status
      formState.describe = record.describe
      
      if (mdl.permissions && permissions.value) {
        const permissionsAction = {}
        mdl.permissions.forEach(permission => {
          permissionsAction[permission.permissionId] = permission.actionEntitySet.map(entity => entity.action)
        })
        
        permissions.value.forEach(permission => {
          const selected = permissionsAction[permission.id]
          permission.selected = selected || []
          onChangeCheck(permission)
        })
      }
    }
    
    const onChangeCheck = (permission) => {
      permission.indeterminate = !!permission.selected.length && (permission.selected.length < permission.actionsOptions.length)
      permission.checkedAll = permission.selected.length === permission.actionsOptions.length
    }
    
    const onChangeCheckAll = (e, permission) => {
      Object.assign(permission, {
        selected: e.target.checked ? permission.actionsOptions.map(obj => obj.value) : [],
        indeterminate: false,
        checkedAll: e.target.checked
      })
    }
    
    onMounted(() => {
      getRoleList().then((res) => {
        roles.value = res.result.data
        roles.value.push({
          id: '-1',
          name: '新增角色',
          describe: '新增一个角色'
        })
      })
      loadPermissions()
    })
    
    return {
      formRef,
      formState,
      mdl,
      roles,
      permissions,
      isMobile,
      edit,
      onChangeCheck,
      onChangeCheckAll
    }
  }
}
</script>

<style scoped>

</style>