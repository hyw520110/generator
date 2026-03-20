<template>
  <div>
    <a-form :model="formState" style="max-width: 800px; margin: 40px auto 0;" ref="formRef" :rules="rules">
      <a-form-item
        label="生成目录"
        name="outputDir" required
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        help="此目录需要有写权限，最后一级子目录为项目名"
      >
        <a-input v-model:value="formState.outputDir" placeholder="此目录需可写，最后一级子目录为项目名"/>
      </a-form-item>
      <a-form-item
        label="项目描述"
        name="description"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        help="项目描述，如：后台管理项目，移动端项目"
      >
        <a-input v-model:value="formState.description" placeholder="项目描述"/>
      </a-form-item>
      <a-form-item
        label="包名"
        name="rootPackage" required
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        help="java类包名"
      >
        <a-input v-model:value="formState.rootPackage" placeholder="com.big.box"/>
      </a-form-item>
      <a-form-item
        label="工程模块"
        name="modules" required
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        help="工程模块名，多个用逗号分隔，如：api,app"
      >
        <a-input v-model:value="formState.modules" placeholder="工程模块名，如：api,app"/>
      </a-form-item>
      <a-form-item
        label="删除目录"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        help="删除生成目录，默认否，开启时需谨慎!"
      >
        <a-switch v-model:checked="formState.delOutputDir" checkedChildren="是" unCheckedChildren="否"/>
      </a-form-item>
      <a-form-item
        label="文件覆盖"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        help="生成同名文件时，是否覆盖原文件/强制生成"
      >
        <a-switch v-model:checked="formState.fileOverride" checkedChildren="是" unCheckedChildren="否" />
      </a-form-item>
      <a-form-item
        label="打开目录"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        help="代码生成完成后是否打开生成目录"
      >
        <a-switch v-model:checked="formState.openDir" checkedChildren="是" unCheckedChildren="否" />
      </a-form-item>
      <a-form-item :wrapperCol="{span: 19, offset: 5}">
        <a-button :loading="loading" type="primary" @click="nextStep">下一步</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { step1 } from '@/api/generator'

export default {
  name: 'Step1',
  emits: ['nextStep'],
  setup (props, { emit }) {
    const formRef = ref()
    const loading = ref(false)
    
    const labelCol = { lg: { span: 5 }, sm: { span: 5 } }
    const wrapperCol = { lg: { span: 19 }, sm: { span: 19 } }
    
    const formState = reactive({
      outputDir: '/opt/output/demo',
      description: '代码生成器',
      rootPackage: 'com.hyw.generator',
      modules: 'api,app',
      delOutputDir: false,
      fileOverride: true,
      openDir: true
    })
    
    const rules = {
      outputDir: [{ required: true, message: '请输入生成目录，此目录需可写，最后一级子目录为项目名' }],
      rootPackage: [{ required: true, message: '请输入包名' }],
      modules: [{ required: true, message: '请输入工程模块名' }]
    }
    
    const nextStep = async () => {
      try {
        await formRef.value.validate()
        loading.value = true
        await step1(formState)
        loading.value = false
        emit('nextStep')
      } catch (error) {
        loading.value = false
      }
    }
    
    return {
      formRef,
      formState,
      labelCol,
      wrapperCol,
      loading,
      rules,
      nextStep
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

// 必填项红色星号样式
:deep(.ant-form-item-required::after) {
  display: inline-block;
  margin-left: 4px;
  color: #ff4d4f;
  font-size: 14px;
  font-family: SimSun, sans-serif;
  line-height: 1;
  content: '*';
}
</style>