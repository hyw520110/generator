<template>
  <div class="antv-chart-mini">
    <svg class="mini-area" viewBox="0 0 120 46" preserveAspectRatio="none">
      <polygon :points="areaPoints" fill="rgba(24, 144, 255, .16)" />
      <polyline :points="linePoints" fill="none" stroke="#1890ff" stroke-width="2" />
    </svg>
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

function points (rows) {
  const max = Math.max(...rows.map(item => Number(item.y) || 0), 1)
  const step = rows.length > 1 ? 120 / (rows.length - 1) : 120
  return rows.map((item, index) => `${index * step},${46 - ((Number(item.y) || 0) / max) * 40 - 3}`).join(' ')
}

export default {
  name: 'MiniArea',
  data () {
    return { data }
  },
  computed: {
    linePoints () {
      return points(this.data)
    },
    areaPoints () {
      return `0,46 ${this.linePoints} 120,46`
    }
  }
}
</script>

<style lang="less" scoped>
@import "chart";
.mini-area {
  width: 100%;
  height: 46px;
}
</style>
