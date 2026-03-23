<template>
  <a-modal
    :title="title"
    :width="640"
    :open="visible"
    :confirmLoading="confirmLoading"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-spin :spinning="confirmLoading">
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
<#list table.fields as field>
<#if !field.isPrimarykey()>
        <a-form-item
          label="${field.comment?default(field.name)}"
          name="${field.propertyName}"
        >
          <a-input v-model:value="formState.${field.propertyName}" placeholder="请输入${field.comment?default(field.name)}" />
        </a-form-item>
</#if>
</#list>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>
import { ref, reactive } from 'vue'
import { getInfo, add${table.beanName?cap_first}, edit${table.beanName?cap_first} } from '@/api/${table.beanName}'

export default {
  name: '${table.beanName?cap_first}Form',
  emits: ['ok'],
  setup (props, { emit }) {
    const title = ref('操作')
    const visible = ref(false)
    const confirmLoading = ref(false)
    const formRef = ref()
    const id = ref('')

    const formState = reactive({
<#list table.fields as field>
      ${field.propertyName}: ''<#if field?has_next>,</#if>
</#list>
    })

    const formRules = {
<#list table.fields as field>
<#if !field.isPrimarykey()>
      ${field.propertyName}: [{ required: <#if field.isNullAble()>false<#else>true</#if>, message: '请输入${field.comment?default(field.name)}', trigger: 'blur' }]<#if field?has_next>,</#if>
</#if>
</#list>
    }

    const add = () => {
      title.value = '新增'
      visible.value = true
      id.value = ''
      resetForm()
    }

    const edit = (recordId) => {
      title.value = '编辑'
      visible.value = true
      id.value = recordId
      resetForm()
      getInfo(recordId).then(res => {
        const data = res.data
        Object.assign(formState, data)
      })
    }

    const resetForm = () => {
<#list table.fields as field>
      formState.${field.propertyName} = ''
</#list>
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        confirmLoading.value = true
        setTimeout(() => {
          visible.value = false
          confirmLoading.value = false
          if (id.value) {
            edit${table.beanName?cap_first}({ ...formState, id: id.value }).then(() => {
              emit('ok')
            })
          } else {
            add${table.beanName?cap_first}(formState).then(() => {
              emit('ok')
            })
          }
          resetForm()
        }, 500)
      } catch (error) {
        confirmLoading.value = false
      }
    }

    const handleCancel = () => {
      visible.value = false
      resetForm()
    }

    return {
      title,
      visible,
      confirmLoading,
      formRef,
      formState,
      formRules,
      id,
      add,
      edit,
      handleSubmit,
      handleCancel
    }
  }
}
</script>
