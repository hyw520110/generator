import Mock from 'mockjs2'
import { builder, getBody } from '../util'

const username = ['admin', 'user', 'super']
// md5('admin') = 21232f297a57a5a743894a0e4a801fc3
const password = ['21232f297a57a5a743894a0e4a801fc3']

const login = (options) => {
  // 登录 API 使用 data 发送 JSON body
  const body = getBody(options)
  
  console.log('[Mock] login body:', body)
  
  // 支持 userName 或 username
  const user = body?.userName || body?.username
  const pass = body?.password
  
  console.log('[Mock] user:', user, 'pass:', pass ? '***' : 'empty')
  
  if (!user || !pass || !username.includes(user) || !password.includes(pass)) {
    return builder({}, '账户或密码错误', 401)
  }

  // 返回结构需要匹配 store/modules/user.js 中 Login action 期望的格式
  return builder({
    'userToken': '4291d7da9005377ec9aec4a71ea837f',
    'userInfo': {
      'userId': '1',
      'name': 'Admin',
      'username': 'admin',
      'avatar': 'https://gw.alipayobjects.com/zos/rmsportal/jZUIxmJycoymBprLOUbT.png'
    }
  }, '', 200, { 'Custom-Header': Mock.mock('@guid') })
}

const logout = () => {
  return builder({}, '[测试接口] 注销成功')
}

const smsCaptcha = () => {
  return builder({ captcha: Mock.mock('@integer(10000, 99999)') })
}

const twofactor = () => {
  return builder({ stepCode: Mock.mock('@integer(0, 1)') })
}

const refreshUserAuthCache = () => {
  return builder({}, '刷新成功')
}

Mock.mock(/\/auth\/login/, 'put', login)
Mock.mock(/\/auth\/logout/, 'put', logout)
Mock.mock(/\/account\/sms/, 'post', smsCaptcha)
Mock.mock(/\/auth\/2step-code/, 'post', twofactor)
Mock.mock(/\/user\/authcache/, 'put', refreshUserAuthCache)

console.log('[Mock] auth 服务已注册')
