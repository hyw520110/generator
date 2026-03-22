<template>
  <div>
    <a-form :model="formState" style="max-width: 800px; margin: 40px auto 0;" ref="formRef">
      <a-form-item label="视图" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.view">
          <a-radio-button value="VUE">VUE(ant-design-pro-vue)</a-radio-button>
          <a-radio-button value="THYMELEAF">THYMELEAF</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="构建" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.projectBuilder">
          <a-radio-button value="MAVEN">maven</a-radio-button>
          <a-radio-button value="GRADLE">gradle</a-radio-button>
          <a-radio-button value="">不生成</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="Java版本" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.javaVersion" @change="onJavaVersionChange">
          <a-radio-button value="8">Java 8</a-radio-button>
          <a-radio-button value="11">Java 11</a-radio-button>
          <a-radio-button value="17">Java 17</a-radio-button>
          <a-radio-button value="21">Java 21</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="SpringBoot版本" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.springBootVersion">
          <a-radio-button v-for="v in versionOptions.springBoot" :key="v" :value="v">{{ v }}</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="SpringCloud版本" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.springCloudVersion">
          <a-radio-button v-for="v in versionOptions.springCloud" :key="v" :value="v">{{ v }}</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="SpringCloudAlibaba版本" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.springCloudAlibabaVersion">
          <a-radio-button v-for="v in versionOptions.springCloudAlibaba" :key="v" :value="v">{{ v }}</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="dubbo" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.dubboVersion" @change="onDubboVersionChange">
          <a-radio-button v-for="v in versionOptions.dubbo" :key="v" :value="v">{{ v || '不需要' }}</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="mybatis(SQL类型)" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.mybatisType">
          <a-radio-button value="plus">mybatis-plus</a-radio-button>
          <a-radio-button value="xml">xml</a-radio-button>
          <a-radio-button value="annotation">annotation</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="注册中心/配置中心" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.registryCenter">
          <a-radio-button value="nacos">nacos</a-radio-button>
          <a-radio-button value="zookeeper">zookeeper</a-radio-button>
          <a-radio-button value="">不需要</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item v-if="formState.registryCenter === 'zookeeper'" label="zookeeper地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.zookeeperAddr" placeholder="zookeeper连接地址,如:localhost:2181"/>
      </a-form-item>
      <a-form-item v-if="formState.registryCenter === 'nacos'" label="nacos地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.nacosAddr" placeholder="nacos连接地址,如:localhost:8848"/>
      </a-form-item>
      <a-form-item v-if="formState.registryCenter === 'nacos'" label="nacos用户名" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.nacosUsername" placeholder="nacos用户名,默认:nacos"/>
      </a-form-item>
      <a-form-item v-if="formState.registryCenter === 'nacos'" label="nacos密码" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.nacosPassword" placeholder="nacos密码,默认:nacos"/>
      </a-form-item>
      <a-form-item label="redis地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.redisHost" placeholder="redis单机或集群地址"/>
      </a-form-item>
      <a-form-item label="redis密码" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.redisPassword" placeholder="redis密码"/>
      </a-form-item>
      <a-form-item label="Sentinel版本" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.sentinelVersion">
          <a-radio-button value="1.8.6">1.8.6</a-radio-button>
          <a-radio-button value="1.8.8">1.8.8</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="Sentinel地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.sentinelAddr" placeholder="请输入SENTINEL地址，不需要则置空"/>
      </a-form-item>
      <a-form-item label="链路监控" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.skywalkingAddr" placeholder="请输入skywalking地址，不需要则置空"/>
      </a-form-item>
      <a-form-item label="认证" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-checkbox-group v-model:value="formState.secure" :options="secureOptions"/>
      </a-form-item>
      <a-form-item :wrapperCol="{span: 19, offset: 5}">
        <a-button :loading="loading" style="margin-left: 8px; margin-right: 16px; cursor: pointer;" @click="prevStep">上一步</a-button>
        <a-button :loading="loading" type="primary" style="cursor: pointer;" @click="nextStep">下一步</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { step2, getConfig } from '@/api/generator'

