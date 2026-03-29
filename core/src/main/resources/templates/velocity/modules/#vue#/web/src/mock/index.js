import Mock from 'mockjs2'

// Mock 服务 - 仅在开发环境有效
// 导入所有 mock 服务（立即注册 Mock 拦截器）
import './services/auth'
import './services/user'
import './services/manage'
import './services/other'
import './services/tagCloud'
import './services/article'
import './services/resource'

// 开发环境下设置 Mock 延迟
if (import.meta.env.DEV) {
  Mock.setup({
    timeout: 200
  })
  console.log('[Mock] 服务已启动')
}