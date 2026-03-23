<template>
  <div class="header-info">
    <span>{{ title }}</span>
    <p>{{ value }}</p>
    <em v-if="bordered" />
  </div>
</template>

<script>
export default {
  name: 'Info',
  props: {
    title: {
      type: String,
      default: ''
    },
    value: {
      type: String,
      default: ''
    },
    bordered: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style lang="less" scoped>
// Ant Design Vue 4.x 使用 CSS-in-JS，定义需要的变量
@text-color-secondary: rgba(0, 0, 0, 0.45);
@heading-color: rgba(0, 0, 0, 0.85);
@font-size-base: 14px;
@border-color-split: rgba(0, 0, 0, 0.06);

.header-info {
  position: relative;
  text-align: center;
  & > span {
    display: inline-block;
    margin-bottom: 4px;
    color: @text-color-secondary;
    font-size: @font-size-base;
    line-height: 22px;
  }
  & > p {
    margin: 0;
    color: @heading-color;
    font-size: 24px;
    line-height: 32px;
  }
  & > em {
    position: absolute;
    top: 0;
    right: 0;
    width: 1px;
    height: 56px;
    background-color: @border-color-split;
  }
}
</style>
