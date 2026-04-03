<template>
  <page-header-wrapper :title="false" content="表单页用于向用户收集或验证信息，基础表单常见于数据项较少的表单场景。">
    <a-card :body-style="{padding: '24px 32px'}" :bordered="false">
      <a-form @submit="handleSubmit" :model="formState" ref="formRef">
        <a-form-item label="标题" :labelCol="{lg: {span: 7}, sm: {span: 7}}" :wrapperCol="{lg: {span: 10}, sm: {span: 17} }" name="name">
          <a-input v-model:value="formState.name" name="name" placeholder="给目标起个名字"/>
        </a-form-item>
        <a-form-item label="起止日期" :labelCol="{lg: {span: 7}, sm: {span: 7}}" :wrapperCol="{lg: {span: 10}, sm: {span: 17} }" name="buildTime">
          <a-range-picker v-model:value="formState.buildTime" name="buildTime" style="width: 100%"/>
        </a-form-item>
        <a-form-item label="目标描述" :labelCol="{lg: {span: 7}, sm: {span: 7}}" :wrapperCol="{lg: {span: 10}, sm: {span: 17} }" name="description">
          <a-textarea v-model:value="formState.description" rows="4" placeholder="请输入你阶段性工作目标"/>
        </a-form-item>
        <a-form-item label="衡量标准" :labelCol="{lg: {span: 7}, sm: {span: 7}}" :wrapperCol="{lg: {span: 10}, sm: {span: 17} }" name="type">
          <a-textarea v-model:value="formState.type" rows="4" placeholder="请输入衡量标准"/>
        </a-form-item>
        <a-form-item label="客户" :labelCol="{lg: {span: 7}, sm: {span: 7}}" :wrapperCol="{lg: {span: 10}, sm: {span: 17} }" name="customer">
          <a-input v-model:value="formState.customer" placeholder="请描述你服务的客户，内部客户直接 @姓名／工号"/>
        </a-form-item>
        <a-form-item label="邀评人" :labelCol="{lg: {span: 7}, sm: {span: 7}}" :wrapperCol="{lg: {span: 10}, sm: {span: 17} }" :required="false">
          <a-input v-model:value="formState.invite" placeholder="请直接 @姓名／工号，最多可邀请 5 人"/>
        </a-form-item>
        <a-form-item label="权重" :labelCol="{lg: {span: 7}, sm: {span: 7}}" :wrapperCol="{lg: {span: 10}, sm: {span: 17} }" :required="false">
          <a-input-number v-model:value="formState.weight" :min="0" :max="100"/>
          <span> %</span>
        </a-form-item>
        <a-form-item label="目标公开" :labelCol="{lg: {span: 7}, sm: {span: 7}}" :wrapperCol="{lg: {span: 10}, sm: {span: 17} }" :required="false" help="客户、邀评人默认被分享">
          <a-radio-group v-model:value="formState.target">
            <a-radio :value="1">公开</a-radio>
            <a-radio :value="2">部分公开</a-radio>
            <a-radio :value="3">不公开</a-radio>
          </a-radio-group>
          <a-form-item v-show="formState.target === 2">
            <a-select v-model:value="formState.targetUsers" mode="multiple">
              <a-select-option value="4">同事一</a-select-option>
              <a-select-option value="5">同事二</a-select-option>
              <a-select-option value="6">同事三</a-select-option>
            </a-select>
          </a-form-item>
        </a-form-item>
        <a-form-item :wrapperCol="{ span: 24 }" style="text-align: center">
          <a-button htmlType="submit" type="primary">提交</a-button>
          <a-button style="margin-left: 8px">保存</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </page-header-wrapper>
</template>

<script>
import { ref, reactive } from 'vue'

export default {
  name: 'BaseForm',
  setup () {
    const formRef = ref()
    
    const formState = reactive({
      name: '',
      buildTime: null,
      description: '',
      type: '',
      customer: '',
      invite: '',
      weight: 0,
      target: 1,
      targetUsers: []
    })
    
    const handleSubmit = async (e) => {
      e.preventDefault()
      try {
        await formRef.value.validate()
        console.log('Received values of form: ', formState)
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