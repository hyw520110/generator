<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
#set($count=1)
#foreach($field in $table.fields)
#if(!$field.sensitive && $count < 4)
#set($count = $count + 1)
          <a-col :md="8" :sm="24">
            <a-form-item label="#if("${field.comment}"=="")${field.name}#else${field.comment}#end">
              <a-input v-model:value="queryParam.${field.propertyName}" placeholder="" />
            </a-form-item>
          </a-col>
#end
#end
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
      <a-dropdown v-if="hasPrimaryKey && selectedRowKeys.length > 0">
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
      #if($EXCEL)
      <a-button style="margin-left: 8px" @click="handleExport">
        <template #icon><download-outlined /></template>
        导出 Excel
      </a-button>
      #end
    </div>

    <s-table
      ref="tableRef"
      size="default"
      :columns="columns"
      :data="loadData"
      :alert="options.alert"
      :rowSelection="options.rowSelection"
      :rowKey="getRecordKey"
      showPagination="auto"
    >
      <template #headerCell="{ column }">
        <a-tooltip v-if="column.fullTitle" :title="column.fullTitle" placement="top">
          <span>{{ column.title }}</span>
        </a-tooltip>
        <span v-else>{{ column.title }}</span>
      </template>
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'serial'">
          {{ index + 1 }}
        </template>
#foreach($field in $table.fields)
#if(!$field.sensitive)
        <template v-else-if="column.dataIndex === '${field.propertyName}'">
          {{ text }}
        </template>
#end
#end
        <template v-else-if="column.dataIndex === 'action'">
          <template v-if="hasPrimaryKey">
            <a @click="handleEdit(record)">编辑</a>
            <a-divider type="vertical" />
            <a @click="handleDelete(record)">删除</a>
            #if($WORKFLOW)
            <a-divider type="vertical" />
            <a @click="handleStartWorkflow(record)">发起审批</a>
            #end
          </template>
          <span v-else class="text-muted">仅新增</span>
        </template>
      </template>
    </s-table>

    <create-form ref="createModalRef" @ok="handleOk" />
  </a-card>
</template>

