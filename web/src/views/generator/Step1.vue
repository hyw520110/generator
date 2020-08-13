<template>
  <div>
    <a-form :form="form" style="max-width: 800px; margin: 40px auto 0;" >
      <a-form-item
        label="生成目录"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
      >
        <a-input v-decorator="['outputDir', { initialValue: '/output/demo', rules: [{required: true, message: '请输入生成目录，此目录需可写，最后一级子目录为项目名'}] }]" placeholder="此目录需可写，最后一级子目录为项目名"/>
      </a-form-item>
      <a-form-item
        label="项目描述"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
      >
        <a-input v-decorator="['description', { initialValue: '代码生成器', rules: [{required: false, message: '请输入项目描述'}] }]" placeholder="项目描述"/>
      </a-form-item>
      <a-form-item
        label="包名"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
      >
        <a-input v-decorator="['rootPackage', { initialValue: 'com.hyw.generator', rules: [{required: true, message: '请输入包名'}] }]" placeholder="com.big.box"/>
      </a-form-item>
      <a-form-item
        label="工程模块"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
      >
        <a-input v-decorator="['modules', { initialValue: 'api,app', rules: [{required: true, message: '请输入工程模块名'}] }]" placeholder="工程模块名，如：api,app"/>
      </a-form-item>
      <a-form-item
        label="删除生成目录"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
      >
        <ASwitch v-decorator="['delOutputDir', { valuePropName:'checked', initialValue:false }]" checkedChildren="是" unCheckedChildren="否"/>
      </a-form-item>
      <a-form-item
        label="文件覆盖"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
      >
        <ASwitch v-decorator="['fileOverride', { valuePropName:'checked', initialValue:true }]" checkedChildren="是" unCheckedChildren="否" />
      </a-form-item>
      <a-form-item
        label="打开目录"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
      >
        <ASwitch v-decorator="['openDir', { valuePropName:'checked', initialValue:true }]" checkedChildren="是" unCheckedChildren="否" />
      </a-form-item>
      <a-form-item :wrapperCol="{span: 19, offset: 5}">
        <a-button :loading="loading" type="primary" @click="nextStep">下一步</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <div class="step-form-style-desc">
      <h3>说明</h3>
      <h4>生成目录</h4>
      <p>此目录需要有写权限，最后一级子目录为项目名</p>
      <h4>删除生成目录</h4>
      <p>执行代码生成前，先删除生成目录，默认关闭，开启时需谨慎!</p>
      <h4>文件覆盖</h4>
      <p>生成同名文件时，是否覆盖原文件/强制生成</p>
    </div>
  </div>
</template>

<script>
import { step1 } from '@/api/generator'
export default {
  name: 'Step1',
  data () {
    return {
      labelCol: { lg: { span: 5 }, sm: { span: 5 } },
      wrapperCol: { lg: { span: 19 }, sm: { span: 19 } },
      loading: false,
      form: this.$form.createForm(this)
    }
  },
  methods: {
    nextStep () {
      const { form: { validateFields } } = this
      const that = this
      // 先校验，通过表单校验后，才进入下一步
      validateFields((err, values) => {
        if (!err) {
          console.log(values)
          that.loading = true
          step1(values).then(res => {
            that.loading = false
            that.$emit('nextStep')
          })
        } else {
          that.loading = false
        }
      })
    }
  }
}
</script>

<style lang="less" scoped>
.step-form-style-desc {
  padding: 0 56px;
  color: rgba(0,0,0,.45);

  h3 {
    margin: 0 0 12px;
    color: rgba(0,0,0,.45);
    font-size: 16px;
    line-height: 32px;
  }

  h4 {
    margin: 0 0 4px;
    color: rgba(0,0,0,.45);
    font-size: 14px;
    line-height: 22px;
  }

  p {
    margin-top: 0;
    margin-bottom: 12px;
    line-height: 22px;
  }
}
</style>
