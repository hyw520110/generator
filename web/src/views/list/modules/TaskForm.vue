<template>
  <a-form @submit="handleSubmit" :model="formState" ref="formRef">
    <a-form-item label="任务名称" :labelCol="labelCol" :wrapperCol="wrapperCol" name="title">
      <a-input v-model:value="formState.title"/>
    </a-form-item>
    <a-form-item label="开始时间" :labelCol="labelCol" :wrapperCol="wrapperCol" name="startAt">
      <a-date-picker v-model:value="formState.startAt" style="width: 100%" valueFormat="YYYY-MM-DD HH:mm"/>
    </a-form-item>
    <a-form-item label="任务负责人" :labelCol="labelCol" :wrapperCol="wrapperCol" name="owner">
      <a-select v-model:value="formState.owner">
        <a-select-option :value="0">付晓晓</a-select-option>
        <a-select-option :value="1">周毛毛</a-select-option>
      </a-select>
    </a-form-item>
    <a-form-item label="产品描述" :labelCol="labelCol" :wrapperCol="wrapperCol" name="description">
      <a-textarea v-model:value="formState.description"/>
    </a-form-item>
  </a-form>
</template>

<script>
import { ref, reactive, onMounted, watch } from 'vue'

const fields = ['title', 'startAt', 'owner', 'description']

export default {
  name: 'TaskForm',
  props: {
    record: {
      type: Object,
      default: null
    }
  },
  setup (props) {
    const formRef = ref()
    
    const labelCol = {
      xs: { span: 24 },
      sm: { span: 7 }
    }
    const wrapperCol = {
      xs: { span: 24 },
      sm: { span: 13 }
    }
    
    const formState = reactive({
      title: '',
      startAt: null,
      owner: undefined,
      description: ''
    })
    
    onMounted(() => {
      if (props.record) {
        fields.forEach(field => {
          if (props.record[field] !== undefined) {
            formState[field] = props.record[field]
          }
        })
      }
    })
    
    watch(() => props.record, (newRecord) => {
      if (newRecord) {
        fields.forEach(field => {
          if (newRecord[field] !== undefined) {
            formState[field] = newRecord[field]
          }
        })
      }
    })
    
    const onOk = () => {
      console.log('监听了 modal ok 事件')
      return Promise.resolve(true)
    }
    
    const onCancel = () => {
      console.log('监听了 modal cancel 事件')
      return Promise.resolve(true)
    }
    
    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        console.log('values', formState)
      } catch (errors) {
        // validation failed
      }
    }
    
    return {
      formRef,
      formState,
      labelCol,
      wrapperCol,
      onOk,
      onCancel,
      handleSubmit
    }
  }
}
</script>