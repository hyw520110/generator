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
<#list table.fields as field>
<#if !field.isPrimarykey()>
        <a-form-item
          label="${field.comment?default(field.name)}"
          :labelCol="{ span: 5 }"
          :wrapperCol="{ span: 12 }"
        >
          <a-input v-decorator="['${field.propertyName}', { rules: [{ required: <#if field.isNullAble()>false<#else>true</#if>, message: '请输入${field.comment?default(field.name)}' }] }]" />
        </a-form-item>
</#if>
</#list>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>
import { getInfo, add${table.beanName?cap_first}, edit${table.beanName?cap_first} } from '@/api/${table.beanName}'

export default {
  name: '${table.beanName?cap_first}Form',
  data () {
    return {
      title: '操作',
      visible: false,
      confirmLoading: false,
      form: this.$form.createForm(this),
      id: ''
    }
  },
  methods: {
    add () {
      this.title = '新增'
      this.visible = true
      this.form.resetFields()
      this.id = ''
    },
    edit (id) {
      this.title = '编辑'
      this.visible = true
      this.id = id
      this.form.resetFields()
      getInfo(id).then(res => {
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
            if (this.id) {
              edit${table.beanName?cap_first}({ ...values, id: this.id }).then(res => {
                this.$emit('ok')
              })
            } else {
              add${table.beanName?cap_first}(values).then(res => {
                this.$emit('ok')
              })
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