// Java版本对应的组件版本组合
const JAVA_VERSION_PRESETS = {
  '8': {
    springBoot: ['2.3.12.RELEASE', '2.7.18'],
    springCloud: ['Hoxton.SR12', '2021.0.8'],
    springCloudAlibaba: ['2.2.7.RELEASE', '2021.0.5.0'],
    dubbo: ['2.7.23', ''],
    defaults: {
      springBoot: '2.7.18',
      springCloud: '2021.0.8',
      springCloudAlibaba: '2021.0.5.0',
      dubbo: '2.7.23'
    }
  },
  '11': {
    springBoot: ['2.7.18', '3.0.13'],
    springCloud: ['2021.0.8', '2022.0.4'],
    springCloudAlibaba: ['2021.0.5.0', '2022.0.0.0'],
    dubbo: ['2.7.23', '3.2.10', '3.3.0', ''],
    defaults: {
      springBoot: '2.7.18',
      springCloud: '2021.0.8',
      springCloudAlibaba: '2021.0.5.0',
      dubbo: '3.2.10'
    }
  },
  '17': {
    springBoot: ['3.0.13', '3.2.12', '3.4.0'],
    springCloud: ['2022.0.4', '2023.0.0', '2024.0.0'],
    springCloudAlibaba: ['2022.0.0.0', '2023.0.1.0', '2023.0.3.2'],
    dubbo: ['3.2.10', '3.3.0', ''],
    defaults: {
      springBoot: '3.2.12',
      springCloud: '2023.0.0',
      springCloudAlibaba: '2023.0.1.0',
      dubbo: '3.3.0'
    }
  },
  '21': {
    springBoot: ['3.2.12', '3.4.0'],
    springCloud: ['2023.0.0', '2024.0.0'],
    springCloudAlibaba: ['2023.0.1.0', '2023.0.3.2'],
    dubbo: ['3.3.0', ''],
    defaults: {
      springBoot: '3.4.0',
      springCloud: '2024.0.0',
      springCloudAlibaba: '2023.0.3.2',
      dubbo: ''
    }
  }
}

