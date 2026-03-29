<template>
  <div class="relation-graph" ref="relationGraphRef">
    <div class="graph-toolbar">
      <a-input-search
        v-model:value="searchText"
        placeholder="搜索表名"
        style="width: 200px"
        @search="onSearch"
        allow-clear
      />
      <a-button-group style="margin-left: 12px">
        <a-button @click="zoomIn" title="放大">
          <template #icon><ZoomInOutlined /></template>
        </a-button>
        <a-button @click="zoomOut" title="缩小">
          <template #icon><ZoomOutOutlined /></template>
        </a-button>
        <a-button @click="fitView" title="适应画布">
          <template #icon><ExpandOutlined /></template>
        </a-button>
        <a-button @click="resetView" title="重置">
          <template #icon><ReloadOutlined /></template>
        </a-button>
        <a-button @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
          <template #icon>
            <FullscreenExitOutlined v-if="isFullscreen" />
            <FullscreenOutlined v-else />
          </template>
        </a-button>
      </a-button-group>
      <a-button type="primary" style="margin-left: 12px" @click="exportImage">
        <template #icon><DownloadOutlined /></template>
        导出图片
      </a-button>
    </div>
    <!-- 统计信息和视图切换 -->
    <div class="graph-stats" v-if="stats.tableCount">
      <a-tag color="blue">表: {{ stats.tableCount }}</a-tag>
      <a-tag color="green">关系: {{ stats.relationCount }}</a-tag>
      <a-divider type="vertical" style="margin: 0 8px; background: #e2e8f0;" />
      <a-button-group size="small">
        <a-button @click="$emit('switch-view', 'list')">
          <template #icon><UnorderedListOutlined /></template>
          列表
        </a-button>
        <a-button type="primary">
          <template #icon><ApartmentOutlined /></template>
          关系图
        </a-button>
      </a-button-group>
    </div>
    <!-- 图例 -->
    <div class="graph-legend">
      <div class="legend-item">
        <span class="legend-line legend-solid"></span>
        <span class="legend-text">必选关系</span>
      </div>
      <div class="legend-item">
        <span class="legend-line legend-dashed"></span>
        <span class="legend-text">可选关系</span>
      </div>
    </div>
    <div ref="graphContainer" class="graph-container"></div>
    
    <!-- 右侧详情面板 -->
    <aside class="details-panel" :class="{ 'panel-open': selectedTable }">
      <div class="panel-header">
        <div class="panel-title">
          <DatabaseOutlined class="panel-icon" />
          <span v-if="tableDetails">{{ tableDetails.tableName }}</span>
          <span v-else>表详情</span>
        </div>
        <div class="panel-actions">
          <a-button type="text" size="small" @click="copyTableName" title="复制表名">
            <template #icon><CopyOutlined /></template>
          </a-button>
          <a-button type="text" size="small" @click="closeDetailsPanel" title="关闭">
            <template #icon><CloseOutlined /></template>
          </a-button>
        </div>
      </div>
      
      <div class="panel-body">
        <!-- 加载状态 -->
        <div v-if="detailsLoading" class="panel-loading">
          <a-spin size="small" />
          <span>加载中...</span>
        </div>
        
        <!-- 表基本信息 -->
        <div v-else-if="tableDetails" class="panel-content">
          <div class="detail-section">
            <div class="section-title">
              <InfoCircleOutlined />
              <span>基本信息</span>
            </div>
            <div class="detail-item">
              <span class="item-label">表名</span>
              <span class="item-value mono">{{ tableDetails.tableName }}</span>
            </div>
            <div class="detail-item" v-if="tableDetails.comment">
              <span class="item-label">备注</span>
              <span class="item-value">{{ tableDetails.comment }}</span>
            </div>
            <div class="detail-item">
              <span class="item-label">字段数</span>
              <span class="item-value">{{ tableDetails.columns?.length || 0 }}</span>
            </div>
          </div>
          
          <!-- 字段列表 -->
          <div class="detail-section">
            <div class="section-title">
              <TableOutlined />
              <span>字段列表</span>
              <span class="section-count">{{ tableDetails.columns?.length || 0 }}</span>
            </div>
            <div class="column-list">
              <div 
                v-for="(col, index) in tableDetails.columns" 
                :key="index" 
                class="column-item"
                :class="{ 'is-primary': col.isPrimary }"
              >
                <div class="column-header">
                  <span class="column-name mono">{{ col.columnName }}</span>
                  <a-tag v-if="col.isPrimary" size="small" color="blue">PK</a-tag>
                  <a-tag v-else-if="col.isForeignKey" size="small" color="orange">FK</a-tag>
                </div>
                <div class="column-meta">
                  <span class="column-type">{{ col.dataType }}</span>
                  <span v-if="col.isNullable === false" class="column-not-null">NOT NULL</span>
                </div>
                <div v-if="col.comment" class="column-comment">{{ col.comment }}</div>
                <div v-if="col.defaultValue" class="column-default">
                  <span class="default-label">默认值:</span>
                  <code class="default-value mono">{{ col.defaultValue }}</code>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 外键关系 -->
          <div v-if="tableDetails.foreignKeys && tableDetails.foreignKeys.length > 0" class="detail-section">
            <div class="section-title">
              <LinkOutlined />
              <span>外键关系</span>
            </div>
            <div class="fk-list">
              <div v-for="(fk, index) in tableDetails.foreignKeys" :key="index" class="fk-item">
                <div class="fk-info">
                  <span class="fk-column mono">{{ fk.column }}</span>
                  <ArrowRightOutlined class="fk-arrow" />
                  <span class="fk-ref mono">{{ fk.referenceTable }}.{{ fk.referenceColumn }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 未选中状态 -->
        <div v-else class="panel-placeholder">
          <ThunderboltOutlined class="placeholder-icon" />
          <p>点击表节点查看详情</p>
        </div>
      </div>
    </aside>
    <!-- Tooltip -->
    <div v-show="tooltip.visible" class="graph-tooltip" :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }">
      <div class="tooltip-title">{{ tooltip.title }}</div>
      <div v-if="tooltip.content" class="tooltip-content">{{ tooltip.content }}</div>
      <div v-if="tooltip.tableName && tooltip.tableName !== tooltip.title" class="tooltip-hint">
        表名: {{ tooltip.tableName }}
      </div>
    </div>
    <div v-if="loading" class="graph-loading">
      <a-spin size="large" tip="加载中..." />
    </div>
    <div v-if="!loading && (!graphData.nodes || graphData.nodes.length === 0)" class="graph-empty">
      <a-empty description="暂无表关系数据" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Graph } from '@antv/g6'
