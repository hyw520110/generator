import T from 'ant-design-vue/es/table/Table'
import get from 'lodash.get'
import { SettingOutlined } from '@ant-design/icons-vue'

export default {
  components: {
    SettingOutlined
  },
  data () {
    return {
      needTotalList: [],

      selectedRows: [],
      selectedRowKeys: [],

      localLoading: false,
      localDataSource: [],
      localPagination: Object.assign({}, this.pagination),
      
      // 列选择器相关
      columnSettingsVisible: false,
      visibleColumns: []
    }
  },
  props: Object.assign({}, T.props, {
    rowKey: {
      type: [String, Function],
      default: 'key'
    },
    data: {
      type: Function,
      required: true
    },
    pageNum: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 10
    },
    showSizeChanger: {
      type: Boolean,
      default: true
    },
    size: {
      type: String,
      default: 'default'
    },
    /**
     * alert: {
     *   show: true,
     *   clear: Function
     * }
     */
    alert: {
      type: [Object, Boolean],
      default: null
    },
    rowSelection: {
      type: Object,
      default: null
    },
    /** @Deprecated */
    showAlertInfo: {
      type: Boolean,
      default: false
    },
    showPagination: {
      type: [String, Boolean],
      default: 'auto'
    },
    /**
     * enable page URI mode
     *
     * e.g:
     * /users/1
     * /users/2
     * /users/3?queryParam=test
     * ...
     */
    pageURI: {
      type: Boolean,
      default: false
    },
    // 是否显示列选择器
    showColumnSettings: {
      type: Boolean,
      default: true
    },
    // 是否启用横向滚动
    scroll: {
      type: Object,
      default: () => ({ x: 'max-content' })
    }
  }),
  watch: {
    'localPagination.current' (val) {
      this.pageURI && this.$router.push({
        ...this.$route,
        name: this.$route.name,
        params: Object.assign({}, this.$route.params, {
          pageNo: val
        })
      })
    },
    pageNum (val) {
      Object.assign(this.localPagination, {
        current: val
      })
    },
    pageSize (val) {
      Object.assign(this.localPagination, {
        pageSize: val
      })
    },
    showSizeChanger (val) {
      Object.assign(this.localPagination, {
        showSizeChanger: val
      })
    }
  },
  created () {
    const { pageNo } = this.$route.params
    const localPageNum = this.pageURI && (pageNo && parseInt(pageNo)) || this.pageNum
    this.localPagination = ['auto', true].includes(this.showPagination) && Object.assign({}, this.localPagination, {
      current: localPageNum,
      pageSize: this.pageSize,
      showSizeChanger: this.showSizeChanger
    }) || false
    console.log('this.localPagination', this.localPagination)
    this.needTotalList = this.initTotalList(this.columns)
    // 初始化可见列
    this.initVisibleColumns()
    this.loadData()
  },
  methods: {
    /**
     * 初始化可见列
     */
    initVisibleColumns () {
      if (this.columns && this.columns.length > 0) {
        this.visibleColumns = this.columns.map(col => ({
          ...col,
          visible: col.visible !== false // 默认可见
        }))
      }
    },
    /**
     * 切换列的可见性
     */
    toggleColumnVisible (dataIndex) {
      const col = this.visibleColumns.find(c => c.dataIndex === dataIndex)
      if (col) {
        col.visible = !col.visible
      }
    },
    /**
     * 重置列显示
     */
    resetColumnSettings () {
      this.visibleColumns.forEach(col => {
        col.visible = true
      })
    },
    /**
     * 表格重新加载方法
     * 如果参数为 true, 则强制刷新到第一页
     * @param Boolean bool
     */
    refresh (bool = false) {
      bool && (this.localPagination = Object.assign({}, {
        current: 1, pageSize: this.pageSize
      }))
      this.loadData()
    },
    /**
     * 加载数据方法
     * @param {Object} pagination 分页选项器
     * @param {Object} filters 过滤条件
     * @param {Object} sorter 排序条件
     */
    loadData (pagination, filters, sorter) {
      this.localLoading = true
      const parameter = Object.assign({
        pageNum: (pagination && pagination.current) ||
          this.showPagination && this.localPagination.current || this.pageNum,
        pageSize: (pagination && pagination.pageSize) ||
          this.showPagination && this.localPagination.pageSize || this.pageSize
      },
      (sorter && sorter.field && {
        sortField: sorter.field
      }) || {},
      (sorter && sorter.order && {
        sortOrder: sorter.order
      }) || {}, {
        ...filters
      }
      )
      console.log('parameter', parameter)
      const result = this.data(parameter)
      // 对接自己的通用数据接口需要修改下方代码中的 r.pageNo, r.totalCount, r.data
      // eslint-disable-next-line
      if ((typeof result === 'object' || typeof result === 'function') && typeof result.then === 'function') {
        result.then(r => {
          this.localPagination = this.showPagination && Object.assign({}, this.localPagination, {
            current: r.current, // 返回结果中的当前分页数
            total: r.total, // 返回结果中的总记录数
            showSizeChanger: this.showSizeChanger,
            pageSize: (pagination && pagination.pageSize) ||
              this.localPagination.pageSize
          }) || false
          // 为防止删除数据后导致页面当前页面数据长度为 0 ,自动翻页到上一页
          if (r.records.length === 0 && this.showPagination && this.localPagination.current > 1) {
            this.localPagination.current--
            this.loadData()
            return
          }

          // 这里用于判断接口是否有返回 r.totalCount 且 this.showPagination = true 且 pageNo 和 pageSize 存在 且 totalCount 小于等于 pageNo * pageSize 的大小
          // 当情况满足时，表示数据不满足分页大小，关闭 table 分页功能
          try {
            if ((['auto', true].includes(this.showPagination) && r.total <= (r.current * this.localPagination.pageSize))) {
              this.localPagination.hideOnSinglePage = true
            }
          } catch (e) {
            this.localPagination = false
          }
          console.log('loadData -> this.localPagination', this.localPagination)
          this.localDataSource = r.records // 返回结果中的数组数据
          this.localLoading = false
        })
      }
    },
    initTotalList (columns) {
      const totalList = []
      columns && columns instanceof Array && columns.forEach(column => {
        if (column.needTotal) {
          totalList.push({
            ...column,
            total: 0
          })
        }
      })
      return totalList
    },
    /**
     * 用于更新已选中的列表数据 total 统计
     * @param selectedRowKeys
     * @param selectedRows
     */
    updateSelect (selectedRowKeys, selectedRows) {
      this.selectedRows = selectedRows
      this.selectedRowKeys = selectedRowKeys
      const list = this.needTotalList
      this.needTotalList = list.map(item => {
        return {
          ...item,
          total: selectedRows.reduce((sum, val) => {
            const total = sum + parseInt(get(val, item.dataIndex))
            return isNaN(total) ? 0 : total
          }, 0)
        }
      })
    },
    /**
     * 清空 table 已选中项
     */
    clearSelected () {
      if (this.rowSelection) {
        this.rowSelection.onChange([], [])
        this.updateSelect([], [])
      }
    },
    /**
     * 处理交给 table 使用者去处理 clear 事件时，内部选中统计同时调用
     * @param callback
     * @returns {*}
     */
    renderClear (callback) {
      if (this.selectedRowKeys.length <= 0) return null
      return (
        <a style="margin-left: 24px" onClick={() => {
          callback()
          this.clearSelected()
        }}>清空</a>
      )
    },
    renderAlert () {
      // 绘制统计列数据
      const needTotalItems = this.needTotalList.map((item) => {
        return (<span style="margin-right: 12px">
          {item.title}总计 <a style="font-weight: 600">{!item.customRender ? item.total : item.customRender(item.total)}</a>
        </span>)
      })

      // 绘制 清空 按钮
      const clearItem = (typeof this.alert.clear === 'boolean' && this.alert.clear) ? (
        this.renderClear(this.clearSelected)
      ) : (this.alert !== null && typeof this.alert.clear === 'function') ? (
        this.renderClear(this.alert.clear)
      ) : null

      // 绘制 alert 组件 - Vue 3 兼容写法
      const messageSlot = () => (
        <span>
          <span style="margin-right: 12px">已选择: <a style="font-weight: 600">{this.selectedRows.length}</a></span>
          {needTotalItems}
          {clearItem}
        </span>
      )

      return (
        <a-alert showIcon={true} style="margin-bottom: 16px" v-slots={{ message: messageSlot }} />
      )
    }
  },

  render () {
    const props = {}
    const localKeys = Object.keys(this.$data)
    const showAlert = (typeof this.alert === 'object' && this.alert !== null && this.alert.show) && typeof this.rowSelection.selectedRowKeys !== 'undefined' || this.alert

    Object.keys(T.props).forEach(k => {
      const localKey = `local${k.substring(0, 1).toUpperCase()}${k.substring(1)}`
      if (localKeys.includes(localKey)) {
        props[k] = this[localKey]
        return props[k]
      }
      if (k === 'rowSelection') {
        if (showAlert && this.rowSelection) {
          // 如果需要使用alert，则重新绑定 rowSelection 事件
          console.log('this.rowSelection', this.rowSelection)
          props[k] = {
            ...this.rowSelection,
            selectedRows: this.selectedRows,
            selectedRowKeys: this.selectedRowKeys,
            onChange: (selectedRowKeys, selectedRows) => {
              this.updateSelect(selectedRowKeys, selectedRows)
              typeof this[k].onChange !== 'undefined' && this[k].onChange(selectedRowKeys, selectedRows)
            }
          }
          return props[k]
        } else if (!this.rowSelection) {
          // 如果没打算开启 rowSelection 则清空默认的选择项
          props[k] = null
          return props[k]
        }
      }
      this[k] && (props[k] = this[k])
      return props[k]
    })
    
    // Vue 3 兼容的插槽处理
    const slots = {}
    for (const name in this.$slots) {
      slots[name] = this.$slots[name]
    }
    
    /**
     * 截取表头标题：冒号或逗号前的内容
     * @param {string} title - 原始标题
     * @returns {object} { title: 截断后的标题, fullTitle: 完整标题(如果有分隔符) }
     */
    const getShortTitle = (title) => {
      if (!title || typeof title !== 'string') return { title }
      // 英文冒号、中文冒号、中文逗号
      const colonIndex = title.indexOf(':')
      const cnColonIndex = title.indexOf('\uFF1A')
      const cnCommaIndex = title.indexOf('\uFF0C')
      // 取所有分隔符中最早出现的位置
      const validIndices = [colonIndex, cnColonIndex, cnCommaIndex].filter(i => i >= 0)
      if (validIndices.length === 0) return { title }
      const splitIndex = Math.min(...validIndices)
      if (splitIndex > 0) {
        return {
          title: title.substring(0, splitIndex).trim(),
          fullTitle: title
        }
      }
      return { title }
    }
    
    // 处理列配置
    const processedColumns = this.visibleColumns.filter(col => col.visible).map(col => {
      const columnConfig = { ...col }
      // 处理表头标题：检测冒号或逗号，截断显示
      if (col.title && typeof col.title === 'string') {
        const { title: shortTitle, fullTitle } = getShortTitle(col.title)
        if (fullTitle) {
          // 有分隔符，使用 tooltip 显示完整标题
          columnConfig.title = () => (
            <a-tooltip title={fullTitle} placement="top">
              {shortTitle}
            </a-tooltip>
          )
        }
      }
      // 添加文本省略和最大宽度
      if (!col.width && col.dataIndex !== 'serial' && col.dataIndex !== 'action') {
        columnConfig.ellipsis = {
          showTitle: false
        }
        columnConfig.customRender = ({ text }) => (
          <a-tooltip title={text} placement="top">
            {text}
          </a-tooltip>
        )
      }
      return columnConfig
    })
    
    // 添加横向滚动
    if (this.scroll && !props.scroll) {
      props.scroll = this.scroll
    }

    // 准备 v-slots，透传所有插槽
    const vSlots = {}
    Object.keys(this.$slots).forEach(key => {
      vSlots[key] = this.$slots[key]
    })

    const table = (
      <a-table {...props} columns={processedColumns} v-slots={vSlots} onChange={this.loadData} />
    )
    
    // 列选择器组件
    const columnSettingsMenu = (
      <a-menu>
        <a-menu-item-group title="列设置">
          {this.visibleColumns.map(col => (
            <a-menu-item key={col.dataIndex}>
              <a-checkbox 
                checked={col.visible} 
                onChange={() => this.toggleColumnVisible(col.dataIndex)}
              >
                {typeof col.title === 'function' ? col.title() : col.title}
              </a-checkbox>
            </a-menu-item>
          ))}
        </a-menu-item-group>
        <a-menu-divider />
        <a-menu-item onClick={this.resetColumnSettings}>
          重置
        </a-menu-item>
      </a-menu>
    )
    
    const columnSettings = this.showColumnSettings ? (
      <a-dropdown trigger={['click']} visible={this.columnSettingsVisible} onVisibleChange={(visible) => { this.columnSettingsVisible = visible }} overlay={columnSettingsMenu}>
        <a-button style="margin-left: 8px" size="small" icon={<setting-outlined />}>
          列设置
        </a-button>
      </a-dropdown>
    ) : null

    return (
      <div class="table-wrapper">
        { showAlert ? this.renderAlert() : null }
        <div style="margin-bottom: 8px; display: flex; justify-content: flex-end;">
          {columnSettings}
        </div>
        { table }
      </div>
    )
  }
}