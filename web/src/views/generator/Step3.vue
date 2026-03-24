<template>
  <div style="position: relative;">
    <!-- 下载中心按钮（页面右上角） -->
    <div class="download-center-btn">
      <a-button @click="showDownloadDrawer" :loading="downloadsLoading">
        <template #icon><download-outlined /></template>
        下载中心
        <a-badge :count="downloadCount" :overflow-count="99" v-if="downloadCount > 0" />
      </a-button>
    </div>

    <!-- 表关系图弹窗 -->
    <a-modal
      v-model:open="relationGraphVisible"
      title="表关系图"
      :width="1200"
      :footer="null"
      :bodyStyle="{ padding: '0', height: '600px' }"
      destroyOnClose
    >
      <RelationGraph ref="relationGraphRef" :graphData="graphData" :loading="graphLoading" />
    </a-modal>

    <!-- 下载中心抽屉 -->
    <a-drawer
      v-model:open="downloadDrawerVisible"
      title="下载中心"
      placement="right"
      :width="400"
    >
      <!-- 连接验证错误提示 -->
      <a-alert v-if="downloadError" type="error" :message="downloadError" show-icon style="margin-bottom: 16px;" />
      
      <a-list
        :loading="downloadsLoading"
        :data-source="downloadList"
        item-layout="horizontal"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :description="'大小: ' + item.size + ' | ' + item.time">
              <template #title>
                <span class="download-item-name">{{ item.name }}</span>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a @click="downloadFile(item.path)">下载</a>
              <a-popconfirm
                title="确定删除此文件？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDeleteFile(item.path)"
              >
                <a style="color: #ff4d4f;">删除</a>
              </a-popconfirm>
            </template>
          </a-list-item>
        </template>
        <template #empty>
          <a-empty description="暂无下载文件" />
        </template>
      </a-list>
    </a-drawer>

    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form layout="inline" :model="formState" @submit="handQuery" ref="formRef">
          <a-row :gutter="48">
            <a-col :md="5" :sm="24">
              <a-form-item label="数据库IP" name="ipAndPort">
                <a-input v-model:value="formState.ipAndPort" placeholder="数据库IP:端口" @change="onDbConfigChange"/>
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24" class="db-col">
              <a-form-item label="数据库" name="dbName">
                <a-auto-complete
                  v-model:value="formState.dbName"
                  :options="dbOptions"
                  placeholder="请选择或输入数据库名"
                  allow-clear
                  class="db-auto-complete"
                />
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-item label="数据库用户名" name="username">
                <a-input v-model:value="formState.username" placeholder="数据库用户名" @change="onDbConfigChange"/>
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-item label="数据库密码" name="pwd">
                <a-input v-model:value="formState.pwd" placeholder="数据库密码" type="password" @change="onDbConfigChange"/>
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-item label="表名称" name="include">
                <a-input v-model:value="formState.include" placeholder="需生成的表名或表名前缀,多个表逗号分隔"/>
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-item label="不包含" name="exclude">
                <a-input v-model:value="formState.exclude" placeholder="排除的表名或表名前缀,多个表逗号分隔"/>
              </a-form-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-item label="移除表前缀" name="tablePrefix">
                <a-input v-model:value="formState.tablePrefix" placeholder="生成代码时需移除的表前缀，如:batch_,qrtz_"/>
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

    <a-card style="margin-top: 5px" :bordered="false">
      <div style="margin-bottom: 16px; display: flex; align-items: center;">
        <a-dropdown :disabled="!hasSelected || generating">
          <a-button type="primary">
            生成代码
            <down-outlined />
          </a-button>
          <template #overlay>
            <a-menu @click="handleGenCode">
              <a-menu-item key="unpacked">
                生成代码
              </a-menu-item>
              <a-menu-item key="packed">
                打包下载
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-dropdown :disabled="!hasSelected || generating" style="margin-left: 8px;">
          <a-button>
            生成文档
            <down-outlined />
          </a-button>
          <template #overlay>
            <a-menu @click="handleGenDoc">
              <a-menu-item key="word">
                <file-word-outlined />
                生成 Word 文档
              </a-menu-item>
              <a-menu-item key="pdf">
                <file-pdf-outlined />
                生成 PDF 文档
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-dropdown style="margin-left: 8px;">
          <a-button>
            <apartment-outlined />
            关系图
            <down-outlined />
          </a-button>
          <template #overlay>
            <a-menu @click="handleShowRelationGraph">
              <a-menu-item key="selected" :disabled="!hasSelected">
                选中表关系图
              </a-menu-item>
              <a-menu-item key="all">
                所有表关系图
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <span style="margin-left: 8px">
          <template v-if="hasSelected">
            {{ `选择了 ${selectedRowKeys.length} 项` }}
          </template>
        </span>
        <!-- 生成中的计时器 -->
        <template v-if="generating">
          <a-tag color="processing" style="margin-left: 16px;">
            <template #icon><sync-outlined spin /></template>
            生成中... {{ formatDuration(elapsedTime) }}
          </a-tag>
        </template>
      </div>

      <!-- 生成结果提示 -->
      <template v-if="genResult">
        <a-alert type="success" show-icon style="margin-bottom: 16px;">
          <template #message>
            <div>
              <strong>{{ genResult.tableCount }} 个表</strong> 的代码已生成
              <template v-if="genResult.packed">
                ，耗时 {{ formatDuration(genResult.duration) }}
                <a-button type="link" @click="downloadCode" style="padding: 0 4px;">下载代码</a-button>
              </template>
              <template v-else>
                到 <strong>{{ genResult.outputDir }}</strong>，耗时 {{ formatDuration(genResult.duration) }}
              </template>
            </div>
          </template>
        </a-alert>
      </template>

      <!-- 错误提示 -->
      <template v-if="errorMessage && !genResult">
        <div style="margin-bottom: 16px;">
          <a-alert :type="errorType" :message="errorMessage" show-icon style="width: 100%;" />
        </div>
      </template>

      <!-- 关系图视图 -->
      <template v-if="showGraphView">
        <div v-if="graphLoading" class="graph-loading-wrapper">
          <a-spin size="large" tip="加载中..." />
        </div>
        <div v-else-if="graphData.nodes.length > 0" class="graph-view-container">
          <RelationGraph :graphData="graphData" @node-click="handleGraphNodeClick" @switch-view="hideGraphView" />
        </div>
        <a-empty v-else description="暂无表关系数据" style="padding: 100px 0;" />
      </template>

      <!-- 表格视图 -->
      <a-table
        v-else
        ref="table"
        size="default"
        rowKey="name"
        :pagination="pagination"
        :columns="columns"
        :dataSource="data"
        :loading="loading"
        :rowSelection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'action'">
            <a-popconfirm title="确认生成该表对应的代码?" @confirm="handleCreate(record)" okText="生成" cancelText="取消">
              <a>生成</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { getTableList, getDatabases, genCode, getDownloads, genDoc, getTableRelations, getConfig, deleteFile } from '@/api/generator'
