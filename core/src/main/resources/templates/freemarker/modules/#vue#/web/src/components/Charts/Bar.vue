<template>
  <div class="chart-bar">
    <h4 v-if="title">{{ title }}</h4>
    <div class="bars" :style="{ height: height + 'px' }">
      <div v-for="item in normalizedData" :key="item.x" class="bar-item">
        <div class="bar-track">
          <div class="bar-fill" :style="{ height: item.percent + '%' }" />
        </div>
        <span class="bar-label">{{ item.x }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Bar',
  props: {
    title: { type: String, default: '' },
    data: { type: Array, default: () => [] },
    height: { type: Number, default: 254 }
  },
  computed: {
    normalizedData () {
      const max = Math.max(...this.data.map(item => Number(item.y) || 0), 1)
      return this.data.map(item => ({
        ...item,
        percent: Math.max(4, Math.round(((Number(item.y) || 0) / max) * 100))
      }))
    }
  }
}
</script>

<style scoped>
.chart-bar {
  padding: 0 0 32px 32px;
}
.chart-bar h4 {
  margin-bottom: 20px;
}
.bars {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}
.bar-item {
  display: flex;
  flex: 1;
  min-width: 0;
  height: 100%;
  flex-direction: column;
  align-items: center;
}
.bar-track {
  display: flex;
  align-items: flex-end;
  width: 100%;
  height: calc(100% - 24px);
  background: #f5f7fa;
}
.bar-fill {
  width: 100%;
  background: #1890ff;
  transition: height .2s ease;
}
.bar-label {
  width: 100%;
  margin-top: 8px;
  overflow: hidden;
  color: rgba(0, 0, 0, .45);
  font-size: 12px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
