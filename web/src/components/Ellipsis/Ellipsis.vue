<template>
  <a-tooltip v-if="tooltip && fullLength > length">
    <template #title>{{ fullStr }}</template>
    <span>{{ displayStr }}</span>
  </a-tooltip>
  <span v-else>{{ displayStr }}</span>
</template>

<script>
import { cutStrByFullLength, getStrFullLength } from '@/components/_util/util'

export default {
  name: 'Ellipsis',
  props: {
    prefixCls: {
      type: String,
      default: 'ant-pro-ellipsis'
    },
    tooltip: {
      type: Boolean
    },
    length: {
      type: Number,
      required: true
    },
    lines: {
      type: Number,
      default: 1
    },
    fullWidthRecognition: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    fullStr () {
      const slots = this.$slots.default || []
      return slots.map(vNode => vNode.children || vNode.text || '').join('')
    },
    fullLength () {
      return getStrFullLength(this.fullStr)
    },
    displayStr () {
      const str = cutStrByFullLength(this.fullStr, this.length)
      return this.fullLength > this.length ? str + '...' : str
    }
  }
}
</script>