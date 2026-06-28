<template>
  <div class="antv-chart-mini">
    <div class="mini-bars">
      <span v-for="item in normalizedData" :key="item.x" :style="{ height: item.percent + '%' }" />
    </div>
  </div>
</template>

<script>
import moment from 'moment'

const data = []
const beginDay = new Date().getTime()
for (let i = 0; i < 10; i++) {
  data.push({
    x: moment(new Date(beginDay + 1000 * 60 * 60 * 24 * i)).format('YYYY-MM-DD'),
    y: Math.round(Math.random() * 10)
  })
}

export default {
  name: 'MiniBar',
  data () {
    return { data }
  },
  computed: {
    normalizedData () {
      const max = Math.max(...this.data.map(item => Number(item.y) || 0), 1)
      return this.data.map(item => ({ ...item, percent: Math.max(8, ((Number(item.y) || 0) / max) * 100) }))
    }
  }
}
</script>

<style lang="less" scoped>
@import "chart";
.mini-bars {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 46px;
}
.mini-bars span {
  flex: 1;
  min-width: 3px;
  background: #1890ff;
}
</style>
