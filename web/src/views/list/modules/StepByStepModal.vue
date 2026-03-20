<template>
  <a-modal
    title="分步对话框"
    :width="640"
    :open="visible"
    :confirmLoading="confirmLoading"
    @cancel="handleCancel"
  >
    <a-spin :spinning="confirmLoading">
      <a-steps :current="currentStep" :style="{ marginBottom: '28px' }" size="small">
        <a-step title="基本信息"/>
        <a-step title="配置规则属性"/>
        <a-step title="设定调度周期"/>
      </a-steps>
      <a-form :model="formState" ref="formRef">
        <!-- step1 -->
        <div v-show="currentStep === 0">
          <a-form-item label="规则名称" :labelCol="labelCol" :wrapperCol="wrapperCol" name="name">
            <a-input v-model:value="formState.name"/>
          </a-form-item>
          <a-form-item label="规则描述" :labelCol="labelCol" :wrapperCol="wrapperCol" name="desc">
            <a-textarea :rows="4" v-model:value="formState.desc"/>
          </a-form-item>
        </div>
        <div v-show="currentStep === 1">
          <a-form-item label="监控对象" :labelCol="labelCol" :wrapperCol="wrapperCol" name="target">
            <a-select v-model:value="formState.target" style="width: 100%">
              <a-select-option :value="0">表一</a-select-option>
              <a-select-option :value="1">表二</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="规则模板" :labelCol="labelCol" :wrapperCol="wrapperCol" name="template">
            <a-select v-model:value="formState.template" style="width: 100%">
              <a-select-option :value="0">规则模板一</a-select-option>
              <a-select-option :value="1">规则模板二</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="规则类型" :labelCol="labelCol" :wrapperCol="wrapperCol" name="type">
            <a-radio-group v-model:value="formState.type" style="width: 100%">
              <a-radio :value="0">强</a-radio>
              <a-radio :value="1">弱</a-radio>
            </a-radio-group>
          </a-form-item>
        </div>
        <div v-show="currentStep === 2">
          <a-form-item label="开始时间" :labelCol="labelCol" :wrapperCol="wrapperCol" name="time">
            <a-date-picker v-model:value="formState.time" style="width: 100%"/>
          </a-form-item>
          <a-form-item label="调度周期" :labelCol="labelCol" :wrapperCol="wrapperCol" name="frequency">
            <a-select v-model:value="formState.frequency" style="width: 100%">
              <a-select-option value="month">月</a-select-option>
              <a-select-option value="week">周</a-select-option>
            </a-select>
          </a-form-item>
        </div>
      </a-form>
    </a-spin>
    <template #footer>
      <a-button key="back" @click="backward" v-if="currentStep > 0" :style="{ float: 'left' }">上一步</a-button>
      <a-button key="cancel" @click="handleCancel">取消</a-button>
      <a-button key="forward" :loading="confirmLoading" type="primary" @click="handleNext(currentStep)">{{ currentStep === 2 ? '完成' : '下一步' }}</a-button>
    </template>
  </a-modal>
</template>

<script>
import { ref, reactive } from 'vue'

export default {
  name: 'StepByStepModal',
  emits: ['ok'],
  setup (props, { emit }) {
    const formRef = ref()
    
    const labelCol = {
      xs: { span: 24 },
      sm: { span: 7 }
    }
    const wrapperCol = {
      xs: { span: 24 },
      sm: { span: 13 }
    }
    
    const visible = ref(false)
    const confirmLoading = ref(false)
    const currentStep = ref(0)
    
    const formState = reactive({
      name: '',
      desc: '',
      target: 0,
      template: 0,
      type: 0,
      time: null,
      frequency: 'month'
    })
    
    const edit = (record) => {
      visible.value = true
      if (record) {
        Object.assign(formState, record)
      }
    }
    
    const handleNext = async (step) => {
      const nextStep = step + 1
      if (nextStep <= 2) {
        currentStep.value = nextStep
        return
      }
      // last step
      confirmLoading.value = true
      setTimeout(() => {
        confirmLoading.value = false
        emit('ok', { ...formState })
      }, 1500)
    }
    
    const backward = () => {
      currentStep.value--
    }
    
    const handleCancel = () => {
      visible.value = false
      currentStep.value = 0
    }
    
    return {
      formRef,
      formState,
      labelCol,
      wrapperCol,
      visible,
      confirmLoading,
      currentStep,
      edit,
      handleNext,
      backward,
      handleCancel
    }
  }
}
</script>