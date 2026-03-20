<template>
  <div class="main">
    <a-form
      id="formLogin"
      class="user-layout-login"
      ref="formRef"
      :model="formState"
      @submit="handleSubmit"
    >
      <a-tabs
        v-model:activeKey="customActiveKey"
        :tabBarStyle="{ textAlign: 'center', borderBottom: 'unset' }"
        @change="handleTabClick"
      >
        <a-tab-pane key="tab1" tab="账号密码登录">
          <a-alert v-if="isLoginError" type="error" showIcon style="margin-bottom: 24px;" message="账户或密码错误（admin/ant.design )" />
          <a-form-item name="username">
            <a-input
              size="large"
              type="text"
              placeholder="账户: admin"
              v-model:value="formState.username"
            >
              <template #prefix>
                <UserOutlined :style="{ color: 'rgba(0,0,0,.25)' }"/>
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="password">
            <a-input-password
              size="large"
              placeholder="密码: admin or ant.design"
              v-model:value="formState.password"
            >
              <template #prefix>
                <LockOutlined :style="{ color: 'rgba(0,0,0,.25)' }"/>
              </template>
            </a-input-password>
          </a-form-item>
        </a-tab-pane>
        <a-tab-pane key="tab2" tab="手机号登录">
          <a-form-item name="mobile">
            <a-input size="large" type="text" placeholder="手机号" v-model:value="formState.mobile">
              <template #prefix>
                <MobileOutlined :style="{ color: 'rgba(0,0,0,.25)' }"/>
              </template>
            </a-input>
          </a-form-item>

          <a-row :gutter="16">
            <a-col class="gutter-row" :span="16">
              <a-form-item name="captcha">
                <a-input size="large" type="text" placeholder="验证码" v-model:value="formState.captcha">
                  <template #prefix>
                    <MailOutlined :style="{ color: 'rgba(0,0,0,.25)' }"/>
                  </template>
                </a-input>
              </a-form-item>
            </a-col>
            <a-col class="gutter-row" :span="8">
              <a-button
                class="getCaptcha"
                tabindex="-1"
                :disabled="state.smsSendBtn"
                @click.stop.prevent="getCaptcha"
              >{{ !state.smsSendBtn ? '获取验证码' : (state.time + ' s') }}</a-button>
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>

      <a-form-item>
        <a-checkbox v-model:checked="formState.rememberMe">自动登录</a-checkbox>
        <router-link
          :to="{ name: 'recover', params: { user: 'aaa'} }"
          class="forge-password"
          style="float: right;"
        >忘记密码</router-link>
      </a-form-item>

      <a-form-item style="margin-top:24px">
        <a-button
          size="large"
          type="primary"
          htmlType="submit"
          class="login-button"
          :loading="state.loginBtn"
          :disabled="state.loginBtn"
        >确定</a-button>
      </a-form-item>

      <div class="user-login-other">
        <span>其他登录方式</span>
        <a>
          <AlipayCircleOutlined class="item-icon"/>
        </a>
        <a>
          <TaobaoCircleOutlined class="item-icon"/>
        </a>
        <a>
          <WeiboCircleOutlined class="item-icon"/>
        </a>
        <router-link class="register" :to="{ name: 'register' }">注册账户</router-link>
      </div>
    </a-form>

    <two-step-captcha
      v-if="requiredTwoStepCaptcha"
      :visible="stepCaptchaVisible"
      @success="stepCaptchaSuccess"
      @cancel="stepCaptchaCancel"
    ></two-step-captcha>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { notification, message } from 'ant-design-vue'
import md5 from 'md5'
import {
  UserOutlined,
  LockOutlined,
  MobileOutlined,
  MailOutlined,
  AlipayCircleOutlined,
  TaobaoCircleOutlined,
  WeiboCircleOutlined
} from '@ant-design/icons-vue'
import TwoStepCaptcha from '@/components/tools/TwoStepCaptcha'
import { timeFix } from '@/utils/util'
import { getSmsCaptcha, get2step } from '@/api/login'

