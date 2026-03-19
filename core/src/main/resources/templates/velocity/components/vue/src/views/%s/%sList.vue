<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
#set($count=1)
#foreach($field in ${table.fields})
#if(!${field.isPrimarykey()} && $count < 3)
#set($count = ${count}+1 )
          <a-col :md="8" :sm="24">
            <a-form-item label="#if("${field.comment}"=="")${field.name}#else${field.comment}#end">
              <a-input v-model="queryParam.${field.name}" placeholder=""/>
            </a-form-item>
          </a-col>
#end          
#end
          <template v-if="advanced">
#foreach($field in ${table.fields})
#if(!${field.isPrimarykey()} && $count > $velocityCount)
            <a-col :md="8" :sm="24">
              <a-form-item label="#if("${field.comment}"=="")${field.name}#else${field.comment}#end">
                <a-input-number v-model="queryParam.${field.name}" style="width: 100%"/>
              </a-form-item>
            </a-col>
#end
#end
          </template>

          <a-col :md="!advanced && 8 || 24" :sm="24">
            <span class="table-page-search-submitButtons" :style="advanced && { float: 'right', overflow: 'hidden' } || {} ">
              <a-button type="primary" @click="$refs.table.refresh(true)">查询</a-button>
              <a-button style="margin-left: 8px" @click="() => queryParam = {}">重置</a-button>
              <a @click="toggleAdvanced" style="margin-left: 8px">
                {{ advanced ? '收起' : '展开' }}
                <a-icon :type="advanced ? 'up' : 'down'"/>
              </a>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <div class="table-operator">
      <a-button type="primary" icon="plus" @click="$refs.createModal.add()" v-action:add>新建</a-button>
      <a-button type="dashed" @click="tableOption">{{ optionAlertShow && '关闭' || '开启' }} alert</a-button>
      <a-dropdown v-action:edit v-if="selectedRowKeys.length > 0">
        <a-menu slot="overlay">
          <a-menu-item key="1"><a-icon type="delete" />删除</a-menu-item>
          <a-menu-item key="2"><a-icon type="cancel" />取消</a-menu-item>
        </a-menu>
        <a-button style="margin-left: 8px">
          批量操作 <a-icon type="down" />
        </a-button>
      </a-dropdown>
    </div>

    <s-table
      ref="table"
      size="default"
      rowKey="#if(""=="${table.getPrimarykeyFieldsNames()}")id#else${table.getPrimarykeyFieldsNames()}#end"
      :columns="columns"
      :data="loadData"
      :alert="options.alert"
      :rowSelection="options.rowSelection"
    >
      <span slot="action" slot-scope="text, record">
        <template>
          <a @click="handleEdit(record)" v-action:edit>编辑</a>
          <a-divider type="vertical" />
          <a-popconfirm title="确认删除?" @confirm="handleDel(record)" okText="删除" cancelText="取消" v-action:del>
            <a>删除</a>
          </a-popconfirm>
        </template>
      </span>
    </s-table>
    <create-form ref="createModal" @add="handleAddOk" @edit="handleEditOk" />
  </a-card>
</template>

<script>
import { STable, Ellipsis } from '@/components'
import CreateForm from './${table.beanName}Form'
import { getList, add${table.beanName}, edit${table.beanName}, del${table.beanName} } from '@/api/${table.beanName}'

export default {
  name: '${table.beanName}List',
  components: {
    STable,
    Ellipsis,
    CreateForm
  },
  data () {
    return {
      mdl: {},
      // 高级搜索 展开/关闭
      advanced: false,
      // 查询参数
      queryParam: {},
      // 表头
      columns: [
#foreach($field in ${table.fields})
#if(!${field.isPrimarykey()}&&"${field.comment}"!="")
        {
          title: '${field.comment}',
          sorter: true,
          dataIndex: '${field.propertyName}'
        },
#end
#end
        {
          title: '操作',
          dataIndex: 'action',
          width: '150px',
          scopedSlots: { customRender: 'action' }
        }
      ],
      // 加载数据方法 必须为 Promise 对象
      loadData: parameter => {
        console.log('loadData.parameter', parameter)
        return getList(Object.assign(parameter, this.queryParam))
          .then(res => {
            return res.data
          })
      },
      selectedRowKeys: [],
      selectedRows: [],

      // custom table alert & rowSelection
      options: {
        alert: { show: true, clear: () => { this.selectedRowKeys = [] } },
        rowSelection: {
          selectedRowKeys: this.selectedRowKeys,
          onChange: this.onSelectChange
        }
      },
      optionAlertShow: false
    }
  },
  methods: {
    tableOption () {
      if (!this.optionAlertShow) {
        this.options = {
          alert: { show: true, clear: () => { this.selectedRowKeys = [] } },
          rowSelection: {
            selectedRowKeys: this.selectedRowKeys,
            onChange: this.onSelectChange
          }
        }
        this.optionAlertShow = true
      } else {
        this.options = {
          alert: false,
          rowSelection: null
        }
        this.optionAlertShow = false
      }
    },
    handleEdit (record) {
      console.log(record)
      this.$refs.createModal.edit(record)
    },
    handleEditOk (values) {
      edit${table.beanName}({ ...values }).then(res => {
        console.log(res)
        this.$message.info('修改成功')
        this.$refs.table.refresh()
      })
    },
    handleDel (record) {
      del${table.beanName}(#foreach($field in ${table.primarykeyFields})record.${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}) + ","#end#end).then(res => {
        this.$message.info('删除成功')
        this.$refs.table.refresh()
      })
    },
    handleAddOk (values) {
      add${table.beanName}(values).then(res => {
        this.$message.info('添加成功')
        this.$refs.table.refresh(true)
      })
    },
    onSelectChange (selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys
      this.selectedRows = selectedRows
    },
    toggleAdvanced () {
      this.advanced = !this.advanced
    },
    resetSearchForm () {
      this.queryParam = {
      // import moment from 'moment'
      // date: moment(new Date())
      }
      this.$refs.table.refresh(true)
    }
  }
}
</script>
