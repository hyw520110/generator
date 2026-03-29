<template>
  <a-form @submit="handleSubmit" :model="formState" class="form" ref="formRef">
    <a-row class="form-row" :gutter="16">
      <a-col :lg="6" :md="12" :sm="24">
        <a-form-item label="任务名" name="name2">
          <a-input v-model:value="formState.name2" placeholder="请输入任务名称"/>
        </a-form-item>
      </a-col>
      <a-col :xl="{span: 7, offset: 1}" :lg="{span: 8}" :md="{span: 12}" :sm="24">
        <a-form-item label="任务描述" name="url2">
          <a-input v-model:value="formState.url2" placeholder="请输入任务描述"/>
        </a-form-item>
      </a-col>
      <a-col :xl="{span: 9, offset: 1}" :lg="{span: 10}" :md="{span: 24}" :sm="24">
        <a-form-item label="执行人" name="owner2">
          <a-select v-model:value="formState.owner2" placeholder="请选择执行人">
            <a-select-option value="黄丽丽">黄丽丽</a-select-option>
            <a-select-option value="李大刀">李大刀</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row class="form-row" :gutter="16">
      <a-col :lg="6" :md="12" :sm="24">
        <a-form-item label="责任人" name="approver2">
          <a-select v-model:value="formState.approver2" placeholder="请选择责任人">
            <a-select-option value="王伟">王伟</a-select-option>
            <a-select-option value="李红军">李红军</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
      <a-col :xl="{span: 7, offset: 1}" :lg="{span: 8}" :md="{span: 12}" :sm="24">
        <a-form-item label="提醒时间" name="dateRange2">
          <a-time-picker v-model:value="formState.dateRange2" style="width: 100%"/>
        </a-form-item>
      </a-col>
      <a-col :xl="{span: 9, offset: 1}" :lg="{span: 10}" :md="{span: 24}" :sm="24">
        <a-form-item label="任务类型" name="type2">
          <a-select v-model:value="formState.type2" placeholder="请选择任务类型">
            <a-select-option value="定时执行">定时执行</a-select-option>
            <a-select-option value="周期执行">周期执行</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
    </a-row>
    <a-form-item v-if="showSubmit">
      <a-button htmlType="submit">Submit</a-button>
    </a-form-item>
  </a-form>
</template>

<script>
import { ref, reactive } from 'vue'
import { notification } from 'ant-design-vue'

export default {
  name: 'TaskForm',
  props: {
    showSubmit: {
      type: Boolean,
      default: false
    }
  },
  setup () {
    const formRef = ref()
    
    const formState = reactive({
      name2: '',
      url2: '',
      owner2: undefined,
      approver2: undefined,
      dateRange2: null,
      type2: undefined
    })
    
    const handleSubmit = async (e) => {
      e.preventDefault()
      try {
        await formRef.value.validate()
        notification.success({
          message: 'Received values of form:',
          description: JSON.stringify(formState)
        })
      } catch (error) {
        // validation failed
      }
    }
    
    return {
      formRef,
      formState,
      handleSubmit
    }
  }
}
</script>

<style scoped>

</style>