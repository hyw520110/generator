<script lang="jsx">
import { defineComponent } from 'vue'
import Avatar from 'ant-design-vue/es/avatar'
import AvatarItem from './Item'
import { filterEmpty } from '@/components/_util/util'

export default defineComponent({
  name: 'AvatarList',
  components: {
    Avatar,
    AvatarItem
  },
  props: {
    prefixCls: {
      type: String,
      default: 'ant-pro-avatar-list'
    },
    size: {
      type: [String, Number],
      default: 'default'
    },
    maxLength: {
      type: Number,
      default: 0
    },
    excessItemsStyle: {
      type: Object,
      default: () => ({
        color: '#f56a00',
        backgroundColor: '#fde3cf'
      })
    }
  },
  methods: {
    getItems (items) {
      const { prefixCls, size, maxLength, excessItemsStyle } = this
      const classString = {
        [`${prefixCls}-item`]: true,
        [`${size}`]: true
      }

      let displayItems = items
      if (maxLength > 0) {
        displayItems = items.slice(0, maxLength)
        displayItems.push(<Avatar size={size} style={excessItemsStyle}>{`+${maxLength}`}</Avatar>)
      }
      return displayItems.map((item) => (
        <li class={classString}>{item}</li>
      ))
    }
  },
  render () {
    const { prefixCls, size } = this.$props
    const classString = {
      [`${prefixCls}`]: true,
      [`${size}`]: true
    }
    const items = filterEmpty(this.$slots.default?.())
    const itemsDom = items && items.length ? <ul class={`${prefixCls}-items`}>{this.getItems(items)}</ul> : null

    return (
      <div class={classString}>
        {itemsDom}
      </div>
    )
  }
})

AvatarItem.install = function (Vue) {
  Vue.component(AvatarItem.name, AvatarItem)
}

export { AvatarItem }
</script>

<style lang="less" scoped>
.ant-pro-avatar-list {
  display: inline-block;
  
  &-items {
    display: inline-block;
    margin: 0;
    padding: 0;
    list-style: none;
  }
  
  &-item {
    display: inline-block;
    margin-left: -8px;
    
    &:first-child {
      margin-left: 0;
    }
  }
  
  &.large {
    .ant-pro-avatar-list-item {
      margin-left: -12px;
    }
  }
  
  &.small {
    .ant-pro-avatar-list-item {
      margin-left: -6px;
    }
  }
  
  &.mini {
    .ant-pro-avatar-list-item {
      margin-left: -4px;
    }
  }
}
</style>