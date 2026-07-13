<template>
  <div class="liquid-chart" :style="{ width: chartWidth, height: chartHeight }">
    <div class="liquid-ring">
      <div class="liquid-fill" :style="{ height: percentage + '%' }" />
      <span>{{ percentage }}%</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Liquid',
  props: {
    height: { type: Number, default: 160 },
    width: { type: Number, default: 160 },
    value: { type: Number, default: 45 }
  },
  computed: {
    percentage () {
      return Math.max(0, Math.min(100, Math.round(this.value)))
    },
    chartWidth () {
      return this.width ? `${this.width}px` : '160px'
    },
    chartHeight () {
      return this.height ? `${this.height}px` : '160px'
    }
  }
}
</script>

<style scoped>
.liquid-chart {
  display: flex;
  align-items: center;
  justify-content: center;
}
.liquid-ring {
  position: relative;
  width: 100%;
  max-width: 160px;
  aspect-ratio: 1;
  overflow: hidden;
  border: 8px solid #e6f4ff;
  border-radius: 50%;
  background: #f5f7fa;
}
.liquid-fill {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  background: #1890ff;
  opacity: .72;
}
.liquid-ring span {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 600;
  color: rgba(0, 0, 0, .85);
}
</style>
