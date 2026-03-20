<script>
import { h } from 'vue'

export default {
  name: 'RouteView',
  props: {
    keepAlive: {
      type: Boolean,
      default: true
    }
  },
  render () {
    const { $route: { meta }, $store: { getters } } = this
    // 这里增加了 multiTab 的判断，当开启了 multiTab 时
    // 应当全部组件皆缓存，否则会导致切换页面后页面还原成原始状态
    // 若确实不需要，可改为 return meta.keepAlive ? inKeep : notKeep
    const showKeepAlive = this.keepAlive || getters.multiTab || meta.keepAlive
    
    if (!getters.multiTab && !meta.keepAlive) {
      return h('router-view')
    }
    
    if (showKeepAlive) {
      return h('keep-alive', {}, [
        h('router-view')
      ])
    }
    return h('router-view')
  }
}
</script>