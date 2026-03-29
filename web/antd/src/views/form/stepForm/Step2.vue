<template>
  <div>
    <a-form :model="formState" style="max-width: 500px; margin: 40px auto 0;" ref="formRef">
      <a-alert
        :closable="true"
        message="确认转账后，资金将直接打入对方账户，无法退回。"
        style="margin-bottom: 24px;"
      />
      <a-form-item label="付款账户" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        ant-design@alipay.com
      </a-form-item>
      <a-form-item label="收款账户" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        test@example.com
      </a-form-item>
      <a-form-item label="收款人姓名" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        Alex
      </a-form-item>
      <a-form-item label="转账金额" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        ￥ 5,000.00
      </a-form-item>
      <a-divider />
      <a-form-item label="支付密码" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText" name="paymentPassword">
        <a-input type="password" style="width: 80%;" v-model:value="formState.paymentPassword"/>
      </a-form-item>
      <a-form-item :wrapperCol="{span: 19, offset: 5}">
        <a-button :loading="loading" type="primary" @click="nextStep">提交</a-button>
        <a-button style="margin-left: 8px" @click="prevStep">上一步</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, onBeforeUnmount } from 'vue'

export default {
  name: 'Step2',
  emits: ['nextStep', 'prevStep'],
  setup (props, { emit }) {
    const formRef = ref()
    const loading = ref(false)
    let timer = null
    
    const labelCol = { lg: { span: 5 }, sm: { span: 5 } }
    const wrapperCol = { lg: { span: 19 }, sm: { span: 19 } }
    
    const formState = reactive({
      paymentPassword: '123456'
    })
    
    const nextStep = async () => {
      loading.value = true
      try {
        await formRef.value.validate()
        console.log('表单 values', formState)
        timer = setTimeout(() => {
          loading.value = false
          emit('nextStep')
        }, 1500)
      } catch (error) {
        loading.value = false
      }
    }
    
    const prevStep = () => {
      emit('prevStep')
    }
    
    onBeforeUnmount(() => {
      if (timer) {
        clearTimeout(timer)
      }
    })
    
    return {
      formRef,
      formState,
      labelCol,
      wrapperCol,
      loading,
      nextStep,
      prevStep
    }
  }
}
</script>

<style lang="less" scoped>
.stepFormText {
  margin-bottom: 24px;

  .ant-form-item-label,
  .ant-form-item-control {
    line-height: 22px;
  }
}
</style>