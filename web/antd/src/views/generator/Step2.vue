<template>
  <div>
    <a-form :model="formState" style="max-width: 800px; margin: 40px auto 0;" ref="formRef">
      <!-- 组件分组展示 -->
      <a-divider orientation="left">组件选择</a-divider>

      <!-- 互斥组（单选） -->
      <template v-for="grp in exclusiveGroups" :key="grp.group">
        <a-form-item :label="grp.name" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
          <template #label>
            {{ grp.name }}
            <a-tag v-if="grp.required" color="red">必选</a-tag>
          </template>
          <a-radio-group button-style="solid" v-model:value="formState[grp.field]">
            <a-radio-button
              v-for="opt in grp.options"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
      </template>

      <!-- 非互斥组（多选）- 认证授权 -->
      <a-form-item label="认证授权" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <template #label>
          认证授权
          <a-tag color="blue">可多选</a-tag>
        </template>
        <a-checkbox-group v-model:value="formState.secure">
          <a-checkbox v-for="opt in authOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </a-checkbox>
        </a-checkbox-group>
      </a-form-item>

      <!-- 构建工具 -->
      <a-form-item label="构建工具" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <template #label>
          构建工具
          <a-tag color="red">必选</a-tag>
        </template>
        <a-radio-group button-style="solid" v-model:value="formState.projectBuilder">
          <a-radio-button v-for="opt in buildTools" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </a-radio-button>
        </a-radio-group>
      </a-form-item>

      <!-- 组件配置（根据选择动态显示） -->
      <a-divider orientation="left">组件配置</a-divider>

      <!-- 注册中心配置 -->
      <template v-if="formState.registryCenter === 'zookeeper'">
        <a-form-item label="zookeeper地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
          <a-input v-model:value="formState.zookeeperAddr" placeholder="zookeeper连接地址,如:localhost:2181"/>
        </a-form-item>
      </template>
      <template v-if="formState.registryCenter === 'nacos'">
        <a-form-item label="nacos地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
          <a-input v-model:value="formState.nacosAddr" placeholder="nacos连接地址,如:localhost:8848"/>
        </a-form-item>
        <a-form-item label="nacos用户名" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
          <a-input v-model:value="formState.nacosUsername" placeholder="nacos用户名,默认:nacos"/>
        </a-form-item>
        <a-form-item label="nacos密码" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
          <a-input v-model:value="formState.nacosPassword" placeholder="nacos密码,默认:nacos"/>
        </a-form-item>
      </template>

      <!-- Redis配置 -->
      <template v-if="formState.redisEnabled">
        <a-form-item label="redis地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
          <a-input v-model:value="formState.redisHost" placeholder="redis单机或集群地址"/>
        </a-form-item>
        <a-form-item label="redis密码" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
          <a-input v-model:value="formState.redisPassword" placeholder="redis密码"/>
        </a-form-item>
      </template>

      <!-- Sentinel配置 -->
      <template v-if="formState.sentinelEnabled">
        <a-form-item label="Sentinel版本" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
          <a-radio-group button-style="solid" v-model:value="formState.sentinelVersion">
            <a-radio-button value="1.8.6">1.8.6</a-radio-button>
            <a-radio-button value="1.8.8">1.8.8</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="Sentinel地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
          <a-input v-model:value="formState.sentinelAddr" placeholder="请输入SENTINEL地址，不需要则置空"/>
        </a-form-item>
      </template>

      <!-- 链路追踪配置 -->
      <template v-if="formState.trace">
        <a-form-item label="链路监控地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
          <a-input v-model:value="formState.skywalkingAddr" :placeholder="formState.trace === 'SKYWALKING' ? '请输入skywalking地址' : '请输入zipkin地址'"/>
        </a-form-item>
      </template>

      <!-- 版本选择（独立于组件分组） -->
      <a-divider orientation="left">版本配置</a-divider>
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
      <a-form-item label="Dubbo版本" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.dubboVersion" @change="onDubboVersionChange">
          <a-radio-button v-for="v in versionOptions.dubbo" :key="v" :value="v">{{ v || '不需要' }}</a-radio-button>
        </a-radio-group>
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

// 组件分组配置（互斥组，单选）
const exclusiveGroups = [
  {
    group: 'ORM',
    name: 'ORM框架',
    required: true,
    field: 'mybatisType',
    options: [
      { value: 'plus', label: 'MyBatis-Plus', component: 'MYBATIS' },
      { value: 'xml', label: 'MyBatis XML', component: 'MYBATIS' },
      { value: 'jpa', label: 'JPA', component: 'JPA' }
    ]
  },
  {
    group: 'VIEW',
    name: '视图技术',
    required: false,
    field: 'view',
    options: [
      { value: 'VUE', label: 'Vue3 + Vite', component: 'VUE' },
      { value: 'THYMELEAF', label: 'Thymeleaf', component: 'THYMELEAF' }
    ]
  },
  {
    group: 'REGISTRY',
    name: '注册/配置中心',
    required: false,
    field: 'registryCenter',
    options: [
      { value: 'nacos', label: 'Nacos', component: 'NACOS' },
      { value: 'zookeeper', label: 'Zookeeper', component: 'ZOOKEEPER' },
      { value: '', label: '不需要', component: null }
    ]
  },
  {
    group: 'MICROSERVICE',
    name: '微服务框架',
    required: false,
    field: 'microservice',
    options: [
      { value: 'SPRINGCLOUD', label: 'Spring Cloud', component: 'SPRINGCLOUD' },
      { value: 'DUBBO', label: 'Dubbo', component: 'DUBBO' },
      { value: '', label: '不需要', component: null }
    ]
  },
  {
    group: 'TRACE',
    name: '链路追踪',
    required: false,
    field: 'trace',
    options: [
      { value: 'SKYWALKING', label: 'SkyWalking', component: 'SKYWALKING' },
      { value: 'ZIPKIN', label: 'Zipkin', component: 'ZIPKIN' },
      { value: '', label: '不需要', component: null }
    ]
  },
  {
    group: 'CACHE',
    name: '缓存',
    required: false,
    field: 'redisEnabled',
    options: [
      { value: 'true', label: 'Redis', component: 'REDIS' },
      { value: '', label: '不需要', component: null }
    ]
  },
  {
    group: 'FLOW_PROTECT',
    name: '流量防护',
    required: false,
    field: 'sentinelEnabled',
    options: [
      { value: 'true', label: 'Sentinel', component: 'SENTINEL' },
      { value: '', label: '不需要', component: null }
    ]
  },
  {
    group: 'API_DOC',
    name: '接口文档',
    required: false,
    field: 'swaggerEnabled',
    options: [
      { value: 'true', label: 'Swagger2', component: 'SWAGGER2' },
      { value: '', label: '不需要', component: null }
    ]
  }
]

