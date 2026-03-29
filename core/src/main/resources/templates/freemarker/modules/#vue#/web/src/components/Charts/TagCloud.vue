<template>
  <div class="tag-cloud" :style="{ width: width + 'px', height: height + 'px' }">
    <div v-if="data.length === 0" class="empty">暂无数据</div>
    <div v-else class="tag-cloud-content">
      <span
        v-for="(item, index) in displayTags"
        :key="index"
        class="tag-item"
        :style="{
          fontSize: item.size + 'px',
          color: colors[index % colors.length],
          transform: `rotate(${item.rotate}deg)`
        }"
      >
        {{ item.name }}
      </span>
    </div>
  </div>
</template>

<script>
import * as DataSet from '@antv/data-set'

const colors = ['#1890ff', '#2fc25b', '#facc14', '#223273', '#8543e0', '#13c2c2', '#3436c7', '#f04864']

export default {
  name: 'TagCloud',
  props: {
    tagList: {
      type: Array,
      required: true
    },
    height: {
      type: Number,
      default: 400
    },
    width: {
      type: Number,
      default: 640
    }
  },
  data () {
    return {
      data: [],
      colors
    }
  },
  computed: {
    displayTags () {
      return this.data.map((item, index) => ({
        ...item,
        size: Math.max(12, Math.min(32, item.size || 16)),
        rotate: Math.random() > 0.5 ? 0 : 90
      }))
    }
  },
  watch: {
    tagList: function (val) {
      if (val.length > 0) {
        this.initTagCloud(val)
      }
    }
  },
  mounted () {
    if (this.tagList.length > 0) {
      this.initTagCloud(this.tagList)
    }
  },
  methods: {
    initTagCloud (dataSource) {
      // 简化版词云，直接处理数据
      const values = dataSource.map(d => d.value || 0)
      const min = Math.min(...values)
      const max = Math.max(...values)
      
      this.data = dataSource.map(item => ({
        name: item.name,
        value: item.value,
        size: min === max ? 20 : ((item.value - min) / (max - min)) * (32 - 12) + 12
      }))
    }
  }
}
</script>

<style lang="less" scoped>
.tag-cloud {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  
  .empty {
    color: #999;
    font-size: 14px;
  }
  
  .tag-cloud-content {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 16px;
    
    .tag-item {
      display: inline-block;
      padding: 4px 8px;
      cursor: pointer;
      transition: all 0.3s;
      
      &:hover {
        transform: scale(1.2) !important;
        opacity: 0.8;
      }
    }
  }
}
</style>