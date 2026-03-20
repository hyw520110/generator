<template>
  <div class="main user-layout-register">
    <h3><span>注册</span></h3>
    <a-form ref="formRef" :model="formState" id="formRegister">
      <a-form-item name="email">
        <a-input
          size="large"
          type="text"
          placeholder="邮箱"
          v-model:value="formState.email"
        />
      </a-form-item>

      <a-popover
        placement="rightTop"
        :trigger="['focus']"
        :getPopupContainer="(trigger) => trigger.parentElement"
        v-model:open="state.passwordLevelChecked">
        <template #content>
          <div :style="{ width: '240px' }">
            <div :class="['user-register', passwordLevelClass]">强度：<span>{{ passwordLevelName }}</span></div>
            <a-progress :percent="state.percent" :showInfo="false" :strokeColor="passwordLevelColor"/>
            <div style="margin-top: 10px;">
              <span>请至少输入 6 个字符。请不要使用容易被猜到的密码。</span>
            </div>
          </div>
        </template>
        <a-form-item name="password">
          <a-input-password
            size="large"
            @click="handlePasswordInputClick"
            placeholder="至少6位密码，区分大小写"
            v-model:value="formState.password"
          />
        </a-form-item>
      </a-popover>

      <a-form-item name="password2">
        <a-input-password
          size="large"
          placeholder="确认密码"
          v-model:value="formState.password2"
        />
      </a-form-item>

      <a-form-item name="mobile">
        <a-input size="large" placeholder="11 位手机号" v-model:value="formState.mobile">
          <template #addonBefore>
            <a-select v-model:value="formState.prefix" size="large" style="width: 80px">
              <a-select-option value="+86">+86</a-select-option>
              <a-select-option value="+87">+87</a-select-option>
            </a-select>
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
            size="large"
            :disabled="state.smsSendBtn"
            @click.stop.prevent="getCaptcha"
          >{{ !state.smsSendBtn ? '获取验证码' : (state.time + ' s') }}</a-button>
        </a-col>
      </a-row>

      <a-form-item>
        <a-button
          size="large"
          type="primary"
          htmlType="submit"
          class="register-button"
          :loading="registerBtn"
          @click.stop.prevent="handleSubmit"
          :disabled="registerBtn">注册
        </a-button>
        <router-link class="login" :to="{ name: 'login' }">使用已有账户登录</router-link>
      </a-form-item>
    </a-form>
  </div>
</template>

<script>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { notification, message } from 'ant-design-vue'
import { MailOutlined } from '@ant-design/icons-vue'
import { getSmsCaptcha } from '@/api/login'

const levelNames = {
  0: '低',
  1: '低',
  2: '中',
  3: '强'
}
const levelClass = {
  0: 'error',
  1: 'error',
  2: 'warning',
  3: 'success'
}
const levelColor = {
  0: '#ff0000',
  1: '#ff0000',
  2: '#ff7e05',
  3: '#52c41a'
}

export default {
  name: 'Register',
  components: {
    MailOutlined
  },
  setup () {
    const router = useRouter()
    const formRef = ref()
    const registerBtn = ref(false)
    
    const state = reactive({
      time: 60,
      smsSendBtn: false,
      passwordLevel: 0,
      passwordLevelChecked: false,
      percent: 10,
      progressColor: '#FF0000'
    })
    
    const formState = reactive({
      email: '',
      password: '',
      password2: '',
      mobile: '',
      captcha: '',
      prefix: '+86'
    })
    
    const passwordLevelClass = computed(() => levelClass[state.passwordLevel])
    const passwordLevelName = computed(() => levelNames[state.passwordLevel])
    const passwordLevelColor = computed(() => levelColor[state.passwordLevel])
    
    const handlePasswordLevel = (value) => {
      let level = 0
      if (/[0-9]/.test(value)) level++
      if (/[a-zA-Z]/.test(value)) level++
      if (/[^0-9a-zA-Z_]/.test(value)) level++
      
      state.passwordLevel = level
      state.percent = level * 30
      if (level >= 3) state.percent = 100
      
      return level >= 2
    }
    
    const handlePasswordInputClick = () => {
      state.passwordLevelChecked = true
    }
    
    const handleSubmit = async () => {
      if (!formState.email) {
        notification.error({ message: '错误', description: '请输入邮箱地址' })
        return
      }
      if (!handlePasswordLevel(formState.password)) {
        notification.error({ message: '错误', description: '密码强度不够' })
        return
      }
      if (formState.password !== formState.password2) {
        notification.error({ message: '错误', description: '两次密码不一致' })
        return
      }
      if (!formState.mobile || !/^1[3456789]\d{9}$/.test(formState.mobile)) {
        notification.error({ message: '错误', description: '请输入正确的手机号' })
        return
      }
      if (!formState.captcha) {
        notification.error({ message: '错误', description: '请输入验证码' })
        return
      }
      
      state.passwordLevelChecked = false
      router.push({ name: 'registerResult', params: { ...formState } })
    }
    
    const getCaptcha = (e) => {
      e.preventDefault()
      
      if (!formState.mobile || !/^1[3456789]\d{9}$/.test(formState.mobile)) {
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
    
    const requestFailed = (err) => {
      notification.error({
        message: '错误',
        description: ((err.response || {}).data || {}).message || '请求出现错误，请稍后再试',
        duration: 4
      })
      registerBtn.value = false
    }
    
    return {
      formRef,
      formState,
      state,
      registerBtn,
      passwordLevelClass,
      passwordLevelName,
      passwordLevelColor,
      handlePasswordInputClick,
      handleSubmit,
      getCaptcha
    }
  }
}
</script>

<style lang="less">
.user-register {
  &.error {
    color: #ff0000;
  }

  &.warning {
    color: #ff7e05;
  }

  &.success {
    color: #52c41a;
  }
}

.user-layout-register {
  .ant-input-group-addon:first-child {
    background-color: #fff;
  }
}
</style>

<style lang="less" scoped>
.user-layout-register {
  & > h3 {
    font-size: 16px;
    margin-bottom: 20px;
  }

  .getCaptcha {
    display: block;
    width: 100%;
    height: 40px;
  }

  .register-button {
    width: 50%;
  }

  .login {
    float: right;
    line-height: 40px;
  }
}
</style>