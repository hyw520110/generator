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
    <!-- 统计信息 -->
    <div class="graph-stats" v-if="stats.tableCount">
      <a-tag color="blue">表: {{ stats.tableCount }}</a-tag>
      <a-tag color="green">关系: {{ stats.relationCount }}</a-tag>
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
  FullscreenExitOutlined
} from '@ant-design/icons-vue'

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
    fitViewPadding: 20,
    modes: {
      default: ['drag-canvas', 'zoom-canvas', 'drag-node', 'click-select']
    },
    layout: {
      type: 'dagre',
      rankdir: 'LR',
      nodesep: 80,
      ranksep: 150,
      preventOverlap: true
    },
    defaultNode: {
      type: 'rect',
      size: [180, 50],
      style: {
        fill: '#E6F7FF',
        stroke: '#1890FF',
        lineWidth: 2,
        radius: 4,
        shadowColor: 'rgba(24, 144, 255, 0.2)',
        shadowBlur: 8,
        cursor: 'pointer'
      },
      labelCfg: {
        style: {
          fill: '#333',
          fontSize: 13,
          fontWeight: 500
        }
      }
    },
    nodeStateStyles: {
      selected: {
        fill: '#BAE7FF',
        stroke: '#1890FF',
        lineWidth: 3,
        shadowColor: 'rgba(24, 144, 255, 0.3)',
        shadowBlur: 12
      },
      highlight: {
        fill: '#FFF7E6',
        stroke: '#FA8C16',
        lineWidth: 3,
        shadowColor: 'rgba(250, 140, 22, 0.3)',
        shadowBlur: 12
      },
      hover: {
        fill: '#FFF1F0',
        stroke: '#FF4D4F',
        lineWidth: 2,
        shadowColor: 'rgba(255, 77, 79, 0.2)',
        shadowBlur: 10
      },
      dim: {
        fill: '#F5F5F5',
        stroke: '#D9D9D9',
        lineWidth: 1,
        shadowBlur: 0
      }
    },
    defaultEdge: {
      type: 'polyline',
      style: {
        stroke: '#1890FF',
        lineWidth: 2,
        endArrow: {
          path: 'M 0,0 L 8,4 L 0,8 L 2,4 Z',
          fill: '#1890FF'
        },
        shadowColor: 'rgba(24, 144, 255, 0.1)',
        shadowBlur: 3
      },
      labelCfg: {
        refY: 8,
        style: {
          fill: '#666',
          fontSize: 10,
          background: {
            fill: '#FFF',
            padding: [2, 4, 2, 4],
            radius: 2
          }
        }
      }
    },
    edgeStateStyles: {
      highlight: {
        stroke: '#FA8C16',
        lineWidth: 2.5,
        endArrow: {
          path: 'M 0,0 L 8,4 L 0,8 L 2,4 Z',
          fill: '#FA8C16'
        },
        shadowColor: 'rgba(250, 140, 22, 0.3)',
        shadowBlur: 8,
        // 流动动画效果
        animate: true,
        animateConfig: {
          duration: 1000,
          repeat: true
        }
      },
      dim: {
        stroke: '#E8E8E8',
        lineWidth: 1
      },
      hover: {
        stroke: '#1890FF',
        lineWidth: 2.5,
        endArrow: {
          path: 'M 0,0 L 8,4 L 0,8 L 2,4 Z',
          fill: '#1890FF'
        },
        shadowColor: 'rgba(24, 144, 255, 0.3)',
        shadowBlur: 6
      }
    }
  })
  
  // 布局完成后自动适应画布
  graph.on('afterlayout', () => {
    graph.fitView(20)
    graph.fitCenter()
  })
  
  // 绑定事件
  graph.on('node:mouseenter', (evt) => {
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
  
  graph.on('node:click', (evt) => {
    const node = evt.item
    const model = node.getModel()
    
    // 高亮该节点及其关联的节点和边
    highlightRelated(node.getID())
    
    // 发送点击事件给父组件
    emit('node-click', {
      id: model.id,
      tableName: model.tableName,
      label: model.label,
      comment: model.comment
    })
  })
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
  
  // 转换边数据 - 根据 nullable 设置实线或虚线
  const graphEdges = edges.map((edge, index) => ({
    id: `edge-${index}`,
    source: edge.source,
    target: edge.target,
    label: edge.label || '',
    // nullable=true 表示可选关系，用虚线；nullable=false 表示必选关系，用实线
    style: {
      lineDash: edge.nullable === false ? [] : [5, 3]  // 非空用实线，可空用虚线
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
      // 流动动画效果 - 使用 lineDash 动画
      edge.animate(
        (ratio) => {
          const length = 10 // 虚线段长度
          const offset = ratio * length
          return {
            lineDash: [length, length],
            lineDashOffset: -offset
          }
        },
        {
          repeat: true,
          duration: 1000
        }
      )
    } else {
      graph.setItemState(edge, 'dim', true)
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

// 监听数据变化
watch(() => props.graphData, (newData) => {
  if (!newData || !newData.nodes?.length) return
  
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
}, { deep: true, immediate: true })

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
  background: #fafafa;
  border-radius: 4px;
}

.relation-graph:fullscreen {
  background: #fff;
  padding: 0;
}

.relation-graph:-webkit-full-screen {
  background: #fff;
  padding: 0;
}

.graph-toolbar {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  :deep(.ant-input-affix-wrapper) {
    border-radius: 4px;
  }

  :deep(.ant-btn) {
    border-radius: 4px;
    
    &:hover {
      color: #1890ff;
      border-color: #1890ff;
    }
  }

  :deep(.ant-btn-primary) {
    &:hover {
      opacity: 0.9;
    }
  }
}

.graph-stats {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;

  :deep(.ant-tag) {
    border-radius: 4px;
    padding: 2px 8px;
    font-size: 12px;
  }
}

.graph-legend {
  position: absolute;
  bottom: 12px;
  left: 12px;
  z-index: 10;
  display: flex;
  gap: 16px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  .legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .legend-line {
    width: 24px;
    height: 2px;
    background: #1890ff;
  }

  .legend-solid {
    // 实线
  }

  .legend-dashed {
    background: repeating-linear-gradient(
      90deg,
      #1890ff,
      #1890ff 4px,
      transparent 4px,
      transparent 8px
    );
  }

  .legend-text {
    font-size: 12px;
    color: #666;
  }
}

.graph-container {
  width: 100%;
  height: 100%;
}

.graph-tooltip {
  position: absolute;
  z-index: 100;
  max-width: 300px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.75);
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  pointer-events: none;
  
  .tooltip-title {
    color: #fff;
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 4px;
  }
  
  .tooltip-content {
    color: rgba(255, 255, 255, 0.85);
    font-size: 12px;
    line-height: 1.5;
    word-break: break-word;
  }
  
  .tooltip-hint {
    color: rgba(255, 255, 255, 0.6);
    font-size: 11px;
    margin-top: 4px;
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
  background: rgba(255, 255, 255, 0.9);
}
</style>