import {
  ZoomInOutlined,
  ZoomOutOutlined,
  ExpandOutlined,
  ReloadOutlined,
  DownloadOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  DatabaseOutlined,
  CopyOutlined,
  CloseOutlined,
  InfoCircleOutlined,
  TableOutlined,
  LinkOutlined,
  ArrowRightOutlined,
  ThunderboltOutlined,
  UnorderedListOutlined,
  ApartmentOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const props = defineProps({
  graphData: {
    type: Object,
    default: () => ({ nodes: [], edges: [] })
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['export', 'node-click'])

const relationGraphRef = ref(null)
const graphContainer = ref(null)
const searchText = ref('')
let graph = null
const stats = ref({ tableCount: 0, relationCount: 0 })
const tooltip = ref({ visible: false, x: 0, y: 0, title: '', content: '', tableName: '' })
const isFullscreen = ref(false)

// 选中表的详情数据
const selectedTable = ref(null)
const tableDetails = ref(null)
const detailsLoading = ref(false)

// 初始化图
const initGraph = () => {
  if (!graphContainer.value) return
  
  const width = graphContainer.value.offsetWidth
  const height = graphContainer.value.offsetHeight
  
  if (width === 0 || height === 0) return
  
  graph = new Graph({
    container: graphContainer.value,
    width,
    height,
    fitCenter: true,
    fitViewPadding: 40,
    modes: {
      default: ['drag-canvas', 'zoom-canvas', 'drag-node']
    },
    layout: {
      type: 'dagre',
      rankdir: 'LR',
      nodesep: 100,
      ranksep: 180,
      preventOverlap: true,
      controlPoints: true
    },
    defaultNode: {
      type: 'rect',
      size: [240, 64],
      style: {
        fill: '#ffffff',
        stroke: '#e3f2fd',
        lineWidth: 2,
        radius: 8,
        shadowColor: 'rgba(0, 0, 0, 0.08)',
        shadowBlur: 12,
        cursor: 'pointer',
        lineDash: null
      },
      labelCfg: {
        position: 'center',
        style: {
          fill: '#1f2937',
          fontSize: 15,
          fontWeight: 600,
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial'
        }
      }
    },
    nodeStateStyles: {
      selected: {
        fill: '#eff6ff',
        stroke: '#3b82f6',
        lineWidth: 3,
        shadowColor: 'rgba(59, 130, 246, 0.4)',
        shadowBlur: 16
      },
      highlight: {
        fill: '#fff7ed',
        stroke: '#f97316',
        lineWidth: 3,
        shadowColor: 'rgba(249, 115, 22, 0.4)',
        shadowBlur: 16
      },
      hover: {
        fill: '#fef2f2',
        stroke: '#ef4444',
        lineWidth: 2.5,
        shadowColor: 'rgba(239, 68, 68, 0.3)',
        shadowBlur: 14
      },
      dim: {
        fill: '#f9fafb',
        stroke: '#e5e7eb',
        lineWidth: 1,
        shadowBlur: 0,
        opacity: 0.5
      }
    },
    defaultEdge: {
      type: 'cubic-horizontal',
      style: {
        stroke: '#94a3b8',
        lineWidth: 2,
        endArrow: {
          path: 'M 0,0 L 8,4 L 0,8 L 2,4 Z',
          fill: '#94a3b8'
        },
        shadowColor: 'rgba(0, 0, 0, 0.05)',
        shadowBlur: 4,
        opacity: 0.8
      },
      labelCfg: {
        refY: 10,
        autoRotate: true,
        style: {
          fill: '#64748b',
          fontSize: 11,
          fontWeight: 500,
          background: {
            fill: '#ffffff',
            padding: [3, 6, 3, 6],
            radius: 4,
            stroke: '#e2e8f0',
            lineWidth: 1
          }
        }
      }
    },
    edgeStateStyles: {
      highlight: {
        stroke: '#f97316',
        lineWidth: 3,
        endArrow: {
          path: 'M 0,0 L 8,4 L 0,8 L 2,4 Z',
          fill: '#f97316'
        },
        shadowColor: 'rgba(249, 115, 22, 0.4)',
        shadowBlur: 10,
        // 流动动画效果
        lineDash: [5, 3],
        animate: true,
        animateConfig: {
          duration: 1500,
          repeat: true
        }
      },
      dim: {
        stroke: '#e5e7eb',
        lineWidth: 1,
        opacity: 0.3
      },
      hover: {
        stroke: '#3b82f6',
        lineWidth: 3,
        endArrow: {
          path: 'M 0,0 L 8,4 L 0,8 L 2,4 Z',
          fill: '#3b82f6'
        },
        shadowColor: 'rgba(59, 130, 246, 0.4)',
        shadowBlur: 10
      }
    }
  })
  
  // 布局完成后自动适应画布
  graph.on('afterlayout', () => {
    console.log('[G6] 布局完成，节点数:', graph.getNodes().length, '边数:', graph.getEdges().length)
    graph.fitView(20)
    graph.fitCenter()
    // 布局完成后启动流动动画
    startEdgeAnimation()
  })
  
  // 绑定事件
  graph.on('node:mouseenter', (evt) => {
    console.log('[G6] 节点悬停:', evt.item.getModel().tableName)
    const node = evt.item
    const model = node.getModel()
    graph.setItemState(node, 'hover', true)

    // 显示 tooltip
    tooltip.value = {
      visible: true,
      x: evt.canvasX + 15,
      y: evt.canvasY + 15,
      title: model.comment || model.label || model.tableName,
      content: model.comment ? '' : '',
      tableName: model.tableName || model.id
    }

    // 高亮相关节点和边
    highlightRelated(node.getID())
  })
  
  graph.on('node:mouseleave', (evt) => {
    const node = evt.item
    graph.setItemState(node, 'hover', false)
    tooltip.value.visible = false
    clearHighlight()
  })
  
  // 边悬停事件
  graph.on('edge:mouseenter', (evt) => {
    const edge = evt.item
    const model = edge.getModel()
    graph.setItemState(edge, 'hover', true)
    
    // 显示边 tooltip
    const sourceNode = graph.findById(model.source)
    const targetNode = graph.findById(model.target)
    const sourceName = sourceNode ? sourceNode.getModel().tableName : model.source
    const targetName = targetNode ? targetNode.getModel().tableName : model.target
    
    const nullableText = model.nullable === false ? '必选' : '可选'
    
    tooltip.value = {
      visible: true,
      x: evt.canvasX + 15,
      y: evt.canvasY + 15,
      title: `${sourceName} → ${targetName}`,
      content: `${model.fkColumn || model.label} → ${model.pkColumn || 'id'} (${nullableText})`,
      tableName: ''
    }
  })
  
  graph.on('edge:mouseleave', (evt) => {
    const edge = evt.item
    graph.setItemState(edge, 'hover', false)
    tooltip.value.visible = false
  })

  // 节点点击事件
  graph.on('node:click', (evt) => {
    console.log('节点被点击:', evt.item.getModel())
    const node = evt.item
    const model = node.getModel()

    // 高亮该节点及其关联的节点和边
    highlightRelated(node.getID())

    // 设置选中的表
    selectedTable.value = {
      id: model.id,
      tableName: model.tableName,
      label: model.label,
      comment: model.comment
    }

    // 加载表详情数据
    loadTableDetails(model.id)

    // 发送点击事件给父组件
    emit('node-click', {
      id: model.id,
      tableName: model.tableName,
      label: model.label,
      comment: model.comment
    })
  })

  // 画布空白区域点击，关闭面板
  graph.on('canvas:click', (evt) => {
    console.log('画布被点击')
    closeDetailsPanel()
  })
}

// 加载表详情数据
const loadTableDetails = async (tableId) => {
  console.log('[loadTableDetails] 开始加载表详情:', tableId)
  console.log('[loadTableDetails] props.graphData:', props.graphData)
  
  detailsLoading.value = true
  try {
    // 从 props.graphData 中获取表详情数据
    const tableDetailsData = props.graphData?.tableDetails || {}
    const tableForeignKeysData = props.graphData?.tableForeignKeys || {}
    
    console.log('[loadTableDetails] tableDetailsData:', tableDetailsData)
    console.log('[loadTableDetails] tableForeignKeysData:', tableForeignKeysData)
    
    // 获取表的基本信息
    const tableNode = props.graphData?.nodes?.find(n => n.id === tableId || n.tableName === tableId)
    console.log('[loadTableDetails] tableNode:', tableNode)
    
    if (tableDetailsData[tableId] && tableDetailsData[tableId].length > 0) {
      // 使用 API 返回的表详情数据
      tableDetails.value = {
        tableName: tableNode?.tableName || tableId,
        comment: tableNode?.comment || '',
        columns: tableDetailsData[tableId],
        foreignKeys: tableForeignKeysData[tableId] || []
      }
      console.log('[loadTableDetails] 表详情数据加载成功:', tableDetails.value)
    } else {
      // 如果没有 tableDetails 数据，使用模拟数据（用于测试）
      console.log('[loadTableDetails] 未找到表详情数据，使用模拟数据:', tableId)
      tableDetails.value = {
        tableName: tableNode?.tableName || tableId,
        comment: tableNode?.comment || '系统用户表',
        columns: [
          { columnName: 'id', dataType: 'bigint', isPrimary: true, isForeignKey: false, isNullable: false, comment: '主键 ID', defaultValue: null },
          { columnName: 'username', dataType: 'varchar(50)', isPrimary: false, isForeignKey: false, isNullable: false, comment: '用户名', defaultValue: null },
          { columnName: 'email', dataType: 'varchar(100)', isPrimary: false, isForeignKey: false, isNullable: false, comment: '邮箱', defaultValue: null },
          { columnName: 'status', dataType: 'tinyint', isPrimary: false, isForeignKey: false, isNullable: false, comment: '状态：0-禁用，1-启用', defaultValue: '1' },
          { columnName: 'created_at', dataType: 'datetime', isPrimary: false, isForeignKey: false, isNullable: false, comment: '创建时间', defaultValue: 'CURRENT_TIMESTAMP' }
        ],
        foreignKeys: []
      }
    }
  } catch (error) {
    console.error('[loadTableDetails] 加载表详情失败:', error)
    message.error('加载表详情失败')
  } finally {
    detailsLoading.value = false
  }
}

// 关闭详情面板
const closeDetailsPanel = () => {
  selectedTable.value = null
  tableDetails.value = null
}

// 复制表名
const copyTableName = async () => {
  if (!tableDetails.value?.tableName) return
  
  try {
    await navigator.clipboard.writeText(tableDetails.value.tableName)
    message.success('表名已复制到剪贴板')
  } catch (error) {
    message.error('复制失败')
  }
}

// 更新图数据
const updateGraph = () => {
  if (!graph || !props.graphData) return
  
  const { nodes, edges, tableCount, relationCount } = props.graphData
  
  stats.value = { tableCount: tableCount || nodes?.length || 0, relationCount: relationCount || edges?.length || 0 }
  
  if (!nodes || nodes.length === 0) {
    graph.clear()
    return
  }
  
  // 转换节点数据
  const graphNodes = nodes.map(node => ({
    id: node.id,
    label: node.label || node.tableName || node.id,
    tableName: node.tableName,
    comment: node.comment,
    ...node
  }))
  
  // 转换边数据 - 根据 nullable 设置实线或虚线，并添加流动动画
  const graphEdges = edges.map((edge, index) => ({
    id: `edge-${index}`,
    source: edge.source,
    target: edge.target,
    label: edge.label || '',
    // nullable=true 表示可选关系，用虚线；nullable=false 表示必选关系，用实线
    style: {
      lineDash: edge.nullable === false ? [] : [5, 3],  // 非空用实线，可空用虚线
      // 添加流动动画配置
      animate: {
        type: 'line-dash',
        duration: 2000,  // 动画周期（毫秒）
        repeat: true     // 循环播放
      }
    },
    ...edge
  }))
  
  // G6 v4: 使用 data() 设置数据，render() 渲染
  graph.data({ nodes: graphNodes, edges: graphEdges })
  graph.render()

  // 延迟调用 fitView 确保布局完成
  setTimeout(() => {
    if (graph) {
      graph.fitView(20)
      graph.fitCenter()
      // 启动流动动画
      startEdgeAnimation()
    }
  }, 300)
}

// 高亮相关节点和边
const highlightRelated = (nodeId) => {
  const edges = graph.getEdges()
  const nodes = graph.getNodes()

  // 找到与当前节点相关的节点和边
  const relatedNodes = new Set([nodeId])
  const relatedEdges = new Set()

  edges.forEach(edge => {
    const model = edge.getModel()
    if (model.source === nodeId || model.target === nodeId) {
      relatedNodes.add(model.source)
      relatedNodes.add(model.target)
      relatedEdges.add(edge)
    }
  })

  // 设置节点状态
  nodes.forEach(node => {
    const id = node.getID()
    if (relatedNodes.has(id)) {
      graph.setItemState(node, 'highlight', true)
    } else {
      graph.setItemState(node, 'dim', true)
    }
  })

  // 设置边状态并添加流动动画
  edges.forEach(edge => {
    if (relatedEdges.has(edge)) {
      graph.setItemState(edge, 'highlight', true)
      // G6 v4 使用 updateItem 来实现动画效果
      const model = edge.getModel()
      const originalLineDash = model.nullable === false ? [] : [5, 3]
      
      // 使用 lineDash 动画实现流动效果
      let offset = 0
      const animateFlow = () => {
        if (!graph || edge.get('destroyed')) return
        offset = (offset + 1) % 10
        graph.updateItem(edge, {
          lineDashOffset: -offset
        })
        requestAnimationFrame(animateFlow)
      }
      requestAnimationFrame(animateFlow)
    } else {
      graph.setItemState(edge, 'dim', true)
    }
  })
}

// 启动边的持续流动动画
const startEdgeAnimation = () => {
  if (!graph) return

  const edges = graph.getEdges()
  edges.forEach((edge) => {
    const model = edge.getModel()
    const isNullable = model.nullable !== false // 可空关系用虚线

    // 设置初始 lineDash
    const lineDash = isNullable ? [5, 3] : [0, 0]

    // 使用 G6 的 animate 方法实现流动效果
    // 通过不断改变 lineDashOffset 实现虚线流动
    if (lineDash[0] > 0) {
      // 虚线才有流动效果
      let offset = 0
      const animate = () => {
        if (!graph || edge.get('destroyed')) return
        offset = (offset + 0.5) % 8 // 控制流动速度
        graph.updateItem(edge, {
          lineDashOffset: -offset
        })
        requestAnimationFrame(animate)
      }
      requestAnimationFrame(animate)
    }
  })
}

// 清除高亮
const clearHighlight = () => {
  if (!graph) return
  
  graph.getNodes().forEach(node => {
    graph.clearItemStates(node, ['highlight', 'dim'])
  })
  
  graph.getEdges().forEach(edge => {
    graph.clearItemStates(edge, ['highlight', 'dim'])
    // 停止动画
    edge.stopAnimate()
    // 恢复原始的 lineDash（根据 nullable）
    const model = edge.getModel()
    const originalLineDash = model.nullable === false ? [] : [5, 3]
    graph.updateItem(edge, {
      style: {
        lineDash: originalLineDash,
        lineDashOffset: 0
      }
    })
  })
}

// 搜索
const onSearch = (value) => {
  if (!graph) return
  
  if (!value) {
    clearHighlight()
    return
  }
  
  const nodes = graph.getNodes()
  const lowerValue = value.toLowerCase()
  
  nodes.forEach(node => {
    const model = node.getModel()
    const tableName = (model.tableName || model.id || '').toLowerCase()
    const label = (model.label || '').toLowerCase()
    
    if (tableName.includes(lowerValue) || label.includes(lowerValue)) {
      graph.setItemState(node, 'highlight', true)
    } else {
      graph.setItemState(node, 'dim', true)
    }
  })
}

// 缩放
const zoomIn = () => {
  if (graph) {
    const zoom = graph.getZoom()
    graph.zoomTo(zoom * 1.2)
  }
}

const zoomOut = () => {
  if (graph) {
    const zoom = graph.getZoom()
    graph.zoomTo(zoom / 1.2)
  }
}

const fitView = () => {
  if (graph) {
    graph.fitView(20)
  }
}

const resetView = () => {
  if (graph) {
    graph.zoomTo(1)
    graph.fitView(20)
  }
}

// 导出图片
const exportImage = () => {
  if (graph) {
    graph.downloadFullImage('table-relation-graph', 'image/png', {
      backgroundColor: '#FFF'
    })
  }
}

// 全屏切换
const toggleFullscreen = () => {
  if (!relationGraphRef.value) return
  
  if (!document.fullscreenElement) {
    relationGraphRef.value.requestFullscreen().then(() => {
      isFullscreen.value = true
      // 全屏后重新调整图大小
      setTimeout(() => {
        if (graph && graphContainer.value) {
          graph.changeSize(graphContainer.value.offsetWidth, graphContainer.value.offsetHeight)
          graph.fitView(20)
        }
      }, 100)
    }).catch(err => {
      console.error('全屏失败:', err)
    })
  } else {
    document.exitFullscreen().then(() => {
      isFullscreen.value = false
      // 退出全屏后重新调整图大小
      setTimeout(() => {
        if (graph && graphContainer.value) {
          graph.changeSize(graphContainer.value.offsetWidth, graphContainer.value.offsetHeight)
          graph.fitView(20)
        }
      }, 100)
    })
  }
}

// 监听全屏变化
const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
  // 全屏状态变化后调整图大小
  setTimeout(() => {
    if (graph && graphContainer.value) {
      graph.changeSize(graphContainer.value.offsetWidth, graphContainer.value.offsetHeight)
      graph.fitView(20)
    }
  }, 100)
}

// 监听数据变化（只监听 nodes 变化，避免不必要的重新渲染）
watch(() => props.graphData?.nodes, (newNodes) => {
  console.log('[watch] graphData.nodes 变化，节点数:', newNodes?.length)
  if (!newNodes || newNodes.length === 0) return

  // 如果 graph 还未初始化，等待初始化完成后再更新
  if (!graph) {
    // 尝试初始化
    nextTick(() => {
      setTimeout(() => {
        if (!graph) {
          initGraph()
        }
        if (graph && props.graphData && props.graphData.nodes?.length > 0) {
          updateGraph()
        }
      }, 50)
    })
    return
  }

  nextTick(() => {
    updateGraph()
  })
}, { immediate: true })

// 窗口大小变化
const handleResize = () => {
  if (graph && graphContainer.value) {
    graph.changeSize(graphContainer.value.offsetWidth, graphContainer.value.offsetHeight)
  }
}

onMounted(() => {
  // 等待 DOM 更新完成后再初始化
  nextTick(() => {
    // 使用 setTimeout 确保容器已完成渲染
    setTimeout(() => {
      initGraph()
      if (graph && props.graphData && props.graphData.nodes?.length > 0) {
        updateGraph()
      }
    }, 100)
  })
  
  window.addEventListener('resize', handleResize)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onUnmounted(() => {
  if (graph) {
    graph.destroy()
    graph = null
  }
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
</script>

<style lang="less" scoped>
.relation-graph {
  position: relative;
  width: 100%;
  height: 100%;
  background: #f8fafc;
  border-radius: 8px;
  overflow: hidden;
}

.relation-graph:fullscreen {
  background: #f1f5f9;
  padding: 0;
  border-radius: 0;
}

.relation-graph:-webkit-full-screen {
  background: #f1f5f9;
  padding: 0;
  border-radius: 0;
}

.graph-toolbar {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06), 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08), 0 2px 4px rgba(0, 0, 0, 0.06);
  }

  :deep(.ant-input-affix-wrapper) {
    border-radius: 6px;
    border-color: #e2e8f0;
    transition: all 0.2s;

    &:focus,
    &:hover {
      border-color: #3b82f6;
      box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
    }
  }

  :deep(.ant-btn) {
    border-radius: 6px;
    border-color: #e2e8f0;
    background: #ffffff;
    transition: all 0.2s;

    &:hover {
      color: #3b82f6;
      border-color: #3b82f6;
      background: #eff6ff;
    }
  }

  :deep(.ant-btn-primary) {
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    border: none;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);

    &:hover {
      background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
    }
  }
}

.graph-stats {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);

  :deep(.ant-tag) {
    border-radius: 6px;
    padding: 3px 10px;
    font-size: 12px;
    font-weight: 500;
    border: none;
  }

  :deep(.ant-tag-blue) {
    background: #eff6ff;
    color: #3b82f6;
  }

  :deep(.ant-tag-green) {
    background: #f0fdf4;
    color: #22c55e;
  }
}

.graph-legend {
  position: absolute;
  bottom: 16px;
  left: 16px;
  z-index: 10;
  display: flex;
  gap: 20px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);

  .legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .legend-line {
    width: 28px;
    height: 2px;
    background: #3b82f6;
    border-radius: 2px;
  }

  .legend-solid {
    // 实线
  }

  .legend-dashed {
    background: repeating-linear-gradient(
      90deg,
      #3b82f6,
      #3b82f6 5px,
      transparent 5px,
      transparent 10px
    );
  }

  .legend-text {
    font-size: 12px;
    color: #64748b;
    font-weight: 500;
  }
}

