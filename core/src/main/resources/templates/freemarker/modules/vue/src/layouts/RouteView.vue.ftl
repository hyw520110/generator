<template>
  <router-view v-slot="{ Component }">
    <keep-alive v-if="shouldKeepAlive">
      <component :is="Component" />
    </keep-alive>
    <component :is="Component" v-else />
  </router-view>
</template>

<script>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'

export default {
  name: 'RouteView',
  props: {
    keepAlive: {
      type: Boolean,
      default: true
    }
  },
  setup (props) {
    const route = useRoute()
    const store = useStore()
    
    const shouldKeepAlive = computed(() => {
      const meta = route.meta
      const getters = store.getters
      // 这里增加了 multiTab 的判断，当开启了 multiTab 时
      // 应当全部组件皆缓存，否则会导致切换页面后页面还原成原始状态
      return props.keepAlive || getters.multiTab || meta.keepAlive
    })
    
    return {
      shouldKeepAlive
    }
  }
}
</script>