// 构建工具选项
const buildTools = [
  { value: 'MAVEN', label: 'Maven' },
  { value: 'GRADLE', label: 'Gradle' }
]

// 认证授权选项（非互斥，可多选）
const authOptions = [
  { value: 'SHIRO', label: 'Shiro' },
  { value: 'JWT', label: 'JWT' }
]

export default {
  name: 'Step2',
  emits: ['nextStep', 'prevStep'],
  setup (props, { emit }) {
    const formRef = ref()
    const loading = ref(false)

    const labelCol = { lg: { span: 5 }, sm: { span: 5 } }
    const wrapperCol = { lg: { span: 19 }, sm: { span: 19 } }

    const formState = reactive({
      javaVersion: '17',
      view: 'VUE',
      projectBuilder: 'MAVEN',
      microservice: '',
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
      trace: '',
      redisEnabled: '',
      sentinelEnabled: '',
      swaggerEnabled: '',
      secure: []
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
        formState.springBootVersion = preset.defaults.springBoot
        formState.springCloudVersion = preset.defaults.springCloud
        formState.springCloudAlibabaVersion = preset.defaults.springCloudAlibaba
        formState.dubboVersion = preset.defaults.dubbo
        if (formState.dubboVersion) {
          formState.registryCenter = 'nacos'
        }
      }
    }

    // Dubbo版本变化时的联动逻辑
    const onDubboVersionChange = () => {
      if (!formState.dubboVersion) {
        formState.registryCenter = ''
      } else if (!formState.registryCenter) {
        formState.registryCenter = 'nacos'
      }
    }

    // 组件配置映射表
    const componentMappings = {
      SPRINGBOOT: { 'springboot_version': 'springBootVersion' },
      SPRINGCLOUD: { 'springcloud_version': 'springCloudVersion', 'springcloud_alibaba_version': 'springCloudAlibabaVersion' },
      DUBBO: { 'dubbo_version': 'dubboVersion' },
      MYBATIS: { 'mapperType': 'mybatisType' },
      ZOOKEEPER: { 'connect-string': 'zookeeperAddr' },
      REDIS: { 'spring_redis_cluster_nodes': 'redisHost', 'spring_redis_password': 'redisPassword' },
      SENTINEL: { 'sentinel_version': 'sentinelVersion', 'dashboard.server': 'sentinelAddr' },
      SKYWALKING: { 'skywalking.addr': 'skywalkingAddr' },
      NACOS: { 'nacos.addr': 'nacosAddr' },
      SWAGGER2: { 'swagger_version': 'swaggerEnabled' }
    }

    // 页面加载时获取配置
    onMounted(async () => {
      try {
        const res = await getConfig()
        if (res.status === 10000 && res.data) {
          const global = res.data.global || {}
          const components = res.data.components || {}

          const globalComps = global.components || []

          // 设置视图
          formState.view = globalComps.includes('VUE') ? 'VUE' : (globalComps.includes('THYMELEAF') ? 'THYMELEAF' : 'VUE')

          // 设置项目构建方式
          if (global.projectBuilder) {
            formState.projectBuilder = global.projectBuilder
          }

          // 遍历 components，根据映射表填充 formState
          Object.entries(componentMappings).forEach(([compName, fieldMappings]) => {
            const compConfig = components[compName]
            if (compConfig) {
              // 注册中心
              if (compName === 'NACOS') {
                formState.registryCenter = 'nacos'
              }
              if (compName === 'ZOOKEEPER') {
                formState.registryCenter = 'zookeeper'
              }
              // 链路追踪
              if (compName === 'SKYWALKING' || compName === 'ZIPKIN') {
                formState.trace = compName
              }
              // 缓存
              if (compName === 'REDIS') {
                formState.redisEnabled = 'true'
              }
              // 流量防护
              if (compName === 'SENTINEL') {
                formState.sentinelEnabled = 'true'
              }
              // 接口文档
              if (compName === 'SWAGGER2') {
                formState.swaggerEnabled = 'true'
              }

              Object.entries(fieldMappings).forEach(([configKey, formField]) => {
                const value = compConfig[configKey]
                if (value !== undefined && value !== null && value !== '') {
                  formState[formField] = value
                }
              })
            }
          })

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
      versionOptions,
      exclusiveGroups,
      authOptions,
      buildTools,
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