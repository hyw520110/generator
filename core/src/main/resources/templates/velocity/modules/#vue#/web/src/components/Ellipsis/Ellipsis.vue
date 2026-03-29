<script lang="jsx">
import { defineComponent } from 'vue'
import Tooltip from 'ant-design-vue/es/tooltip'
import { cutStrByFullLength, getStrFullLength } from '@/components/_util/util'

export default defineComponent({
  name: 'Ellipsis',
  components: {
    Tooltip
  },
  props: {
    prefixCls: {
      type: String,
      default: 'ant-pro-ellipsis'
    },
    tooltip: {
      type: Boolean,
      default: false
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
  methods: {
    getStrDom (str, fullLength) {
      const { length } = this
      return (
        <span>{cutStrByFullLength(str, length) + (fullLength > length ? '...' : '')}</span>
      )
    },
    getTooltip (fullStr, fullLength) {
      return (
        <Tooltip>
          {{
            title: () => fullStr,
            default: () => this.getStrDom(fullStr, fullLength)
          }}
        </Tooltip>
      )
    }
  },
  render () {
    const { tooltip, length } = this.$props
    const slots = this.$slots.default?.() || []
    const str = slots
      .map(vNode => {
        if (typeof vNode.children === 'string') {
          return vNode.children
        }
        return ''
      })
      .join('')
    const fullLength = getStrFullLength(str)
    const strDom = tooltip && fullLength > length ? this.getTooltip(str, fullLength) : this.getStrDom(str, fullLength)
    return strDom
  }
})
</script>