<template>
  <a-modal
    :title="title"
    :width="640"
    :visible="visible"
    :confirmLoading="confirmLoading"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-form-item>#foreach($field in ${table.primarykeyFields})<a-input type="hidden" v-decorator="['${field.propertyName}', {rules: [{required: false}]}]" />#end</a-form-item>
#foreach($field in ${table.fields})
#if(!${field.isPrimarykey()})
        <a-form-item
          label="#if(""!="${field.comment}")${field.comment}#else${field.propertyName}#end"
          :labelCol="labelCol"
          :wrapperCol="wrapperCol">
          <a-input v-decorator="['${field.propertyName}', {rules: [{required:#if(${field.nullAble}) false#else true#end, message: '请输入'}]}]" />
        </a-form-item>
#end
#end
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>

import AFormItem from 'ant-design-vue/es/form/FormItem'
import { getInfo } from '@/api/${table.beanName}'

export default {
  components: { AFormItem },
  data () {
    return {
      labelCol: {
        xs: { span: 12 },
        sm: { span: 7 }
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 13 }
      },
      visible: false,
      confirmLoading: false,
      title: ''
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    add () {
      this.title = '新建#if(${table.comment})${table.comment}#end'
      this.visible = true
    },
    edit (record) {
      this.title = '编辑#if(${table.comment})${table.comment}#end信息'
      this.visible = true

      getInfo(#if(""=="${table.getPrimarykeyFieldsNames()}")record.id#else#foreach($field in ${table.primarykeyFields})record.${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()})+","#end#end#end).then(res => {
        const data = res.data
        this.form.setFieldsValue({ ...data })
      })
    },
    handleSubmit () {
      const { form: { validateFields } } = this
      this.confirmLoading = true
      validateFields((errors, values) => {
        if (!errors) {
          console.log('values', values)
          setTimeout(() => {
            this.visible = false
            this.confirmLoading = false
            if (values.postId) {
              this.$emit('edit', values)
            } else {
              this.$emit('add', values)
            }
            this.form.resetFields()
          }, 500)
        } else {
          this.confirmLoading = false
        }
      })
    },
    handleCancel () {
      this.visible = false
      this.form.resetFields()
    }
  }
}
</script>
