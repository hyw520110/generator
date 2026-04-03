<template>
  <a-modal
    title="新建规则"
    :width="640"
    :open="visible"
    :confirmLoading="loading"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-spin :spinning="loading">
      <a-form :model="formState" v-bind="formLayout" ref="formRef">
        <a-form-item v-show="model && model.id > 0" label="主键ID" name="id">
          <a-input v-model:value="formState.id" disabled/>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-input v-model:value="formState.description"/>
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>
import { ref, reactive, watch } from 'vue'

// 表单字段
const fields = ['description', 'id']

export default {
  props: {
    visible: {
      type: Boolean,
      required: true
    },
    loading: {
      type: Boolean,
      default: () => false
    },
    model: {
      type: Object,
      default: () => null
    }
  },
  emits: ['ok', 'cancel'],
  setup (props, { emit }) {
    const formRef = ref()
    
    const formLayout = {
      labelCol: {
        xs: { span: 24 },
        sm: { span: 7 }
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 13 }
      }
    }
    
    const formState = reactive({
      id: 0,
      description: ''
    })
    
    watch(() => props.model, (newModel) => {
      if (newModel) {
        fields.forEach(field => {
          if (newModel[field] !== undefined) {
            formState[field] = newModel[field]
          }
        })
      }
    }, { immediate: true })
    
    const handleOk = () => {
      emit('ok', { ...formState })
    }
    
    const handleCancel = () => {
      emit('cancel')
    }
    
    return {
      formRef,
      formState,
      formLayout,
      handleOk,
      handleCancel
    }
  }
}
</script>