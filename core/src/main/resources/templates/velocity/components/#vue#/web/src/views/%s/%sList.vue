<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
#set($count=1)
#foreach($field in ${table.fields})
#if(!${field.isPrimarykey()} && !$field.sensitive && $count < 3)
#set($count = ${count}+1 )
          <a-col :md="8" :sm="24">
            <a-form-item label="#if("${field.comment}"=="")${field.name}#else${field.comment}#end">
              <a-input v-model:value="queryParam.${field.propertyName}" placeholder=""/>
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
      <!-- 自定义表头：实现冒号前文本显示，tooltip 显示完整文本 -->
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
#foreach($field in ${table.fields})
#if(!$field.sensitive)
        <template v-else-if="column.dataIndex === '${field.propertyName}'">
          {{ text }}
        </template>
#end
#end
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
import { getList, del${table.beanName} } from '@/api/${table.beanName}'
import CreateForm from './${table.beanName}Form.vue'

export default {
  name: '${table.beanName}List',
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

    /**
     * 截取表头标题：冒号或逗号前的内容
     * @param {string} comment - 字段注释
     * @returns {string} 表头标题
     */
    const getHeaderTitle = (comment) => {
      if (!comment) return ''
      // 英文冒号、中文冒号、中文逗号
      const colonIndex = comment.indexOf(':')
      const cnColonIndex = comment.indexOf('\uFF1A')
      const cnCommaIndex = comment.indexOf('\uFF0C')
      // 取所有分隔符中最早出现的位置
      const validIndices = [colonIndex, cnColonIndex, cnCommaIndex].filter(i => i >= 0)
      if (validIndices.length === 0) return comment
      const splitIndex = Math.min(...validIndices)
      return splitIndex > 0 ? comment.substring(0, splitIndex).trim() : comment
    }

    /**
     * 创建列配置
     * @param {string} comment - 字段注释（可能包含冒号）
     * @param {string} dataIndex - 数据索引
     * @param {object} options - 其他选项
     * @returns {object} 列配置对象
     */
    const createColumn = (comment, dataIndex, options = {}) => {
      const title = getHeaderTitle(comment)
      // 如果截取后的标题与原文不同，说明有冒号，需要 tooltip
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
#foreach($field in ${table.fields})
#if(!$field.sensitive)
      createColumn('#if("${field.comment}"=="")${field.name}#else${field.comment}#end', '${field.propertyName}'#if(${table.getFieldWidthConfig($field)} != "" || ${table.getFieldFixedConfig($field, $foreach.count)} != ""), { #if(${table.getFieldWidthConfig($field)} != "")${table.getFieldWidthConfig($field)}#end#if(${table.getFieldWidthConfig($field)} != "" && ${table.getFieldFixedConfig($field, $foreach.count)} != ""), #end#if(${table.getFieldFixedConfig($field, $foreach.count)} != "")${table.getFieldFixedConfig($field, $foreach.count)}#end }#end),
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
          del${table.beanName}(id).then(() => {
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

/* 表头样式：带 tooltip 的表头显示指针样式 */
:deep(.ant-table-thead > tr > th) {
  cursor: pointer;
}

/* 表格内容长文本处理：最大宽度 + 省略号 */
:deep(.ant-table-tbody > tr > td) {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>