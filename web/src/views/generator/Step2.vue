<template>
  <div>
    <a-form :form="form" style="max-width: 800px; margin: 40px auto 0;">
      <a-form-item label="视图" :labelCol="labelCol" :wrapperCol="wrapperCol" class="stepFormText">
        <a-radio-group button-style="solid" v-decorator="['view', { initialValue: 'VUE', rules: [{required: true}] }]" >
          <a-radio-button value="VUE">VUE(ant-design-pro-vue)</a-radio-button>
          <a-radio-button value="THYMELEAF">THYMELEAF</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        label="构建"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-radio-group button-style="solid" v-decorator="['projectBuilder', { initialValue: 'MAVEN', rules: [{required: false}] }]" >
          <a-radio-button value="MAVEN">maven</a-radio-button>
          <a-radio-button value="GRADLE">gradle</a-radio-button>
          <a-radio-button value="">不生成</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        label="SpringBoot版本"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-radio-group button-style="solid" v-decorator="['springBootVersion', { initialValue: '2.2.5.RELEASE', rules: [{required: true}] }]">
          <a-radio-button value="2.2.5.RELEASE">2.2.5.RELEASE</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        label="SpringCloud版本"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-radio-group button-style="solid" v-decorator="['springCloudVersion', { initialValue: '2.2.3.RELEASE', rules: [{required: true}] }]">
          <a-radio-button value="2.2.3.RELEASE">2.2.3.RELEASE</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        label="SpringCloudAlibaba版本"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-radio-group button-style="solid" v-decorator="['springCloudAlibabaVersion', { initialValue: '2.2.1.RELEASE', rules: [{required: true}] }]">
          <a-radio-button value="2.2.1.RELEASE">2.2.1.RELEASE</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        label="dubbo"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-radio-group button-style="solid" v-decorator="['dubboVersion', { initialValue: '2.7.6', rules: [{required: false}] }]">
          <a-radio-button value="2.7.6">2.7.6</a-radio-button>
          <a-radio-button value="">不需要</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        label="mybatis(SQL类型)"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-radio-group button-style="solid" v-decorator="['mybatisType', { initialValue: 'plus', rules: [{required: true}] }]">
          <a-radio-button value="plus">mybatis-plus</a-radio-button>
          <a-radio-button value="xml">xml</a-radio-button>
          <a-radio-button value="annotation">annotation</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        label="分布式配置"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-radio-group button-style="solid" v-decorator="['configCenter', { initialValue: 'ZOOKEEPER', rules: [{required: true}] }]">
          <a-radio-button value="ZOOKEEPER">zookeeper</a-radio-button>
          <a-radio-button value="NACOS">nacos</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        label="zookeeper地址"
        v-if="form.getFieldValue('configCenter') === 'ZOOKEEPER'"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-input v-decorator="['zookeeperAddr', { initialValue: 'localhost:2181', rules: [{required: true, message: '请输入zookeeper连接地址'}] }]" placeholder="zookeeper连接地址,如:localhost:2181"/>
      </a-form-item>
      <a-form-item
        label="nacos地址"
        v-if="form.getFieldValue('configCenter') === 'NACOS'"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-input v-decorator="['nacosAddr', { initialValue: 'localhost:8848', rules: [{required: true, message: '请输入nacos连接地址'}] }]" placeholder="nacos连接地址,如:localhost:8848"/>
      </a-form-item>
      <a-form-item
        label="redis地址"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-input v-decorator="['redisHost', { initialValue: 'localhost:6379', rules: [{required: true, message: '请输入redis地址'}] }]" placeholder="redis单机或集群地址"/>
      </a-form-item>
      <a-form-item
        label="redis密码"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-input v-decorator="['redisPassword', { initialValue: '123456', rules: [{required: true, message: '请输入redis密码'}] }]" placeholder="redis密码"/>
      </a-form-item>
      <a-form-item
        label="流量哨兵"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-input v-decorator="['sentinelAddr', { initialValue: 'localhost:7030', rules: [{required: false, message: '请输入SENTINEL地址'}] }]" placeholder="请输入SENTINEL地址，不需要则置空"/>
      </a-form-item>
      <a-form-item
        label="链路监控"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-input v-decorator="['skywalkingAddr', { initialValue: 'localhost:11800', rules: [{required: false, message: '请输入skywalking地址'}] }]" placeholder="请输入skywalking地址，不需要则置空"/>
      </a-form-item>
      <a-form-item
        label="认证"
        :labelCol="labelCol"
        :wrapperCol="wrapperCol"
        class="stepFormText"
      >
        <a-checkbox-group :options="secure" v-decorator="['secure', { initialValue: ['SHIRO','JWT'], rules: [{required: true}] }]">
        </a-checkbox-group>
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
import { step2 } from '@/api/generator'
export default {
  name: 'Step2',
  data () {
    return {
      labelCol: { lg: { span: 5 }, sm: { span: 5 } },
      wrapperCol: { lg: { span: 19 }, sm: { span: 19 } },
      form: this.$form.createForm(this),
      loading: false,
      timer: 0,
      secure: [ { label: 'jwt', value: 'JWT' }, { label: 'shiro', value: 'SHIRO' } ]
    }
  },
  methods: {
    nextStep () {
      const that = this
      const { form: { validateFields } } = this
      that.loading = true
      validateFields((err, values) => {
        if (!err) {
          console.log('表单 values', values)
          step2(values).then(res => {
            that.loading = false
            that.$emit('nextStep')
          })
        } else {
          that.loading = false
        }
      })
    },
    prevStep () {
      this.$emit('prevStep')
    }
  },
  beforeDestroy () {
    clearTimeout(this.timer)
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
