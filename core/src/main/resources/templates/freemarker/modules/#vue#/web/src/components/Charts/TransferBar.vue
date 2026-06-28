<template>
  <bar :title="title" :data="data" />
</template>

<script>
import Bar from './Bar.vue'

export default {
  name: 'TransferBar',
  components: { Bar },
  props: {
    title: { type: String, default: '' }
  },
  data () {
    return { data: [] }
  },
  created () {
    this.$http.get('/analysis/month-bar').then(res => {
      this.data = res.result || []
    })
  }
}
</script>
