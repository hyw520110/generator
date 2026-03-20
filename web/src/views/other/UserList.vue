<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
          <a-col :md="8" :sm="24">
            <a-form-item label="角色ID">
              <a-input placeholder="请输入"/>
            </a-form-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-item label="状态">
              <a-select placeholder="请选择" default-value="0">
                <a-select-option value="0">全部</a-select-option>
                <a-select-option value="1">关闭</a-select-option>
                <a-select-option value="2">运行中</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <span class="table-page-search-submitButtons">
              <a-button type="primary">查询</a-button>
              <a-button style="margin-left: 8px">重置</a-button>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <s-table
      row-key="id"
      size="default"
      :columns="columns"
      :data="loadData"
      :expandedRowKeys="expandedRowKeys"
      @expand="handleExpand"
    >
      <template #expandedRowRender="{ record }">
        <div style="margin: 0">
          <a-row :gutter="24" :style="{ marginBottom: '12px' }">
            <a-col :span="12" v-for="(role, index) in record.permissions" :key="index" :style="{ marginBottom: '12px', height: '23px' }">
              <a-col :lg="4" :md="24">
                <span>{{ role.permissionName }}：</span>
              </a-col>
              <a-col :lg="20" :md="24" v-if="role.actionList && role.actionList.length > 0">
                <a-tag color="cyan" v-for="action in role.actionList" :key="action">{{ permissionFilter(action) }}</a-tag>
              </a-col>
              <a-col :span="20" v-else>-</a-col>
            </a-col>
          </a-row>
        </div>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag color="blue">{{ statusFilter(text) }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ text }}
        </template>
        <template v-if="column.dataIndex === 'action'">
          <a @click="handleEdit(record)">编辑</a>
          <a-divider type="vertical"/>
          <a-dropdown>
            <a class="ant-dropdown-link">
              更多 <DownOutlined/>
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item>
                  <a href="javascript:;">详情</a>
                </a-menu-item>
                <a-menu-item>
                  <a href="javascript:;">禁用</a>
                </a-menu-item>
                <a-menu-item>
                  <a href="javascript:;">删除</a>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
      </template>
    </s-table>

    <a-modal
      title="操作"
      style="top: 20px;"
      :width="800"
      v-model:open="visible"
      @ok="handleOk"
    >
      <a-form class="permission-form" :model="formState" ref="formRef">

        <a-form-item
          :labelCol="labelCol"
          :wrapperCol="wrapperCol"
          label="唯一识别码"
          hasFeedback
          validateStatus="success"
          name="id"
        >
          <a-input
            placeholder="唯一识别码"
            disabled
            v-model:value="formState.id"
          />
        </a-form-item>

        <a-form-item
          :labelCol="labelCol"
          :wrapperCol="wrapperCol"
          label="角色名称"
          hasFeedback
          validateStatus="success"
          name="name"
        >
          <a-input
            placeholder="起一个名字"
            v-model:value="formState.name"
          />
        </a-form-item>

        <a-form-item
          :labelCol="labelCol"
          :wrapperCol="wrapperCol"
          label="状态"
          hasFeedback
          validateStatus="warning"
          name="status"
        >
          <a-select v-model:value="formState.status">
            <a-select-option :value="1">正常</a-select-option>
            <a-select-option :value="2">禁用</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          :labelCol="labelCol"
          :wrapperCol="wrapperCol"
          label="描述"
          hasFeedback
          name="describe"
        >
          <a-textarea
            :rows="5"
            placeholder="..."
            v-model:value="formState.describe"
          />
        </a-form-item>

        <a-divider>拥有权限</a-divider>
        <template v-for="permission in permissions" :key="permission.permissionId">
          <a-form-item
            class="permission-group"
            v-if="permission.actionsOptions && permission.actionsOptions.length > 0"
            :labelCol="labelCol"
            :wrapperCol="wrapperCol"
            :label="permission.permissionName"
          >
            <a-checkbox>全选</a-checkbox>
            <a-checkbox-group v-model:value="permission.selected" :options="permission.actionsOptions"/>
          </a-form-item>
        </template>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { DownOutlined } from '@ant-design/icons-vue'
import { STable } from '@/components'
import { getRoleList, getServiceList } from '@/api/manage'
import { PERMISSION_ENUM } from '@/utils/helper/permission'

const STATUS = {
  1: '启用',
  2: '禁用'
}

const columns = [
  { title: '唯一识别码', dataIndex: 'id' },
  { title: '角色名称', dataIndex: 'name' },
  { title: '状态', dataIndex: 'status', scopedSlots: { customRender: 'status' } },
  { title: '创建时间', dataIndex: 'createTime', scopedSlots: { customRender: 'createTime' }, sorter: true },
  { title: '操作', width: '150px', dataIndex: 'action', scopedSlots: { customRender: 'action' } }
]

export default {
  name: 'TableList',
  components: {
    STable,
    DownOutlined
  },
  setup () {
    const formRef = ref()
    const visible = ref(false)
    const permissions = ref([])
    const expandedRowKeys = ref([])
    
    const labelCol = { xs: { span: 24 }, sm: { span: 5 } }
    const wrapperCol = { xs: { span: 24 }, sm: { span: 16 } }
    
    const formState = reactive({
      id: '',
      name: '',
      status: 1,
      describe: '',
      permissionsObj: {}
    })
    
    const statusFilter = (key) => STATUS[key] || key
    const permissionFilter = (key) => {
      const permission = PERMISSION_ENUM[key]
      return permission && permission.label
    }
    
    const loadData = (parameter) => {
      return getRoleList(parameter)
        .then(res => {
          console.log('getRoleList', res)
          expandedRowKeys.value = res.result.data.map(item => item.id)
          return res.result
        })
    }
    
    const handleEdit = (record) => {
      visible.value = true
      console.log('record', record)
      
      permissions.value = record.permissions.map(permission => {
        const actionsOptions = permission.actionEntitySet.map(action => {
          return {
            label: action.describe,
            value: action.action,
            defaultCheck: action.defaultCheck
          }
        })
        return {
          ...permission,
          actionsOptions,
          selected: permission.actionList || []
        }
      })
      
      nextTick(() => {
        formState.id = record.id
        formState.name = record.name
        formState.status = record.status
        formState.describe = record.describe
      })
    }
    
    const handleOk = async (e) => {
      try {
        await formRef.value.validate()
        console.log('form values', formState)
      } catch (err) {
        // validation failed
      }
    }
    
    const handleExpand = (expanded, record) => {
      console.log('expanded', expanded, record)
      if (expanded) {
        expandedRowKeys.value.push(record.id)
      } else {
        expandedRowKeys.value = expandedRowKeys.value.filter(item => record.id !== item)
      }
    }
    
    onMounted(() => {
      getServiceList().then(res => {
        console.log('getServiceList.call()', res)
      })
      
      getRoleList().then(res => {
        console.log('getRoleList.call()', res)
      })
    })
    
    return {
      formRef,
      formState,
      visible,
      labelCol,
      wrapperCol,
      permissions,
      expandedRowKeys,
      columns,
      statusFilter,
      permissionFilter,
      loadData,
      handleEdit,
      handleOk,
      handleExpand
    }
  }
}
</script>

<style lang="less" scoped>
.permission-form {
  /deep/ .permission-group {
    margin-top: 0;
    margin-bottom: 0;
  }
}
</style>