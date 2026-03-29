<template>
  <a-popover
    v-model:open="visible"
    trigger="click"
    placement="bottomRight"
    overlayClassName="header-notice-wrapper"
    :getPopupContainer="() => noticeRef?.parentElement"
    :autoAdjustOverflow="true"
    :arrowPointAtCenter="true"
    :overlayStyle="{ width: '300px', top: '50px' }"
  >
    <template #content>
      <a-spin :spinning="loadding">
        <a-tabs>
          <a-tab-pane tab="通知" key="1">
            <a-list>
              <a-list-item>
                <a-list-item-meta title="你收到了2个新消息" description="通知提示">
                  <template #avatar>
                    <a-avatar style="background-color: white" src="https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=320178652,790985626&fm=26&gp=0.jpg"/>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </a-list>
          </a-tab-pane>
          <a-tab-pane tab="消息" key="2">
            消息提示
          </a-tab-pane>
          <a-tab-pane tab="待办" key="3">
            待办事项
          </a-tab-pane>
        </a-tabs>
      </a-spin>
    </template>
    <span @click="fetchNotice" class="header-notice" ref="noticeRef">
      <a-badge count="2">
        <BellOutlined style="font-size: 16px; padding: 4px" />
      </a-badge>
    </span>
  </a-popover>
</template>

<script>
import { BellOutlined } from '@ant-design/icons-vue'
import { ref } from 'vue'

export default {
  name: 'HeaderNotice',
  components: {
    BellOutlined
  },
  setup () {
    const loadding = ref(false)
    const visible = ref(false)
    const noticeRef = ref(null)

    const fetchNotice = () => {
      if (!visible.value) {
        loadding.value = true
        setTimeout(() => {
          loadding.value = false
        }, 2000)
      } else {
        loadding.value = false
      }
      visible.value = !visible.value
    }

    return {
      loadding,
      visible,
      noticeRef,
      fetchNotice
    }
  }
}
</script>

<style lang="css">
  .header-notice-wrapper {
    top: 50px !important;
  }
</style>
<style lang="less" scoped>
  .header-notice{
    display: inline-block;
    transition: all 0.3s;

    span {
      vertical-align: initial;
    }
  }
</style>