export default {
  name: 'Login',
  components: {
    TwoStepCaptcha,
    UserOutlined,
    LockOutlined,
    MobileOutlined,
    MailOutlined,
    AlipayCircleOutlined,
    TaobaoCircleOutlined,
    WeiboCircleOutlined
  },
  setup () {
    const router = useRouter()
    const store = useStore()
    const formRef = ref()
    
    const customActiveKey = ref('tab1')
    const isLoginError = ref(false)
    const requiredTwoStepCaptcha = ref(false)
    const stepCaptchaVisible = ref(false)
    const loginType = ref(0)
    
    const state = reactive({
      time: 60,
      loginBtn: false,
      smsSendBtn: false
    })
    
    const formState = reactive({
      username: '',
      password: '',
      mobile: '',
      captcha: '',
      rememberMe: false
    })
    
    const Login = (loginParams) => {
      return store.dispatch('Login', loginParams)
    }
    
    const Logout = () => {
      return store.dispatch('Logout')
    }
    
    const handleTabClick = (key) => {
      customActiveKey.value = key
    }
    
    const handleSubmit = (e) => {
      e.preventDefault()
      state.loginBtn = true
      
      if (customActiveKey.value === 'tab1') {
        if (!formState.username) {
          notification.error({ message: '错误', description: '请输入帐户名或邮箱地址' })
          state.loginBtn = false
          return
        }
        if (!formState.password) {
          notification.error({ message: '错误', description: '请输入密码' })
          state.loginBtn = false
          return
        }
        
        const regex = /^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+((\.[a-zA-Z0-9_-]{2,3}){1,2})$/
        const currentLoginType = regex.test(formState.username) ? 0 : 1
        
        const loginParams = {
          [!currentLoginType ? 'email' : 'username']: formState.username,
          password: md5(formState.password),
          rememberMe: formState.rememberMe
        }
        
        Login(loginParams)
          .then((res) => loginSuccess(res))
          .catch(err => requestFailed(err))
          .finally(() => {
            state.loginBtn = false
          })
      } else {
        if (!formState.mobile || !/^1[34578]\d{9}$/.test(formState.mobile)) {
          notification.error({ message: '错误', description: '请输入正确的手机号' })
          state.loginBtn = false
          return
        }
        if (!formState.captcha) {
          notification.error({ message: '错误', description: '请输入验证码' })
          state.loginBtn = false
          return
        }
        
        const loginParams = {
          mobile: formState.mobile,
          captcha: formState.captcha,
          rememberMe: formState.rememberMe
        }
        
        Login(loginParams)
          .then((res) => loginSuccess(res))
          .catch(err => requestFailed(err))
          .finally(() => {
            state.loginBtn = false
          })
      }
    }
    
    const getCaptcha = (e) => {
      e.preventDefault()
      
      if (!formState.mobile || !/^1[34578]\d{9}$/.test(formState.mobile)) {
        notification.error({ message: '错误', description: '请输入正确的手机号' })
        return
      }
      
      state.smsSendBtn = true
      
      const interval = window.setInterval(() => {
        if (state.time-- <= 0) {
          state.time = 60
          state.smsSendBtn = false
          window.clearInterval(interval)
        }
      }, 1000)
      
      const hide = message.loading('验证码发送中..', 0)
      getSmsCaptcha({ mobile: formState.mobile }).then(res => {
        setTimeout(hide, 2500)
        notification.success({
          message: '提示',
          description: '验证码获取成功，您的验证码为：' + res.result.captcha,
          duration: 8
        })
      }).catch(err => {
        setTimeout(hide, 1)
        clearInterval(interval)
        state.time = 60
        state.smsSendBtn = false
        requestFailed(err)
      })
    }
    
    const stepCaptchaSuccess = () => {
      loginSuccess()
    }
    
    const stepCaptchaCancel = () => {
      Logout().then(() => {
        state.loginBtn = false
        stepCaptchaVisible.value = false
      })
    }
    
    const loginSuccess = (res) => {
      console.log(res)
      router.push({ path: '/' })
      setTimeout(() => {
        notification.success({
          message: '欢迎',
          description: `${timeFix()}，欢迎回来`
        })
      }, 1000)
      isLoginError.value = false
    }
    
    const requestFailed = (err) => {
      isLoginError.value = true
      notification.error({
        message: '错误',
        description: ((err.response || {}).data || {}).message || '请求出现错误，请稍后再试',
        duration: 4
      })
    }
    
    onMounted(() => {
      get2step({})
        .then(res => {
          requiredTwoStepCaptcha.value = res.result.stepCode
        })
        .catch(() => {
          requiredTwoStepCaptcha.value = false
        })
    })
    
    return {
      formRef,
      formState,
      state,
      customActiveKey,
      isLoginError,
      requiredTwoStepCaptcha,
      stepCaptchaVisible,
      loginType,
      handleTabClick,
      handleSubmit,
      getCaptcha,
      stepCaptchaSuccess,
      stepCaptchaCancel
    }
  }
}
</script>

<style lang="less" scoped>
.user-layout-login {
  label {
    font-size: 14px;
  }

  .getCaptcha {
    display: block;
    width: 100%;
    height: 40px;
  }

  .forge-password {
    font-size: 14px;
  }

  button.login-button {
    padding: 0 15px;
    font-size: 16px;
    height: 40px;
    width: 100%;
  }

  .user-login-other {
    text-align: left;
    margin-top: 24px;
    line-height: 22px;

    .item-icon {
      font-size: 24px;
      color: rgba(0, 0, 0, 0.2);
      margin-left: 16px;
      vertical-align: middle;
      cursor: pointer;
      transition: color 0.3s;

      &:hover {
        color: #1890ff;
      }
    }

    .register {
      float: right;
    }
  }
}
</style>