export default {
  name: 'Step2',
  emits: ['nextStep', 'prevStep'],
  setup (props, { emit }) {
    const formRef = ref()
    const loading = ref(false)
    
    const labelCol = { lg: { span: 5 }, sm: { span: 5 } }
    const wrapperCol = { lg: { span: 19 }, sm: { span: 19 } }
    
    const secureOptions = [
      { label: 'jwt', value: 'JWT' },
      { label: 'shiro', value: 'SHIRO' }
    ]
    
    const formState = reactive({
      javaVersion: '17',
      view: 'VUE',
      projectBuilder: 'MAVEN',
      springBootVersion: '3.2.12',
      springCloudVersion: '2023.0.0',
      springCloudAlibabaVersion: '2023.0.1.0',
      dubboVersion: '',
      registryCenter: '',
      mybatisType: 'plus',
      zookeeperAddr: 'localhost:2181',
      nacosAddr: 'localhost:8848',
      nacosUsername: 'nacos',
      nacosPassword: 'nacos',
      redisHost: 'localhost:6379',
      redisPassword: '',
      sentinelVersion: '1.8.6',
      sentinelAddr: '',
      skywalkingAddr: '',
      secure: ['JWT', 'SHIRO']
    })
    
    // 根据Java版本计算可选版本
    const versionOptions = computed(() => {
      const preset = JAVA_VERSION_PRESETS[formState.javaVersion] || JAVA_VERSION_PRESETS['17']
      return {
        springBoot: preset.springBoot,
        springCloud: preset.springCloud,
        springCloudAlibaba: preset.springCloudAlibaba,
        dubbo: preset.dubbo
      }
    })
    
    // Java版本变化时自动更新组件版本
    const onJavaVersionChange = () => {
      const preset = JAVA_VERSION_PRESETS[formState.javaVersion]
      if (preset) {
        // 自动设置为该Java版本的默认值
        formState.springBootVersion = preset.defaults.springBoot
        formState.springCloudVersion = preset.defaults.springCloud
        formState.springCloudAlibabaVersion = preset.defaults.springCloudAlibaba
        formState.dubboVersion = preset.defaults.dubbo
        // 默认使用 nacos 作为注册中心/配置中心
        if (formState.dubboVersion) {
          formState.registryCenter = 'nacos'
        }
      }
    }
    
    // Dubbo版本变化时的联动逻辑
    const onDubboVersionChange = () => {
      if (!formState.dubboVersion) {
        // 选择"不需要"dubbo，注册中心也清空
        formState.registryCenter = ''
      } else if (!formState.registryCenter) {
        // 选择了dubbo版本，但注册中心是"不需要"，自动选择nacos
        formState.registryCenter = 'nacos'
      }
      // 如果已经是nacos或zookeeper，保持不变
    }
    
    // 组件配置映射表：组件名 -> { 配置key: formState字段名 }
    const componentMappings = {
      SPRINGBOOT: { 'springboot_version': 'springBootVersion' },
      SPRINGCLOUD: { 'springcloud_version': 'springCloudVersion', 'springcloud_alibaba_version': 'springCloudAlibabaVersion' },
      DUBBO: { 'dubbo_version': 'dubboVersion' },
      MYBATIS: { 'mapperType': 'mybatisType' },
      ZOOKEEPER: { 'connect-string': 'zookeeperAddr' },
      REDIS: { 'spring_redis_cluster_nodes': 'redisHost', 'spring_redis_password': 'redisPassword' },
      SENTINEL: { 'sentinel_version': 'sentinelVersion', 'dashboard.server': 'sentinelAddr' },
      SKYWALKING: { 'skywalking.addr': 'skywalkingAddr' },
      NACOS: { 'nacos.addr': 'nacosAddr' }
    }
    
    // 根据版本推断Java版本
    const inferJavaVersion = (springBootVersion) => {
      if (!springBootVersion) return '17'
      if (springBootVersion.startsWith('3.2') || springBootVersion.startsWith('3.3') || springBootVersion.startsWith('3.4')) {
        return '17'
      }
      if (springBootVersion.startsWith('3.0') || springBootVersion.startsWith('3.1')) {
        return '17'
      }
      if (springBootVersion === '2.7.18') {
        return '11'  // 2.7.18 支持 Java 8-21，默认使用 11 作为中间版本
      }
      return '8'
    }
    
    // 页面加载时获取配置
    onMounted(async () => {
      try {
        const res = await getConfig()
        if (res.status === 10000 && res.data) {
          const global = res.data.global || {}
          const components = res.data.components || {}
          
          // 从 global 获取 view 和 projectBuilder
          const globalComps = global.components || []
          formState.view = globalComps.includes('VUE') ? 'VUE' : (globalComps.includes('THYMELEAF') ? 'THYMELEAF' : 'VUE')
          formState.projectBuilder = global.projectBuilder || 'MAVEN'
          
          // 从 global 获取 javaVersion（仅当配置了有效值时才覆盖默认值）
          // 默认使用 Java 17，不再根据 YAML 配置自动覆盖
          // if (global.javaVersion) {
          //   const javaVer = String(global.javaVersion).replace('1.', '')
          //   if (['8', '11', '17', '21'].includes(javaVer)) {
          //     formState.javaVersion = javaVer
          //   }
          // }
          
          // 遍历 components，根据映射表填充 formState
          Object.entries(componentMappings).forEach(([compName, fieldMappings]) => {
            const compConfig = components[compName]
            if (compConfig) {
              // 特殊处理：NACOS 或 ZOOKEEPER 存在时设置 registryCenter
              if (compName === 'NACOS') {
                formState.registryCenter = 'nacos'
              }
              if (compName === 'ZOOKEEPER') {
                formState.registryCenter = 'zookeeper'
              }
              Object.entries(fieldMappings).forEach(([configKey, formField]) => {
                const value = compConfig[configKey]
                if (value !== undefined && value !== null && value !== '') {
                  formState[formField] = value
                }
              })
            }
          })
          
          // 默认使用 Java 17，不再根据 SpringBoot 版本推断
          // if (!global.javaVersion) {
          //   formState.javaVersion = inferJavaVersion(formState.springBootVersion)
          // }
          
          // 认证组件
          const secure = globalComps.filter(c => c === 'JWT' || c === 'SHIRO')
          if (secure.length > 0) formState.secure = secure
        }
      } catch (err) {
        console.error('获取配置失败:', err)
      }
    })
    
    const nextStep = async () => {
      loading.value = true
      try {
        await step2(formState)
        loading.value = false
        emit('nextStep')
      } catch (error) {
        loading.value = false
      }
    }
    
    const prevStep = () => {
      emit('prevStep')
    }
    
    return {
      formRef,
      formState,
      labelCol,
      wrapperCol,
      loading,
      secureOptions,
      versionOptions,
      onJavaVersionChange,
      onDubboVersionChange,
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
.ant-radio-group-solid .ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled){
  color: #fff;
}
.ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled) {
  color: #40a9ff;
}
</style>