import { SyncOutlined, DownloadOutlined, FileWordOutlined, FilePdfOutlined, DownOutlined, ApartmentOutlined } from '@ant-design/icons-vue'
import RelationGraph from '@/components/RelationGraph/index.vue'

export default {
  name: 'GenCodeView',
  components: { SyncOutlined, DownloadOutlined, FileWordOutlined, FilePdfOutlined, DownOutlined, ApartmentOutlined, RelationGraph },
  emits: ['prevStep'],
  setup (props, { emit }) {
    const formRef = ref()
    const table = ref()
    const loading = ref(false)
    const errorMessage = ref('')
    const errorType = ref('error')
    const data = ref([])
    const selectedRowKeys = ref([])
    const selectedRows = ref([])
    const dbOptions = ref([])
    const dbConnected = ref(false)
    const generating = ref(false)
    const elapsedTime = ref(0)
    const genResult = ref(null)

    // 生成文档相关
    const docGenerating = ref(false)
    const docElapsedTime = ref(0)
    const docResult = ref(null)
    
    // 下载中心相关
    const downloadDrawerVisible = ref(false)
    const downloadList = ref([])
    const downloadsLoading = ref(false)
    
    // 表关系图相关
    const showGraphView = ref(false)
    const graphLoading = ref(false)
    const graphData = ref({ nodes: [], edges: [] })
    
    let timer = null
    
    const formState = reactive({
      ipAndPort: '',
      dbName: '',
      username: '',
      pwd: '',
      include: '',
      exclude: '',
      tablePrefix: ''
    })
    
    const columns = [
      { title: '表名称', dataIndex: 'name' },
      { title: '表描述', dataIndex: 'comment' },
      { title: '创建时间', dataIndex: 'createTime' },
      { title: '操作', dataIndex: 'action', width: '150px' }
    ]
    
    const pagination = {
      defaultPageSize: 50,
      showSizeChanger: true,
      showQuickJumper: true,
      pageSizeOptions: ['20', '50', '100', '200'],
      showTotal: (total) => `共 ${total} 条`
    }
    
    const hasSelected = computed(() => selectedRowKeys.value.length > 0)

    // 格式化耗时
    const formatDuration = (ms) => {
      if (ms < 1000) return `${ms}ms`
      if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
      const min = Math.floor(ms / 60000)
      const sec = Math.round((ms % 60000) / 1000)
      return `${min}m ${sec}s`
    }

    // 开始计时
    const startTimer = () => {
      elapsedTime.value = 0
      timer = setInterval(() => {
        elapsedTime.value += 100
      }, 100)
    }

    // 停止计时
    const stopTimer = () => {
      if (timer) {
        clearInterval(timer)
        timer = null
      }
    }

    // 下拉框搜索过滤
    const filterOption = (input, option) => {
      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
    }

    // 加载数据库列表
    const loadDatabases = async () => {
      if (!formState.ipAndPort || !formState.username) {
        return
      }
      try {
        const res = await getDatabases({
          ipAndPort: formState.ipAndPort,
          username: formState.username,
          pwd: formState.pwd
        })
        if (res.status === 10000 && res.data) {
          dbOptions.value = res.data.map(db => ({ label: db, value: db }))
          dbConnected.value = true
          // 连接成功，设置默认数据库（如果列表中包含 test）
          if (!formState.dbName && res.data.includes('test')) {
            formState.dbName = 'test'
          }
          errorMessage.value = ''
        } else {
          dbConnected.value = false
          // 连接失败，清空默认数据库名
          formState.dbName = ''
          errorMessage.value = res.message || '数据库连接失败'
          errorType.value = 'warning'
        }
      } catch (err) {
        dbConnected.value = false
        // 连接失败，清空默认数据库名
        formState.dbName = ''
        errorMessage.value = '数据库连接失败: ' + (err.message || '请检查连接参数')
        errorType.value = 'warning'
      }
    }

    // 数据库配置变更时重新加载
    let dbLoadTimer = null
    const onDbConfigChange = () => {
      if (dbLoadTimer) {
        clearTimeout(dbLoadTimer)
      }
      // 防抖，500ms 后重新加载
      dbLoadTimer = setTimeout(() => {
        loadDatabases()
      }, 500)
    }
    
    const loadData = async (parameter) => {
      console.log(parameter)
      loading.value = true
      errorMessage.value = ''
      errorType.value = 'error'
      genResult.value = null
      try {
        const res = await getTableList(parameter || formState)
        console.log(res)
        loading.value = false
        const resData = JSON.parse(res.data)
        data.value = resData.tables
        const queryForm = resData.dataSource
        formState.ipAndPort = queryForm.ipAndPort
        formState.dbName = queryForm.dbName
        formState.username = queryForm.username
        formState.pwd = queryForm.pwd
      } catch (error) {
        loading.value = false
        errorMessage.value = error.message || '查询表列表失败'
      }
    }
    
    // 生成代码处理函数
    const handleGenCode = async ({ key }) => {
      const pack = key === 'packed'
      generating.value = true
      genResult.value = null
      errorMessage.value = ''
      startTimer()
      
      try {
        const res = await genCode({ tabName: selectedRowKeys.value.join(','), pack })
        stopTimer()
        generating.value = false
        selectedRowKeys.value = []
        
        if (res.status === 10000 && res.data) {
          genResult.value = res.data
          // 打包模式下刷新下载列表
          if (res.data.packed) {
            loadDownloads()
          }
        } else {
          errorMessage.value = res.message || '生成失败'
          errorType.value = 'error'
        }
      } catch (err) {
        stopTimer()
        generating.value = false
        errorMessage.value = err.response ? err.response.data.message : '生成代码时发生错误'
        errorType.value = 'error'
      }
    }

    // 生成文档处理函数
    const handleGenDoc = async ({ key }) => {
      docGenerating.value = true
      docResult.value = null
      errorMessage.value = ''
      // 启动计时
      docElapsedTime.value = 0
      const docTimer = setInterval(() => {
        docElapsedTime.value += 100
      }, 100)
      
      try {
        const res = await genDoc({
          tabName: selectedRowKeys.value.join(','),
          format: key // 'word' or 'pdf'
        })
        clearInterval(docTimer)
        docGenerating.value = false
        
        if (res.status === 10000 && res.data) {
          docResult.value = res.data
          // 直接下载
          if (docResult.value.docPath) {
            window.open('/v1/gen/download?path=' + encodeURIComponent(docResult.value.docPath), '_blank')
          }
          // 刷新下载列表
          loadDownloads()
        } else {
          errorMessage.value = res.message || '生成文档失败'
          errorType.value = 'error'
        }
      } catch (err) {
        clearInterval(docTimer)
        docGenerating.value = false
        errorMessage.value = err.response ? err.response.data.message : '生成文档时发生错误'
        errorType.value = 'error'
      }
    }

    // 下载代码（最新生成的）
    const downloadCode = () => {
      if (genResult.value && genResult.value.zipPath) {
        window.open('/v1/gen/download?path=' + encodeURIComponent(genResult.value.zipPath), '_blank')
      }
    }

    // 加载下载列表
    const downloadError = ref('')
    const loadDownloads = async () => {
      // 必须有完整的数据库连接信息
      if (!formState.ipAndPort || !formState.dbName || !formState.username) {
        downloadError.value = '请先填写完整的数据库连接信息'
        downloadList.value = []
        return
      }
      
      downloadsLoading.value = true
      downloadError.value = ''
      try {
        const res = await getDownloads({
          ipAndPort: formState.ipAndPort,
          dbName: formState.dbName,
          username: formState.username,
          pwd: formState.pwd
        })
        if (res.status === 10000 && res.data) {
          downloadList.value = res.data
        } else {
          downloadError.value = res.message || '获取下载列表失败'
          downloadList.value = []
        }
      } catch (err) {
        console.error('加载下载列表失败', err)
        downloadError.value = err.message || '连接失败'
        downloadList.value = []
      } finally {
        downloadsLoading.value = false
      }
    }

    // 显示下载中心抽屉
    const showDownloadDrawer = () => {
      downloadDrawerVisible.value = true
      downloadList.value = []
      downloadError.value = ''
      loadDownloads()
    }

    // 下载指定文件
    const downloadFile = (filePath) => {
      window.open('/v1/gen/download?path=' + encodeURIComponent(filePath), '_blank')
    }

    // 删除指定文件
    const handleDeleteFile = async (filePath) => {
      try {
        const res = await deleteFile({ path: filePath })
        if (res.status === 10000) {
          // 刷新列表
          loadDownloads()
        } else {
          errorMessage.value = res.message || '删除失败'
          errorType.value = 'error'
        }
      } catch (err) {
        errorMessage.value = err.response?.data?.message || '删除失败'
        errorType.value = 'error'
      }
    }

    // 显示表关系图（选中表或所有表）
    const handleShowRelationGraph = async ({ key }) => {
      // 必须有完整的数据库连接信息
      if (!formState.ipAndPort || !formState.dbName || !formState.username) {
        errorMessage.value = '请先填写完整的数据库连接信息'
        errorType.value = 'warning'
        return
      }
      
      const isAll = key === 'all'
      showGraphView.value = true
      graphLoading.value = true
      graphData.value = { nodes: [], edges: [] }
      
      try {
        const res = await getTableRelations({
          ipAndPort: formState.ipAndPort,
          dbName: formState.dbName,
          username: formState.username,
          pwd: formState.pwd,
          tabNames: isAll ? '' : selectedRowKeys.value.join(',')
        })
        
        if (res.status === 10000 && res.data) {
          graphData.value = res.data
        } else {
          errorMessage.value = res.message || '获取表关系失败'
          errorType.value = 'error'
        }
      } catch (err) {
        console.error('获取表关系失败', err)
        errorMessage.value = err.message || '获取表关系失败'
        errorType.value = 'error'
      } finally {
        graphLoading.value = false
      }
    }

    // 隐藏关系图，返回表格
    const hideGraphView = () => {
      showGraphView.value = false
      graphData.value = { nodes: [], edges: [] }
    }

    // 处理关系图节点点击 - 保持关系图视图，在右侧面板显示表详情
    // 注：RelationGraph 组件内部已实现右侧详情面板，这里只做选中状态的同步
    const handleGraphNodeClick = (nodeData) => {
      // 同步选中对应的表（用于后续操作）
      if (nodeData.tableName || nodeData.id) {
        const tableName = nodeData.tableName || nodeData.id
        // 如果该表在当前表格数据中，同步选中状态
        if (data.value.some(item => item.name === tableName)) {
          selectedRowKeys.value = [tableName]
        }
      }
      // 保持关系图视图，不在点击节点时返回表格
    }

    // 下载文件数量
    const downloadCount = computed(() => downloadList.value.length)
    
    const handQuery = (e) => {
      e.preventDefault()
      loadData(formState)
    }
    
    const handleCreate = async (record) => {
      console.log('re', record)
      generating.value = true
      genResult.value = null
      startTimer()
      
      try {
        const res = await genCode({ tabName: record.name })
        stopTimer()
        generating.value = false
        
        if (res.status === 10000 && res.data) {
          genResult.value = res.data
        } else {
          errorMessage.value = res.message || '生成失败'
          errorType.value = 'error'
        }
      } catch (error) {
        stopTimer()
        generating.value = false
        errorMessage.value = error.message || '生成代码时发生错误'
        errorType.value = 'error'
      }
    }
    
    const onSelectChange = (keys, rows) => {
      selectedRowKeys.value = keys
      selectedRows.value = rows
      console.log('selectedRowKeys', keys, 'selectedRows', rows)
    }
    
    const resetSearchForm = () => {
      Object.assign(formState, {
        ipAndPort: 'localhost:3306',
        dbName: '',
        username: 'root',
        pwd: '123456',
        include: '',
        exclude: '',
        tablePrefix: ''
      })
      data.value = []
      selectedRowKeys.value = []
      errorMessage.value = ''
      genResult.value = null
      // 重新尝试加载数据库列表
      loadDatabases()
    }

    const prevStep = () => {
      emit('prevStep')
    }

    // 页面加载时获取配置并尝试加载数据库列表
    onMounted(async () => {
      try {
        const res = await getConfig()
        if (res.status === 10000 && res.data) {
          const ds = res.data.dataSource || {}
          if (ds.ipAndPort) formState.ipAndPort = ds.ipAndPort
          if (ds.dbName) formState.dbName = ds.dbName
          if (ds.username) formState.username = ds.username
          if (ds.pwd) formState.pwd = ds.pwd
        }
      } catch (err) {
        console.error('获取配置失败:', err)
      }
      loadDatabases()
    })

    // 组件卸载时清理定时器
    onUnmounted(() => {
      stopTimer()
    })
    
    return {
      formRef,
      table,
      formState,
      columns,
      data,
      loading,
      errorMessage,
      errorType,
      selectedRowKeys,
      selectedRows,
      pagination,
      hasSelected,
      dbOptions,
      dbConnected,
      generating,
      elapsedTime,
      genResult,
      docGenerating,
      docElapsedTime,
      docResult,
      downloadDrawerVisible,
      downloadList,
      downloadsLoading,
      downloadCount,
      downloadError,
      showGraphView,
      graphLoading,
      graphData,
      handleGenCode,
      handleGenDoc,
      handQuery,
      handleCreate,
      onSelectChange,
      resetSearchForm,
      prevStep,
      onDbConfigChange,
      filterOption,
      formatDuration,
      downloadCode,
      showDownloadDrawer,
      downloadFile,
      handleDeleteFile,
      handleShowRelationGraph,
      hideGraphView,
      handleGraphNodeClick
    }
  }
}
</script>

<style scoped>
.download-center-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 100;
}

.download-item-name {
  font-family: monospace;
  font-size: 13px;
  word-break: break-all;
}

.db-col {
  min-width: 200px;
}
.db-col :deep(.ant-form-item-control) {
  flex: 1 1 200px !important;
  min-width: 180px !important;
}
.db-auto-complete {
  width: 100%;
}
.db-auto-complete :deep(.ant-input) {
  width: 100% !important;
}
.db-auto-complete :deep(.ant-select) {
  width: 100% !important;
}

.graph-loading-wrapper {
  text-align: center;
  padding: 150px 0;
  background: #fafafa;
  border-radius: 4px;
  height: 600px;
  border: 1px solid #e8e8e8;
}

.graph-view-container {
  width: 100%;
  height: 600px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
</style>