.graph-container {
  width: 100%;
  height: 100%;
  background-image: 
    linear-gradient(rgba(59, 130, 246, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.04) 1px, transparent 1px);
  background-size: 20px 20px;
  background-position: center center;
  transition: all 0.3s ease;
}

// 右侧详情面板
.details-panel {
  position: absolute;
  top: 0;
  right: -400px;
  width: 400px;
  height: 100%;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(16px);
  border-left: 1px solid #e2e8f0;
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.06);
  transition: right 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 100;
  display: flex;
  flex-direction: column;
  
  &.panel-open {
    right: 0;
  }
  
  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    border-bottom: 1px solid #f1f5f9;
    background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
    
    .panel-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 16px;
      font-weight: 600;
      color: #1e293b;
      
      .panel-icon {
        color: #3b82f6;
        font-size: 18px;
      }
    }
    
    .panel-actions {
      display: flex;
      gap: 4px;
      
      :deep(.ant-btn) {
        color: #64748b;
        
        &:hover {
          color: #3b82f6;
          background: #eff6ff;
        }
      }
    }
  }
  
  .panel-body {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    background: #ffffff;
    
    &::-webkit-scrollbar {
      width: 6px;
    }
    
    &::-webkit-scrollbar-thumb {
      background: #cbd5e1;
      border-radius: 3px;
      
      &:hover {
        background: #94a3b8;
      }
    }
  }
  
  .panel-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 200px;
    gap: 12px;
    color: #64748b;
  }
  
  .panel-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 200px;
    color: #94a3b8;
    
    .placeholder-icon {
      font-size: 48px;
      margin-bottom: 16px;
      opacity: 0.5;
    }
    
    p {
      margin: 0;
      font-size: 14px;
    }
  }
  
  .panel-content {
    .detail-section {
      margin-bottom: 24px;
      
      .section-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        font-weight: 600;
        color: #334155;
        margin-bottom: 12px;
        padding-bottom: 8px;
        border-bottom: 2px solid #e2e8f0;
        
        svg {
          color: #3b82f6;
        }
        
        .section-count {
          margin-left: auto;
          font-size: 12px;
          font-weight: 400;
          color: #94a3b8;
          background: #f1f5f9;
          padding: 2px 8px;
          border-radius: 10px;
        }
      }
      
      .detail-item {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        padding: 10px 0;
        border-bottom: 1px solid #f8fafc;
        
        &:last-child {
          border-bottom: none;
        }
        
        .item-label {
          font-size: 13px;
          color: #64748b;
          font-weight: 500;
        }
        
        .item-value {
          font-size: 13px;
          color: #1e293b;
          text-align: right;
          max-width: 200px;
          word-break: break-word;
        }
      }
    }
    
    .column-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      
      .column-item {
        padding: 12px 14px;
        background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        transition: all 0.2s ease;
        
        &:hover {
          border-color: #3b82f6;
          box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
          transform: translateY(-1px);
        }
        
        &.is-primary {
          border-color: #3b82f6;
          background: linear-gradient(135deg, #eff6ff 0%, #ffffff 100%);
        }
        
        .column-header {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 8px;
          
          .column-name {
            font-size: 14px;
            font-weight: 600;
            color: #1e293b;
          }
        }
        
        .column-meta {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 6px;
          
          .column-type {
            font-size: 12px;
            color: #f97316;
            font-family: 'Monaco', 'Consolas', monospace;
            font-weight: 500;
          }
          
          .column-not-null {
            font-size: 10px;
            color: #ef4444;
            font-weight: 600;
            background: #fef2f2;
            padding: 1px 6px;
            border-radius: 3px;
          }
        }
        
        .column-comment {
          font-size: 12px;
          color: #64748b;
          margin-bottom: 6px;
          line-height: 1.4;
        }
        
        .column-default {
          font-size: 11px;
          color: #94a3b8;
          
          .default-label {
            margin-right: 4px;
          }
          
          .default-value {
            background: #f1f5f9;
            padding: 2px 6px;
            border-radius: 3px;
            color: #475569;
          }
        }
      }
    }
    
    .fk-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
      
      .fk-item {
        padding: 10px 12px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 6px;
        
        .fk-info {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 13px;
          color: #64748b;
          
          .fk-column {
            font-weight: 500;
            color: #1e293b;
          }
          
          .fk-arrow {
            color: #94a3b8;
            font-size: 12px;
          }
          
          .fk-ref {
            color: #3b82f6;
          }
        }
      }
    }
  }
}

.graph-tooltip {
  position: absolute;
  z-index: 100;
  max-width: 320px;
  min-width: 180px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(12px);
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12), 0 2px 6px rgba(0, 0, 0, 0.08);
  pointer-events: none;
  transition: all 0.15s ease;

  .tooltip-title {
    color: #1e293b;
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 6px;
    line-height: 1.4;
  }

  .tooltip-content {
    color: #64748b;
    font-size: 12px;
    line-height: 1.5;
    word-break: break-word;
  }

  .tooltip-hint {
    color: #94a3b8;
    font-size: 11px;
    margin-top: 6px;
    padding-top: 6px;
    border-top: 1px solid #f1f5f9;
    font-family: 'Monaco', 'Consolas', monospace;
  }
}

.graph-loading,
.graph-empty {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
}
</style>
