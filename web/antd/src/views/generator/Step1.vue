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
        <a-input 
          v-model:value="formState.outputDir" 
          placeholder="此目录需可写，最后一级子目录为项目名"
          @blur="validateOutputDir"
        >
          <template #suffix>
            <loading-outlined v-if="dirValidating" style="color: #1890ff" />
            <check-circle-outlined v-else-if="dirValid === true" style="color: #52c41a" />
            <close-circle-outlined v-else-if="dirValid === false" style="color: #ff4d4f" />
          </template>
        </a-input>
        <div v-if="dirValidMsg" :style="{ color: dirValid ? '#52c41a' : '#ff4d4f', fontSize: '12px', marginTop: '4px' }">
          {{ dirValidMsg }}
        </div>
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
import { ref, reactive, onMounted } from 'vue'
import { step1, getConfig, validateOutputDir } from '@/api/generator'
import { LoadingOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'

export default {
  name: 'Step1',
  components: { LoadingOutlined, CheckCircleOutlined, CloseCircleOutlined },
  emits: ['nextStep'],
  setup (props, { emit }) {
    const formRef = ref()
    const loading = ref(false)
    const dirValidating = ref(false)
    const dirValid = ref(null)
    const dirValidMsg = ref('')
    
    const labelCol = { lg: { span: 5 }, sm: { span: 5 } }
    const wrapperCol = { lg: { span: 19 }, sm: { span: 19 } }
    
    const formState = reactive({
      outputDir: '',
      description: '',
      rootPackage: '',
      modules: '',
      delOutputDir: false,
      fileOverride: false,
      openDir: true
    })
    
    const rules = {
      outputDir: [{ required: true, message: '请输入生成目录，此目录需可写，最后一级子目录为项目名' }],
      rootPackage: [{ required: true, message: '请输入包名' }],
      modules: [{ required: true, message: '请输入工程模块名' }]
    }
    
    // 页面加载时获取配置
    onMounted(async () => {
      try {
        const res = await getConfig()
        if (res.status === 10000 && res.data) {
          const global = res.data.global || {}
          const defaults = res.data.defaults || {}
          
          // 使用默认值初始化，因为 global 中的值可能是配置文件的初始值
          formState.outputDir = defaults.outputDir || ''
          formState.description = global.description || '代码生成器'
          formState.rootPackage = global.rootPackage || 'com.hyw.generator'
          formState.modules = global.modules ? global.modules.join(',') : 'api,app'
          formState.delOutputDir = global.delOutputDir || false
          formState.fileOverride = global.fileOverride !== undefined ? global.fileOverride : true
          formState.openDir = global.openDir !== undefined ? global.openDir : true
        }
      } catch (err) {
        console.error('获取配置失败:', err)
      }
    })
    
    // 验证输出目录权限
    const handleValidateOutputDir = async () => {
      if (!formState.outputDir) {
        dirValid.value = null
        dirValidMsg.value = ''
        return
      }
      
      dirValidating.value = true
      dirValid.value = null
      dirValidMsg.value = ''
      
      try {
        const res = await validateOutputDir({ outputDir: formState.outputDir })
        if (res.status === 10000) {
          dirValid.value = true
          dirValidMsg.value = res.message || '目录验证通过'
        } else {
          dirValid.value = false
          dirValidMsg.value = res.message || '验证失败'
        }
      } catch (err) {
        dirValid.value = false
        dirValidMsg.value = err.response?.data?.message || '验证失败'
      } finally {
        dirValidating.value = false
      }
    }
    
    const nextStep = async () => {
      try {
        await formRef.value.validate()
        
        // 如果目录验证未通过，提示用户
        if (dirValid.value === false) {
          return
        }
        
        // 如果还未验证，先验证
        if (dirValid.value === null && formState.outputDir) {
          await handleValidateOutputDir()
          if (dirValid.value === false) {
            return
          }
        }
        
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
      dirValidating,
      dirValid,
      dirValidMsg,
      validateOutputDir: handleValidateOutputDir,
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