<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
<#list table.fields as field>
<#if field?index < 3>
          <a-col :md="8" :sm="24">
            <a-form-item label="${field.comment?default(field.name)}">
              <a-input v-model="queryParam.${field.propertyName}" placeholder="" />
            </a-form-item>
          </a-col>
</#if>
</#list>
          <a-col :md="8" :sm="24">
            <span class="table-page-search-submitButtons">
              <a-button type="primary" @click="$refs.table.refresh(true)">查询</a-button>
              <a-button style="margin-left: 8px" @click="() => queryParam = {}">重置</a-button>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <div class="table-operator">
      <a-button type="primary" icon="plus" @click="$refs.createModal.add()">新建</a-button>
      <a-dropdown v-if="selectedRowKeys.length > 0">
        <a-menu slot="overlay">
          <a-menu-item key="1" @click="$refs.createModal.batchDelete()"><a-icon type="delete" />删除</a-menu-item>
        </a-menu>
        <a-button style="margin-left: 8px">
          批量操作 <a-icon type="down" />
        </a-button>
      </a-dropdown>
    </div>

    <s-table
      ref="table"
      size="default"
      :columns="columns"
      :data="loadData"
      :alert="options.alert"
      :rowSelection="options.rowSelection"
      showPagination="auto"
    >
      <span slot="serial" slot-scope="text, record, index">
        {{ index + 1 }}
      </span>
<#list table.fields as field>
      <span slot="${field.propertyName}" slot-scope="text">
        {{ text }}
      </span>
</#list>
      <span slot="action" slot-scope="text, record">
        <template>
          <a @click="$refs.createModal.edit(record.id)">编辑</a>
          <a-divider type="vertical" />
          <a @click="handleDelete(record.id)">删除</a>
        </template>
      </span>
    </s-table>

    <create-form ref="createModal" @ok="handleOk" />
  </a-card>
</template>

<script>
import { STable } from '@/components'
import { getList, del${table.beanName?cap_first} } from '@/api/${table.beanName}'
import CreateForm from './modules/${table.beanName}Form'

const columns = [
  {
    title: '#',
    scopedSlots: { customRender: 'serial' }
  },
<#list table.fields as field>
  {
    title: '${field.comment?default(field.name)}',
    dataIndex: '${field.propertyName}',
    scopedSlots: { customRender: '${field.propertyName}' }
  },
</#list>
  {
    title: '操作',
    dataIndex: 'action',
    width: '150px',
    scopedSlots: { customRender: 'action' }
  }
]

export default {
  name: '${table.beanName?cap_first}List',
  components: {
    STable,
    CreateForm
  },
  data () {
    this.columns = columns
    return {
      visible: false,
      confirmLoading: false,
      // 创建表单
      form: this.$form.createForm(this),
      // 查询条件参数
      queryParam: {},
      // 加载数据方法 必须为 Promise 对象
      loadData: parameter => {
        return getList(Object.assign(parameter, this.queryParam))
          .then(res => {
            return res.data
          })
      },
      selectedRowKeys: [],
      selectedRows: [],
      options: {
        alert: { show: true, clear: () => { this.selectedRowKeys = [] } },
        rowSelection: { selectedRowKeys: this.selectedRowKeys, onChange: this.onSelectChange }
      }
    }
  },
  methods: {
    handleAdd () {
      this.visible = true
    },
    handleEdit (record) {
      this.visible = true
      this.$nextTick(() => {
        this.form.setFieldsValue(record)
      })
    },
    handleDelete (id) {
      this.$confirm({
        title: '确认删除',
        content: '确定要删除这条记录吗？',
        onOk: () => {
          del${table.beanName?cap_first}(id).then(res => {
            this.$message.info('删除成功')
            this.$refs.table.refresh()
          })
        }
      })
    },
    handleOk () {
      this.$refs.table.refresh()
    },
    onSelectChange (selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys
      this.selectedRows = selectedRows
    },
    toggleAdvanced () {
      this.advanced = !this.advanced
    },
    resetSearchForm () {
      this.queryParam = {}
      this.$refs.table.refresh(true)
    }
  }
}
</script>