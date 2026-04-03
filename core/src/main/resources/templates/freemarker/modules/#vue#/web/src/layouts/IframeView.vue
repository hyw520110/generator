<template>
  <iframe
    :id="id"
    :src="url"
    frameborder="0"
    width="100%"
    height="800px"
    scrolling="auto"></iframe>
</template>

<script>

// 从环境变量获取后端服务器地址
const apiHost = import.meta.env.VITE_API_HOST || 'localhost'
const apiPort = import.meta.env.VITE_API_PORT || '8082'
const baseHost = `http://${apiHost}:${apiPort}`

export default {
  name: 'IframeView',
  data () {
    return {
      url: '',
      id: '',
      height: 'calc(100vh - 132px)'
    }
  },
  created () {
    this.goUrl()
  },
  updated () {
    this.goUrl()
  },
  watch: {
    $route (to, from) {
      this.goUrl()
    }
  },
  methods: {
    goUrl () {
      let url = this.$route.meta.url
      if (url !== null && url !== undefined) {
        // 如果是相对路径，拼接后端服务器地址
        if (url.startsWith('/')) {
          url = baseHost + url
        }
        this.url = url
      }
    }
  }
}
</script>

<style scoped>
iframe {
  width: 100%;
  border: none;
}
</style>