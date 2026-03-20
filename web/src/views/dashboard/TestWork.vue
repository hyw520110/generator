<template>
  <div>
    <h2>本页面内容均为测试功能，暂不提供稳定性保证</h2>
    <a-divider/>
    <div class="multi-tab-test">
      <h4>多标签组件测试功能</h4>
      <a-button @click="handleCloseCurrentTab" style="margin-right: 16px;">关闭当前页</a-button>
      <a-button @click="handleOpenTab" style="margin-right: 16px;">打开 任务列表</a-button>
      <a-popconfirm :open="visible" @confirm="confirm" @cancel="cancel" okText="确定" cancelText="取消">
        <template #title>
          <div>
            <a-form :model="formState" layout="inline">
              <a-form-item label="自定义名称">
                <a-input v-model:value="formState.tabName"/>
              </a-form-item>
            </a-form>
          </div>
        </template>
        <a-button @click="visible = !visible" style="margin-right: 16px;">修改当前 Tab 名称</a-button>
      </a-popconfirm>

      <a-popconfirm :open="visible2" @confirm="confirm2" @cancel="visible2 = false" okText="确定" cancelText="取消">
        <template #title>
          <div>
            <p>页面 KEY 是由页面的路由 <code>path</code> 决定的</p>
            <p>如果要修改某一个页面标题，该页面必须已经被打开在 Tab 栏</p>
            <p>后期可以考虑优化到编程式 Tab 栏，就可以没有这种限制</p>
            <a-form :model="formState2" layout="inline">
              <a-form-item label="页面KEY">
                <a-input v-model:value="formState2.tabKey"/>
              </a-form-item>
              <a-form-item label="自定义名称">
                <a-input v-model:value="formState2.tabName"/>
              </a-form-item>
            </a-form>
          </div>
        </template>
        <a-button @click="visible2 = !visible2">修改某一个 Tab 名称</a-button>
      </a-popconfirm>
    </div>
    <a-divider/>
    <div class="page-loading-test">
      <h4>全局遮罩测试</h4>
      <a-button @click="handleOpenLoading" style="margin-right: 16px;">打开遮罩(5s 自动关闭)</a-button>
      <a-button @click="handleOpenLoadingCustomTip">打开遮罩(自定义提示语)</a-button>
    </div>
  </div>
</template>

<script>
import { ref, reactive, getCurrentInstance } from 'vue'
import { useRoute } from 'vue-router'

export default {
  name: 'TestWork',
  setup () {
    const route = useRoute()
    const instance = getCurrentInstance()
    
    const visible = ref(false)
    const visible2 = ref(false)
    
    const formState = reactive({
      tabName: ''
    })
    
    const formState2 = reactive({
      tabKey: '/dashboard/workplace',
      tabName: ''
    })
    
    const handleCloseCurrentTab = () => {
      // MultiTab functionality would need to be refactored for Vue 3
      console.log('close current tab')
    }
    
    const handleOpenTab = () => {
      console.log('open tab: /features/task')
    }
    
    const handleOpenLoading = () => {
      console.log('show loading')
      setTimeout(() => {
        console.log('hide loading')
      }, 5000)
    }
    
    const handleOpenLoadingCustomTip = () => {
      console.log('show loading with custom tip')
      setTimeout(() => {
        console.log('hide loading')
      }, 5000)
    }
    
    const confirm = (e) => {
      e.stopPropagation()
      if (!formState.tabName) {
        return
      }
      console.log('rename tab:', route.path, formState.tabName)
      visible.value = false
    }
    
    const cancel = () => {
      visible.value = false
    }
    
    const confirm2 = (e) => {
      e.stopPropagation()
      if (!formState2.tabName) {
        return
      }
      console.log('rename tab:', formState2.tabKey, formState2.tabName)
      visible2.value = false
    }
    
    return {
      visible,
      visible2,
      formState,
      formState2,
      handleCloseCurrentTab,
      handleOpenTab,
      handleOpenLoading,
      handleOpenLoadingCustomTip,
      confirm,
      cancel,
      confirm2
    }
  }
}
</script>

<style scoped>

</style>