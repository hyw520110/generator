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
#foreach($field in $table.fields)
#if(!$field.isPrimarykey())
        <a-form-item
          label="#if("${field.comment}"=="")${field.name}#else${field.comment}#end"
          name="${field.propertyName}"
        >
#if($field.vueSwitchControl)
          <a-switch v-model:checked="formState.${field.propertyName}" />
#elseif($field.vueDateControl)
          <a-date-picker v-model:value="formState.${field.propertyName}" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
#elseif($field.vueNumberControl)
          <a-input-number v-model:value="formState.${field.propertyName}" style="width: 100%" placeholder="请输入#if("${field.comment}"=="")${field.name}#else${field.comment}#end" />
#elseif($field.vueTextareaControl)
          <a-textarea v-model:value="formState.${field.propertyName}" :rows="4" placeholder="请输入#if("${field.comment}"=="")${field.name}#else${field.comment}#end" />
#else
          <a-input v-model:value="formState.${field.propertyName}" placeholder="请输入#if("${field.comment}"=="")${field.name}#else${field.comment}#end" />
#end
        </a-form-item>
#end
#end
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>
import { ref, reactive } from 'vue'
import { getInfo, add${table.beanName}, edit${table.beanName} } from '@/api/${table.beanName}'

export default {
  name: '${table.beanName}Form',
  emits: ['ok'],
  setup (props, { emit }) {
    const title = ref('操作')
    const visible = ref(false)
    const confirmLoading = ref(false)
    const formRef = ref()
    const currentRecord = ref({})
    const primaryKeyFields = ${table.primaryKeyJsArray}
    const hasPrimaryKey = primaryKeyFields.length > 0

    const formState = reactive({
#foreach($field in $table.fields)
      ${field.propertyName}: ${field.vueInitialValue}#if($foreach.hasNext),#end
#end
    })

    const formRules = {
#foreach($field in $table.fields)
#if(!$field.isPrimarykey())
      ${field.propertyName}: [{ required: #if($field.isNullAble())false#else true#end, message: '请输入#if("${field.comment}"=="")${field.name}#else${field.comment}#end', trigger: 'blur' }]#if($foreach.hasNext),#end
#end
#end
    }

    const resetForm = () => {
#foreach($field in $table.fields)
      formState.${field.propertyName} = ${field.vueInitialValue}
#end
    }

    const add = () => {
      title.value = '新增'
      visible.value = true
      currentRecord.value = {}
      resetForm()
    }

    const edit = (record) => {
      if (!hasPrimaryKey) {
        return
      }
      title.value = '编辑'
      visible.value = true
      currentRecord.value = record || {}
      resetForm()
      getInfo(record).then(res => {
        Object.assign(formState, res.data)
      })
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
          await edit${table.beanName}({ ...formState, ...primaryKeyPayload })
        } else {
          await add${table.beanName}(formState)
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
