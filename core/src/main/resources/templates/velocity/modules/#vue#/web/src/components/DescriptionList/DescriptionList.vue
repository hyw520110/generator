<template>
  <div :class="['description-list', size, layout === 'vertical' ? 'vertical': 'horizontal']">
    <div v-if="title" class="title">{{ title }}</div>
    <a-row>
      <slot></slot>
    </a-row>
  </div>
</template>

<script lang="jsx">
import { defineComponent, inject, provide } from 'vue'
import { Col } from 'ant-design-vue/es/grid/'

const responsive = {
  1: { xs: 24 },
  2: { xs: 24, sm: 12 },
  3: { xs: 24, sm: 12, md: 8 },
  4: { xs: 24, sm: 12, md: 6 }
}

const Item = defineComponent({
  name: 'DetailListItem',
  props: {
    term: {
      type: String,
      default: '',
      required: false
    }
  },
  setup () {
    const col = inject('col', 3)
    return { col }
  },
  render () {
    const colProps = responsive[this.col] || responsive[3]
    return (
      <Col {...colProps}>
        <div class="term">{this.term}</div>
        <div class="content">{this.$slots.default?.()}</div>
      </Col>
    )
  }
})

export default defineComponent({
  name: 'DetailList',
  components: {
    Col,
    Item
  },
  props: {
    title: {
      type: String,
      default: '',
      required: false
    },
    col: {
      type: Number,
      required: false,
      default: 3
    },
    size: {
      type: String,
      required: false,
      default: 'large'
    },
    layout: {
      type: String,
      required: false,
      default: 'horizontal'
    }
  },
  setup (props) {
    provide('col', props.col > 4 ? 4 : props.col)
  }
})

export { Item }
</script>

<style lang="less" scoped>
.description-list {
  .title {
    color: rgba(0,0,0,.85);
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 16px;
  }

  :deep(.term) {
    color: rgba(0,0,0,.85);
    display: table-cell;
    line-height: 20px;
    margin-right: 8px;
    padding-bottom: 16px;
    white-space: nowrap;

    &:not(:empty):after {
      content: ":";
      margin: 0 8px 0 2px;
      position: relative;
      top: -.5px;
    }
  }

  :deep(.content) {
    color: rgba(0,0,0,.65);
    display: table-cell;
    min-height: 22px;
    line-height: 22px;
    padding-bottom: 16px;
    width: 100%;
    &:empty {
      content: ' ';
      height: 38px;
      padding-bottom: 16px;
    }
  }

  &.small {
    .title {
      font-size: 14px;
      color: rgba(0, 0, 0, .65);
      font-weight: normal;
      margin-bottom: 12px;
    }
    :deep(.term), :deep(.content) {
      padding-bottom: 8px;
    }
  }

  &.large {
    :deep(.term), :deep(.content) {
      padding-bottom: 16px;
    }

    .title {
      font-size: 16px;
    }
  }

  &.vertical {
    .term {
      padding-bottom: 8px;
    }
    :deep(.term), :deep(.content) {
      display: block;
    }
  }
}
</style>