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
      <a-form-item label="SpringBoot版本" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.springBootVersion">
          <a-radio-button value="2.2.5.RELEASE">2.2.5.RELEASE</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="SpringCloud版本" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.springCloudVersion">
          <a-radio-button value="2.2.3.RELEASE">2.2.3.RELEASE</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="SpringCloudAlibaba版本" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.springCloudAlibabaVersion">
          <a-radio-button value="2.2.1.RELEASE">2.2.1.RELEASE</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="dubbo" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.dubboVersion">
          <a-radio-button value="2.7.6">2.7.6</a-radio-button>
          <a-radio-button value="">不需要</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="mybatis(SQL类型)" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.mybatisType">
          <a-radio-button value="plus">mybatis-plus</a-radio-button>
          <a-radio-button value="xml">xml</a-radio-button>
          <a-radio-button value="annotation">annotation</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="分布式配置" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-model:value="formState.configCenter">
          <a-radio-button value="ZOOKEEPER">zookeeper</a-radio-button>
          <a-radio-button value="NACOS">nacos</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item v-if="formState.configCenter === 'ZOOKEEPER'" label="zookeeper地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.zookeeperAddr" placeholder="zookeeper连接地址,如:localhost:2181"/>
      </a-form-item>
      <a-form-item v-if="formState.configCenter === 'NACOS'" label="nacos地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.nacosAddr" placeholder="nacos连接地址,如:localhost:8848"/>
      </a-form-item>
      <a-form-item label="redis地址" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.redisHost" placeholder="redis单机或集群地址"/>
      </a-form-item>
      <a-form-item label="redis密码" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-input v-model:value="formState.redisPassword" placeholder="redis密码"/>
      </a-form-item>
      <a-form-item label="流量哨兵" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
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
import { ref, reactive } from 'vue'
import { step2 } from '@/api/generator'

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
      view: 'VUE',
      projectBuilder: 'MAVEN',
      springBootVersion: '2.2.5.RELEASE',
      springCloudVersion: '2.2.3.RELEASE',
      springCloudAlibabaVersion: '2.2.1.RELEASE',
      dubboVersion: '2.7.6',
      mybatisType: 'plus',
      configCenter: 'ZOOKEEPER',
      zookeeperAddr: 'localhost:2181',
      nacosAddr: 'localhost:8848',
      redisHost: 'localhost:6379',
      redisPassword: '123456',
      sentinelAddr: 'localhost:7030',
      skywalkingAddr: 'localhost:11800',
      secure: ['JWT', 'SHIRO']
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