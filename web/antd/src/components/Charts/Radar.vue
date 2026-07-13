<template>
  <div class="radar-list">
    <div v-for="item in summary" :key="item.item" class="radar-row">
      <span class="radar-name">{{ item.item }}</span>
      <div class="radar-track">
        <span class="radar-fill personal" :style="{ width: item.personal + '%' }" />
        <span class="radar-fill team" :style="{ width: item.team + '%' }" />
        <span class="radar-fill dept" :style="{ width: item.department + '%' }" />
      </div>
    </div>
    <div class="legend">
      <span><i class="personal" />个人</span>
      <span><i class="team" />团队</span>
      <span><i class="dept" />部门</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Radar',
  props: {
    data: { type: Array, default: () => [] }
  },
  computed: {
    summary () {
      const grouped = {}
      this.data.forEach(row => {
        if (!grouped[row.item]) {
          grouped[row.item] = { item: row.item, personal: 0, team: 0, department: 0 }
        }
        if (row.user === '个人') grouped[row.item].personal = row.score
        if (row.user === '团队') grouped[row.item].team = row.score
        if (row.user === '部门') grouped[row.item].department = row.score
      })
      return Object.values(grouped)
    }
  }
}
</script>

<style scoped>
.radar-list {
  padding: 24px;
}
.radar-row {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}
.radar-name {
  color: rgba(0, 0, 0, .65);
}
.radar-track {
  position: relative;
  height: 24px;
  background: #f5f7fa;
}
.radar-fill {
  position: absolute;
  top: 0;
  bottom: 0;
  opacity: .72;
}
.personal { background: #1890ff; }
.team { background: #52c41a; }
.dept { background: #faad14; }
.legend {
  display: flex;
  gap: 16px;
  margin-top: 20px;
}
.legend i {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 6px;
}
</style>
