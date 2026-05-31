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
<#assign javaType = field.fieldType.type?lower_case>
<#assign dbType = field.type?lower_case>
<#if javaType == "boolean" || javaType == "java.lang.boolean">
          <a-switch v-model:checked="formState.${field.propertyName}" />
<#elseif javaType?contains("date") || javaType?contains("time")>
          <a-date-picker v-model:value="formState.${field.propertyName}" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
<#elseif javaType == "integer" || javaType == "int" || javaType == "long" || javaType == "java.lang.long" || javaType == "short" || javaType == "double" || javaType == "float" || javaType == "bigdecimal">
          <a-input-number v-model:value="formState.${field.propertyName}" style="width: 100%" placeholder="请输入${field.comment?default(field.name)}" />
<#elseif dbType?contains("text") || dbType?contains("clob") || field.comment?default("")?length gt 20>
          <a-textarea v-model:value="formState.${field.propertyName}" :rows="4" placeholder="请输入${field.comment?default(field.name)}" />
<#else>
          <a-input v-model:value="formState.${field.propertyName}" placeholder="请输入${field.comment?default(field.name)}" />
</#if>
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
    const currentRecord = ref({})
    const primaryKeyFields = ${table.primaryKeyJsArray}

    const formState = reactive({
<#list table.fields as field>
<#assign javaType = field.fieldType.type?lower_case>
      ${field.propertyName}: <#if javaType == "boolean" || javaType == "java.lang.boolean">false<#elseif javaType == "integer" || javaType == "int" || javaType == "long" || javaType == "java.lang.long" || javaType == "short" || javaType == "double" || javaType == "float" || javaType == "bigdecimal">null<#else>''</#if><#if field?has_next>,</#if>
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
      currentRecord.value = {}
      resetForm()
    }

    const edit = (record) => {
      title.value = '编辑'
      visible.value = true
      currentRecord.value = record || {}
      resetForm()
      getInfo(record).then(res => {
        const data = res.data
        Object.assign(formState, data)
      })
    }

    const resetForm = () => {
<#list table.fields as field>
<#assign javaType = field.fieldType.type?lower_case>
      formState.${field.propertyName} = <#if javaType == "boolean" || javaType == "java.lang.boolean">false<#elseif javaType == "integer" || javaType == "int" || javaType == "long" || javaType == "java.lang.long" || javaType == "short" || javaType == "double" || javaType == "float" || javaType == "bigdecimal">null<#else>''</#if>
</#list>
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        confirmLoading.value = true
        const primaryKeyPayload = primaryKeyFields.reduce((payload, key) => {
          if (currentRecord.value[key] !== undefined) {
            payload[key] = currentRecord.value[key]
          }
          return payload
        }, {})
        if (Object.keys(primaryKeyPayload).length > 0) {
          await edit${table.beanName?cap_first}({ ...formState, ...primaryKeyPayload })
        } else {
          await add${table.beanName?cap_first}(formState)
        }
        visible.value = false
        emit('ok')
        resetForm()
      } catch (error) {
        // 表单校验失败或接口错误由全局请求拦截器提示
      } finally {
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
      currentRecord,
      add,
      edit,
      handleSubmit,
      handleCancel
    }
  }
}
</script>
