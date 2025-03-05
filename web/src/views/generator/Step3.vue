<template>
  <div>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form layout="inline" :form="form" @submit="handQuery">
          <a-row :gutter="48">
            <a-col :md="5" :sm="24">
              <a-form-item label="数据库IP">
                <a-input v-decorator="['ipAndPort', { initialValue: 'localhost:3306', rules: [{required: true, message: '请输入数据库连接地址'}] }]" placeholder="数据库IP:端口"/>
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-item label="数据库">
                <a-input v-decorator="['dbName', { initialValue: 'test', rules: [{required: true, message: '请输入数据库名称'}] }]" placeholder="数据库"/>
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-item label="数据库用户名">
                <a-input v-decorator="['username', { initialValue: 'root', rules: [{required: true, message: '请输入数据库用户名'}] }]" placeholder="数据库用户名"/>
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-item label="数据库密码">
                <a-input v-decorator="['pwd', { initialValue: '123456', rules: [{required: true, message: '请输入数据库密码'}] }]" placeholder="数据库密码" type="password"/>
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-item label="表名称">
                <a-input v-decorator="['include', { rules: [{required: false, message: '请输入表名'}] }]" placeholder="需生成的表名或表名前缀,多个表逗号分隔"/>
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-item label="不包含">
                <a-input v-decorator="['exclude', { rules: [{required: false, message: '请输入排除的表名'}] }]" placeholder="排除的表名或表名前缀,多个表逗号分隔"/>
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-item label="移除表前缀">
                <a-input v-decorator="['tablePrefix', { rules: [{required: false, message: '请输入需移除的表前缀'}] }]" placeholder="生成代码时需移除的表前缀，如:batch_,qrtz_"/>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <span class="table-page-search-submitButtons">
                <a-button type="primary" html-type="submit">查询</a-button>
                <a-button style="margin-left: 8px" @click="resetSearchForm()">重置</a-button>
                <a-button style="margin-left: 8px" @click="prevStep">上一步</a-button>
              </span>
            </a-col>
          </a-row>
        </a-form>
      </div>
    </a-card>

    <a-card
      style="margin-top: 10px"
      :bordered="false">
      <div style="margin-bottom: 16px">
        <a-button
          type="primary"
          @click="start"
          :disabled="!hasSelected"
        >
          生成
        </a-button>
        <span style="margin-left: 8px">
          <template v-if="hasSelected">
            {{ `选择了 ${selectedRowKeys.length} 项` }}
          </template>
        </span>
      </div>
      <a-alert v-if="errorMessage" type="error" :message="errorMessage" style="margin-bottom: 16px" />
      <a-table
        ref="table"
        size="default"
        rowKey="name"
        :pagination="page"
        :columns="columns"
        :dataSource="data"
        :loading="loading"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
      >
        <span slot="action" slot-scope="text, record">
          <template>
            <a-popconfirm title="确认生成该表对应的代码?" @confirm="handleCreate(record)" okText="生成" cancelText="取消">
              <a>生成</a>
            </a-popconfirm>
          </template>
        </span>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import { STable, Ellipsis } from '@/components' // 删除: import { STable, Ellipsis, Result } from '@/components'
import { getTableList, genCode } from '@/api/generator'

export default {
  name: 'GenCodeView',
  components: {
    STable,
    Ellipsis
  },
  data () {
    return {
      mdl: {},
      // 查询参数
      queryParam: {},
      // 表头
      columns: [
        {
          title: '表名称',
          dataIndex: 'name'
        },
        {
          title: '表描述',
          dataIndex: 'comment'
        },
        {
          title: '创建时间',
          dataIndex: 'createTime'
        },
        {
          title: '操作',
          dataIndex: 'action',
          width: '150px',
          scopedSlots: { customRender: 'action' }
        }
      ],
      data: [],
      selectedRowKeys: [],
      selectedRows: [],
      loading: false,
      errorMessage: '',

      // custom table alert & rowSelection
      options: {
        alert: { show: false, clear: () => { this.selectedRowKeys = [] } },
        rowSelection: {
          selectedRowKeys: this.selectedRowKeys,
          onChange: this.onSelectChange
        }
      },

      page: {
        defaultPageSize: 20
      }
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  created () {
    this.loadData()
  },
  computed: {
    hasSelected () {
      return this.selectedRowKeys.length > 0
    }
  },
  methods: {
    start () {
        // 批量生成代码
        this.loading = true
        genCode({ 'tabName': this.selectedRowKeys.join(',') })
            .then(res => {
                this.loading = false
                this.selectedRowKeys = []
                if (res.status === 10000) {
                    this.errorMessage = '代码已生成 ' + res.message
                } else {
                    this.errorMessage = ''
                }
            })
            .catch(err => {
                this.loading = false
                this.errorMessage = err.response ? err.response.data.message : '生成代码时发生错误'
            })
    },
    handQuery (e) {
      e.preventDefault()
      this.form.validateFields((error, values) => {
        if (!error) {
          console.log('values', values)
          this.loadData(values)
        }
      })
    },
    // 加载数据方法 必须为 Promise 对象
    loadData (parameter) {
      console.log(parameter)
      this.loading = true
      getTableList(parameter).then(res => {
        console.log(res)
        this.loading = false
        const data = JSON.parse(res.data)
        this.data = data.tables
        const queryForm = data.dataSource
        this.form.setFieldsValue({ ipAndPort: queryForm.ipAndPort, dbName: queryForm.dbName, username: queryForm.username, pwd: queryForm.pwd })
      })
    },
    handleCreate (record) {
      console.log('re', record)
      genCode({ 'tabName': record.name }).then(res => {
        this.loading = false
      })
    },
    onSelectChange (selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys
      this.selectedRows = selectedRows
      console.log('selectedRowKeys', selectedRowKeys, 'selectedRows', selectedRows)
    },
    resetSearchForm () {
      this.queryParam = {}
      this.loadData()
    },
    prevStep () {
      this.$emit('prevStep')
    }
  }
}
</script>
