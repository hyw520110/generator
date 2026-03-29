<template>
  <div>
    <a-form :model="formState" @submit="handleSubmit" ref="formRef">

      <a-form-item
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        label="规则编号"
        hasFeedback
        validateStatus="success"
        name="no"
      >
        <a-input
          placeholder="规则编号"
          v-model:value="formState.no"
          :disabled="true"
        />
      </a-form-item>

      <a-form-item
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        label="服务调用次数"
        hasFeedback
        validateStatus="success"
        name="callNo"
      >
        <a-input-number :min="1" style="width: 100%" v-model:value="formState.callNo"/>
      </a-form-item>

      <a-form-item
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        label="状态"
        hasFeedback
        validateStatus="warning"
        name="status"
      >
        <a-select v-model:value="formState.status">
          <a-select-option :value="1">Option 1</a-select-option>
          <a-select-option :value="2">Option 2</a-select-option>
          <a-select-option :value="3">Option 3</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        label="描述"
        hasFeedback
        help="请填写一段描述"
        name="description"
      >
        <a-textarea :rows="5" placeholder="..." v-model:value="formState.description"/>
      </a-form-item>

      <a-form-item
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        label="更新时间"
        hasFeedback
        validateStatus="error"
        name="updatedAt"
      >
        <a-date-picker
          style="width: 100%"
          showTime
          format="YYYY-MM-DD HH:mm:ss"
          placeholder="Select Time"
          v-model:value="formState.updatedAt"
        />
      </a-form-item>

      <a-form-item v-bind="buttonCol">
        <a-row>
          <a-col span="6">
            <a-button type="primary" html-type="submit">提交</a-button>
          </a-col>
          <a-col span="10">
            <a-button @click="handleGoBack">返回</a-button>
          </a-col>
          <a-col span="8"></a-col>
        </a-row>
      </a-form-item>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue'
import moment from 'moment'

export default {
  name: 'TableEdit',
  props: {
    record: {
      type: [Object, String],
      default: ''
    }
  },
  emits: ['onGoBack'],
  setup (props, { emit }) {
    const formRef = ref()
    
    const labelCol = {
      xs: { span: 24 },
      sm: { span: 5 }
    }
    const wrapperCol = {
      xs: { span: 24 },
      sm: { span: 12 }
    }
    const buttonCol = {
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 12, offset: 5 }
      }
    }
    
    const formState = reactive({
      no: '',
      callNo: 1,
      status: 1,
      description: '',
      updatedAt: null
    })
    
    const handleGoBack = () => {
      emit('onGoBack')
    }
    
    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        console.log('Received values of form: ', formState)
      } catch (err) {
        // validation failed
      }
    }
    
    const loadEditInfo = (data) => {
      console.log(`将加载信息到表单`)
      new Promise((resolve) => {
        setTimeout(resolve, 1500)
      }).then(() => {
        formState.no = data.no
        formState.callNo = data.callNo
        formState.status = data.status
        formState.description = data.description
        formState.updatedAt = moment(data.updatedAt)
        console.log('formData', formState)
      })
    }
    
    onMounted(() => {
      nextTick(() => {
        if (props.record) {
          loadEditInfo(props.record)
        }
      })
    })
    
    return {
      formRef,
      formState,
      labelCol,
      wrapperCol,
      buttonCol,
      handleGoBack,
      handleSubmit
    }
  }
}
</script>