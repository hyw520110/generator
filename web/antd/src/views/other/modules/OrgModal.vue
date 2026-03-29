<template>
  <a-modal
    title="操作"
    :width="600"
    :open="visible"
    :confirmLoading="confirmLoading"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-spin :spinning="confirmLoading">
      <a-form :model="formState" ref="formRef">

        <a-form-item label="父级ID" name="parentId">
          <a-input v-model:value="formState.parentId" disabled/>
        </a-form-item>

        <a-form-item label="机构名称" name="orgName">
          <a-input v-model:value="formState.orgName"/>
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>
import { ref, reactive, nextTick } from 'vue'
import { message } from 'ant-design-vue'

export default {
  name: 'OrgModal',
  emits: ['close', 'ok'],
  setup (props, { emit }) {
    const formRef = ref()
    const visible = ref(false)
    const confirmLoading = ref(false)
    const mdl = ref({})
    
    const labelCol = { xs: { span: 24 }, sm: { span: 5 } }
    const wrapperCol = { xs: { span: 24 }, sm: { span: 16 } }
    
    const formState = reactive({
      parentId: '',
      orgName: ''
    })
    
    const add = (id) => {
      edit({ parentId: id })
    }
    
    const edit = (record) => {
      mdl.value = Object.assign({}, record)
      visible.value = true
      nextTick(() => {
        formState.parentId = record.parentId
        formState.orgName = record.orgName
      })
    }
    
    const close = () => {
      emit('close')
      visible.value = false
    }
    
    const handleOk = async () => {
      try {
        await formRef.value.validate()
        console.log('form values', formState)
        
        confirmLoading.value = true
        new Promise((resolve) => {
          setTimeout(() => resolve(), 2000)
        }).then(() => {
          message.success('保存成功')
          emit('ok')
        }).catch(() => {
          // Do something
        }).finally(() => {
          confirmLoading.value = false
          close()
        })
      } catch (err) {
        // validation failed
      }
    }
    
    const handleCancel = () => {
      close()
    }
    
    return {
      formRef,
      formState,
      labelCol,
      wrapperCol,
      visible,
      confirmLoading,
      mdl,
      add,
      edit,
      close,
      handleOk,
      handleCancel
    }
  }
}
</script>