<script>
import { ref, reactive } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, DownOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { STable } from '@/components'
import { getList, del${table.beanName}, batchDel${table.beanName} #if($EXCEL), export${table.beanName}#end } from '@/api/${table.beanName}'
import CreateForm from './${table.beanName}Form.vue'

export default {
  name: '${table.beanName}List',
  components: {
    STable,
    CreateForm,
    PlusOutlined,
    DeleteOutlined,
    DownOutlined,
    DownloadOutlined
  },
  setup () {
    const tableRef = ref()
    const createModalRef = ref()
    const selectedRowKeys = ref([])
    const selectedRows = ref([])
    const queryParam = reactive({})
    const primaryKeyFields = ${table.primaryKeyJsArray}
    const hasPrimaryKey = primaryKeyFields.length > 0

    const getHeaderTitle = (comment) => {
      if (!comment) return ''
      const colonIndex = comment.indexOf(':')
      const cnColonIndex = comment.indexOf('\uFF1A')
      const cnCommaIndex = comment.indexOf('\uFF0C')
      const validIndices = [colonIndex, cnColonIndex, cnCommaIndex].filter(i => i >= 0)
      if (validIndices.length === 0) return comment
      const splitIndex = Math.min(...validIndices)
      return splitIndex > 0 ? comment.substring(0, splitIndex).trim() : comment
    }

    const createColumn = (comment, dataIndex, options = {}) => {
      const title = getHeaderTitle(comment)
      const fullTitle = title !== comment ? comment : null
      return {
        title,
        dataIndex,
        ellipsis: true,
        ...(fullTitle && { fullTitle }),
        ...options
      }
    }

    const columns = [
      {
        title: '#',
        dataIndex: 'serial',
        width: 80,
        fixed: 'left'
      },
#foreach($field in $table.fields)
#if(!$field.sensitive)
      createColumn('#if("${field.comment}"=="")${field.name}#else${field.comment}#end', '${field.propertyName}'#if($table.getFieldWidthConfig($field) != "" || $table.getFieldFixedConfig($field, $foreach.index) != ""), { #if($table.getFieldWidthConfig($field) != "")${table.getFieldWidthConfig($field)}#end#if($table.getFieldWidthConfig($field) != "" && $table.getFieldFixedConfig($field, $foreach.index) != ""), #end#if($table.getFieldFixedConfig($field, $foreach.index) != "")${table.getFieldFixedConfig($field, $foreach.index)}#end }#end),
#end
#end
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

    const getRecordKey = (record) => {
      if (!hasPrimaryKey) {
        return Object.keys(record || {}).map(key => record[key]).join(':')
      }
      return primaryKeyFields.map(key => record[key]).join(':')
    }

    const options = {
      alert: {
        show: true,
        clear: () => { selectedRowKeys.value = [] }
      },
      rowSelection: hasPrimaryKey ? {
        selectedRowKeys: selectedRowKeys,
        onChange: (keys, rows) => {
          selectedRowKeys.value = keys
          selectedRows.value = rows
        }
      } : null
    }

    const handleAdd = () => {
      createModalRef.value.add()
    }

    const handleEdit = (record) => {
      if (!hasPrimaryKey) {
        message.warning('当前表未定义主键，无法编辑记录')
        return
      }
      createModalRef.value.edit(record)
    }

    const handleDelete = (record) => {
      if (!hasPrimaryKey) {
        message.warning('当前表未定义主键，无法删除记录')
        return
      }
      Modal.confirm({
        title: '确认删除',
        content: '确定要删除这条记录吗？',
        onOk: async () => {
          await del${table.beanName}(record)
          message.info('删除成功')
          tableRef.value.refresh()
        }
      })
    }

    const handleBatchDelete = () => {
      if (!hasPrimaryKey) {
        message.warning('当前表未定义主键，无法批量删除记录')
        return
      }
      Modal.confirm({
        title: '确认删除',
        content: '确定要删除选中的记录吗？',
        onOk: async () => {
          await batchDel${table.beanName}(selectedRows.value)
          message.info('删除成功')
          selectedRowKeys.value = []
          tableRef.value.refresh()
        }
      })
    }

    const handleOk = () => {
      tableRef.value.refresh()
    }

    
    #if($EXCEL)
    const handleExport = () => {
      message.info('正在导出 Excel 报表，请稍候...')
      export${table.beanName}(queryParam).then(blob => {
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'export_data.xlsx')
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
      })
    }
    #end

    #if($WORKFLOW)
    const handleStartWorkflow = (record) => {
      Modal.confirm({
        title: '发起审批',
        content: '确定要为这条记录发起业务审批流程吗？',
        onOk: () => {
          message.success('审批流程发起成功！')
        }
      })
    }
    #end

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
      getRecordKey,
      selectedRowKeys,
      selectedRows,
      hasPrimaryKey,
      options,
      handleAdd,
      handleEdit,
      handleDelete,
      handleBatchDelete,
      handleOk,
      resetQueryParam,
      #if($EXCEL)
      handleExport,
      #end
      #if($WORKFLOW)
      handleStartWorkflow,
      #end
    }
  }
}
</script>

<style lang="less" scoped>
.table-page-search-wrapper {
  .ant-form-inline {
    .ant-form-item {
      display: flex;
      margin-bottom: 24px;
      margin-right: 0;

      .ant-form-item-control-wrapper {
        flex: 1;
        display: inline-block;
        vertical-align: middle;
      }

      > .ant-form-item-label {
        line-height: 32px;
        padding-right: 8px;
        width: auto;
      }

      .ant-form-item-control {
        line-height: 32px;
        display: inline-block;
        vertical-align: middle;
        flex: 1;
      }
    }
  }
}

.table-operator {
  margin-bottom: 18px;
}

.table-page-search-submitButtons {
  display: block;
  margin-bottom: 24px;
  white-space: nowrap;
}

:deep(.ant-table-thead > tr > th) {
  cursor: pointer;
}

:deep(.ant-table-tbody > tr > td) {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
