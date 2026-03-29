<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
<#list table.fields as field>
<#if field?index < 3 && !field.sensitive>
          <a-col :md="8" :sm="24">
            <a-form-item label="${field.comment?default(field.name)}">
              <a-input v-model:value="queryParam.${field.propertyName}" placeholder="" />
            </a-form-item>
          </a-col>
</#if>
</#list>
          <a-col :md="8" :sm="24">
            <span class="table-page-search-submitButtons">
              <a-button type="primary" @click="tableRef.refresh(true)">查询</a-button>
              <a-button style="margin-left: 8px" @click="resetQueryParam">重置</a-button>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <div class="table-operator">
      <a-button type="primary" @click="handleAdd">
        <template #icon><plus-outlined /></template>
        新建
      </a-button>
      <a-dropdown v-if="selectedRowKeys.length > 0">
        <template #overlay>
          <a-menu>
            <a-menu-item key="1" @click="handleBatchDelete">
              <delete-outlined />
              删除
            </a-menu-item>
          </a-menu>
        </template>
        <a-button style="margin-left: 8px">
          批量操作 <down-outlined />
        </a-button>
      </a-dropdown>
    </div>

    <s-table
      ref="tableRef"
      size="default"
      :columns="columns"
      :data="loadData"
      :alert="options.alert"
      :rowSelection="options.rowSelection"
      showPagination="auto"
    >
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'serial'">
          {{ index + 1 }}
        </template>
<#list table.fields as field>
<#if !field.sensitive>
        <template v-else-if="column.dataIndex === '${field.propertyName}'">
          {{ text }}
        </template>
</#if>
</#list>
        <template v-else-if="column.dataIndex === 'action'">
          <a @click="handleEdit(record.id)">编辑</a>
          <a-divider type="vertical" />
          <a @click="handleDelete(record.id)">删除</a>
        </template>
      </template>
    </s-table>

    <create-form ref="createModalRef" @ok="handleOk" />
  </a-card>
</template>

<script>
import { ref, reactive } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, DownOutlined } from '@ant-design/icons-vue'
import { STable } from '@/components'
import { getList, del${table.beanName?cap_first} } from '@/api/${table.beanName}'
import CreateForm from './${table.beanName}Form.vue'

export default {
  name: '${table.beanName?cap_first}List',
  components: {
    STable,
    CreateForm,
    PlusOutlined,
    DeleteOutlined,
    DownOutlined
  },
  setup () {
    const tableRef = ref()
    const createModalRef = ref()
    const selectedRowKeys = ref([])
    const selectedRows = ref([])
    const queryParam = reactive({})

    const columns = [
      {
        title: '#',
        dataIndex: 'serial',
        width: 80,
        fixed: 'left'
      },
<#list table.fields as field>
<#if !field.sensitive>
      {
        title: '${field.comment?default(field.name)}',
        dataIndex: '${field.propertyName}'<#if table.getFieldWidthConfig(field) != "">,
        ${table.getFieldWidthConfig(field)}</#if><#if table.getFieldFixedConfig(field, field?index) != "">,
        ${table.getFieldFixedConfig(field, field?index)}</#if>
      },
</#if>
</#list>
      {
        title: '操作',
        dataIndex: 'action',
        width: '150px',
        fixed: 'right'
      }
    ]

    const loadData = (parameter) => {
      return getList(Object.assign(parameter, queryParam))
        .then(res => {
          return res.data
        })
    }

    const options = {
      alert: {
        show: true,
        clear: () => { selectedRowKeys.value = [] }
      },
      rowSelection: {
        selectedRowKeys: selectedRowKeys,
        onChange: (keys, rows) => {
          selectedRowKeys.value = keys
          selectedRows.value = rows
        }
      }
    }

    const handleAdd = () => {
      createModalRef.value.add()
    }

    const handleEdit = (id) => {
      createModalRef.value.edit(id)
    }

    const handleDelete = (id) => {
      Modal.confirm({
        title: '确认删除',
        content: '确定要删除这条记录吗？',
        onOk: () => {
          del${table.beanName?cap_first}(id).then(() => {
            message.info('删除成功')
            tableRef.value.refresh()
          })
        }
      })
    }

    const handleBatchDelete = () => {
      Modal.confirm({
        title: '确认删除',
        content: '确定要删除选中的记录吗？',
        onOk: () => {
          // 批量删除逻辑
          message.info('删除成功')
          selectedRowKeys.value = []
          tableRef.value.refresh()
        }
      })
    }

    const handleOk = () => {
      tableRef.value.refresh()
    }

    const resetQueryParam = () => {
      Object.keys(queryParam).forEach(key => {
        queryParam[key] = ''
      })
      tableRef.value.refresh(true)
    }

    return {
      tableRef,
      createModalRef,
      columns,
      queryParam,
      loadData,
      selectedRowKeys,
      selectedRows,
      options,
      handleAdd,
      handleEdit,
      handleDelete,
      handleBatchDelete,
      handleOk,
      resetQueryParam
    }
  }
}
</script>