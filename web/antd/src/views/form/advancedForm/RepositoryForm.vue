<template>
  <a-form @submit="handleSubmit" :model="formState" class="form" ref="formRef">
    <a-row class="form-row" :gutter="16">
      <a-col :lg="6" :md="12" :sm="24">
        <a-form-item label="仓库名" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入仓库名称"/>
        </a-form-item>
      </a-col>
      <a-col :xl="{span: 7, offset: 1}" :lg="{span: 8}" :md="{span: 12}" :sm="24">
        <a-form-item label="仓库域名" name="url">
          <a-input v-model:value="formState.url" addonBefore="http://" addonAfter=".com" placeholder="请输入"/>
        </a-form-item>
      </a-col>
      <a-col :xl="{span: 9, offset: 1}" :lg="{span: 10}" :md="{span: 24}" :sm="24">
        <a-form-item label="仓库管理员" name="owner">
          <a-select v-model:value="formState.owner" placeholder="请选择管理员">
            <a-select-option value="王同学">王同学</a-select-option>
            <a-select-option value="李同学">李同学</a-select-option>
            <a-select-option value="黄同学">黄同学</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row class="form-row" :gutter="16">
      <a-col :lg="6" :md="12" :sm="24">
        <a-form-item label="审批人" name="approver">
          <a-select v-model:value="formState.approver" placeholder="请选择审批员">
            <a-select-option value="王晓丽">王晓丽</a-select-option>
            <a-select-option value="李军">李军</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
      <a-col :xl="{span: 7, offset: 1}" :lg="{span: 8}" :md="{span: 12}" :sm="24">
        <a-form-item label="生效日期" name="dateRange">
          <a-range-picker v-model:value="formState.dateRange" style="width: 100%"/>
        </a-form-item>
      </a-col>
      <a-col :xl="{span: 9, offset: 1}" :lg="{span: 10}" :md="{span: 24}" :sm="24">
        <a-form-item label="仓库类型" name="type">
          <a-select v-model:value="formState.type" placeholder="请选择仓库类型">
            <a-select-option value="公开">公开</a-select-option>
            <a-select-option value="私密">私密</a-select-option>
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
  name: 'RepositoryForm',
  props: {
    showSubmit: {
      type: Boolean,
      default: false
    }
  },
  setup () {
    const formRef = ref()
    
    const formState = reactive({
      name: '',
      url: '',
      owner: undefined,
      approver: undefined,
      dateRange: null,
      type: undefined
    })
    
    const handleSubmit = async (e) => {
      e.preventDefault()
      try {
        await formRef.value.validate()
        // Validate URL format
        const regex = /^user-(.*)$/
        if (!regex.test(formState.url)) {
          notification.error({
            message: '验证错误',
            description: '需要以 user- 开头'
          })
          return
        }
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