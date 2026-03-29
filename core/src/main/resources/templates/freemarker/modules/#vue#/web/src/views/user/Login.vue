<template>
  <div class="main">
    <a-form
      id="formLogin"
      class="user-layout-login"
      :model="formState"
      @finish="handleSubmit"
    >
      <a-tabs
        v-model:activeKey="customActiveKey"
        :tabBarStyle="{ textAlign: 'center', borderBottom: 'unset' }"
        @change="handleTabClick"
      >
        <a-tab-pane key="tab1" tab="账号密码登录">
          <a-form-item
            name="username"
            :rules="[{ required: true, message: '请输入帐户名' }]"
          >
            <a-input
              v-model:value="formState.username"
              size="large"
              type="text"
              placeholder="用户名"
            >
              <template #prefix>
                <UserOutlined :style="{ color: 'rgba(0,0,0,.25)' }" />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item
            name="password"
            :rules="[{ required: true, message: '请输入密码' }]"
          >
            <a-input-password
              v-model:value="formState.password"
              size="large"
              placeholder="密码"
            >
              <template #prefix>
                <LockOutlined :style="{ color: 'rgba(0,0,0,.25)' }" />
              </template>
            </a-input-password>
          </a-form-item>
        </a-tab-pane>
      </a-tabs>

      <a-form-item style="margin-top:24px">
        <a-button
          size="large"
          type="primary"
          html-type="submit"
          class="login-button"
          :loading="loginBtn"
          :disabled="loginBtn"
        >确定</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script>
import { defineComponent, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { notification } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import * as md5Module from 'md5'
import { timeFix } from '@/utils/util'

const md5 = md5Module.default || md5Module

export default defineComponent({
  name: 'Login',
  components: {
    UserOutlined,
    LockOutlined
  },
  setup () {
    const router = useRouter()
    const store = useStore()

    const customActiveKey = ref('tab1')
    const loginBtn = ref(false)

    const formState = reactive({
      username: '',
      password: ''
    })

    const handleTabClick = (key) => {
      customActiveKey.value = key
    }

    const handleSubmit = async () => {
      loginBtn.value = true

      try {
        const loginParams = {
          userName: formState.username,
          loginType: 'UPL',
          password: md5(formState.password)
        }

        await store.dispatch('Login', loginParams)

        router.push({ path: '/dashboard/workplace' }).catch(err => console.log('catch error:', err))

        setTimeout(() => {
          notification.success({
            message: '欢迎',
            description: `${timeFix()}，欢迎回来`
          })
        }, 1000)
      } catch (err) {
        notification.error({
          message: '错误',
          description: ((err.response || {}).data || {}).message || '请求出现错误，请稍后再试',
          duration: 4
        })
      } finally {
        loginBtn.value = false
      }
    }

    return {
      customActiveKey,
      loginBtn,
      formState,
      handleTabClick,
      handleSubmit
    }
  }
})
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