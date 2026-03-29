<template>
  <div class="tag-cloud-placeholder" :style="{ width: width + 'px', height: height + 'px' }">
    <div v-for="(tag, index) in displayTags" :key="index" 
         :style="{ 
           fontSize: getFontSize(tag.value) + 'px', 
           color: getColor(index),
           display: 'inline-block',
           margin: '5px',
           padding: '2px 8px'
         }">
      {{ tag.name }}
    </div>
  </div>
</template>

<script>
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
  computed: {
    displayTags () {
      return this.tagList || []
    },
    min () {
      if (!this.tagList.length) return 0
      return Math.min(...this.tagList.map(t => t.value))
    },
    max () {
      if (!this.tagList.length) return 1
      return Math.max(...this.tagList.map(t => t.value))
    }
  },
  methods: {
    getFontSize (value) {
      return ((value - this.min) / (this.max - this.min || 1)) * (32 - 8) + 8
    },
    getColor (index) {
      const colors = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16']
      return colors[index % colors.length]
    }
  }
}
</script>

<style scoped>
.tag-cloud-placeholder {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 4px;
}
</style>