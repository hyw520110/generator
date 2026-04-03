import { ref, reactive } from 'vue'

// Vue 3 使用简单的响应式事件系统
const events = reactive({
  listeners: {}
})

export default {
  $on(event, callback) {
    if (!events.listeners[event]) {
      events.listeners[event] = []
    }
    events.listeners[event].push(callback)
    return this
  },
  $off(event, callback) {
    if (!events.listeners[event]) return this
    if (!callback) {
      events.listeners[event] = []
    } else {
      events.listeners[event] = events.listeners[event].filter(cb => cb !== callback)
    }
    return this
  },
  $emit(event, ...args) {
    if (!events.listeners[event]) return this
    events.listeners[event].forEach(callback => callback(...args))
    return this
  }
}