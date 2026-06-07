<template>
  <page-view :avatar="avatar" :title="false">
    <div slot="headerContent">
      <div class="title">{{ timeFix }}，{{ displayName }}，欢迎使用 {{ projectName }}</div>
      <div>{{ projectDescription }}</div>
    </div>
    <div slot="extra">
      <a-row class="more-info">
        <a-col :span="8">
          <head-info title="工程版本" :content="projectVersion" :center="false" :bordered="false" />
        </a-col>
        <a-col :span="8">
          <head-info title="Java 版本" :content="javaVersion" :center="false" :bordered="false" />
        </a-col>
        <a-col :span="8">
          <head-info title="根包名" :content="rootPackage" :center="false" />
        </a-col>
      </a-row>
    </div>

    <a-row :gutter="24">
      <a-col :xl="16" :lg="24" :md="24" :sm="24" :xs="24">
        <a-card title="工程概览" class="work-card" :bordered="false">
          <a-row :gutter="16">
            <a-col :md="8" :sm="24" :xs="24" v-for="item in overview" :key="item.title">
              <div class="overview-item">
                <a-icon :type="item.icon" class="overview-icon" />
                <div>
                  <div class="overview-title">{{ item.title }}</div>
                  <div class="overview-desc">{{ item.description }}</div>
                </div>
              </div>
            </a-col>
          </a-row>
        </a-card>

        <a-card title="核心能力" class="work-card" :bordered="false">
          <a-list :data-source="capabilities">
            <a-list-item slot="renderItem" slot-scope="item">
              <a-list-item-meta>
                <a-icon slot="avatar" :type="item.icon" class="list-icon" />
                <span slot="title">{{ item.title }}</span>
                <span slot="description">{{ item.description }}</span>
              </a-list-item-meta>
              <a-tag :color="item.statusColor">{{ item.status }}</a-tag>
            </a-list-item>
          </a-list>
        </a-card>

        <a-card title="下一步建议" class="work-card" :bordered="false">
          <a-timeline>
            <a-timeline-item v-for="item in nextSteps" :key="item.title" :color="item.color">
              <div class="timeline-title">{{ item.title }}</div>
              <div class="timeline-desc">{{ item.description }}</div>
            </a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>

      <a-col :xl="8" :lg="24" :md="24" :sm="24" :xs="24">
        <a-card title="运行检查" class="work-card" :bordered="false">
          <a-list :data-source="runtimeChecks" size="small">
            <a-list-item slot="renderItem" slot-scope="item">
              <span>
                <a-icon :type="item.icon" :class="['check-icon', item.type]" />
                {{ item.title }}
              </span>
              <span class="check-desc">{{ item.description }}</span>
            </a-list-item>
          </a-list>
        </a-card>

        <a-card title="模块入口" class="work-card" :bordered="false" :body-style="{ padding: '16px 24px' }">
          <div class="quick-actions">
            <a-button
              v-for="item in quickActions"
              :key="item.title"
              :type="item.primary ? 'primary' : 'default'"
              :icon="item.icon"
              block
              @click="go(item.path)">
              {{ item.title }}
            </a-button>
          </div>
        </a-card>

        <a-card title="交付清单" class="work-card" :bordered="false">
          <a-list :data-source="deliveryItems" size="small">
            <a-list-item slot="renderItem" slot-scope="item">
              <a-list-item-meta>
                <a-icon slot="avatar" type="check-circle" class="delivery-icon" />
                <span slot="title">{{ item.title }}</span>
                <span slot="description">{{ item.description }}</span>
              </a-list-item-meta>
            </a-list-item>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </page-view>
</template>

<script>
import { timeFix } from '@/utils/util'
import { PageView } from '@/layouts'
import HeadInfo from '@/components/tools/HeadInfo'

export default {
  name: 'Workplace',
  components: {
    PageView,
    HeadInfo
  },
  data () {
    return {
      timeFix: timeFix(),
      avatar: '',
      projectName: '$!projectName' || '后台管理系统',
      projectDescription: '$!description' || '当前工程已完成基础后台骨架、权限入口、接口契约和常用运维清单，可从这里继续完善业务模块。',
      projectVersion: '$!version' || '1.0.0',
      javaVersion: '$!javaVersion' || '-',
      rootPackage: '$!rootPackage' || '-',
      overview: [
        { icon: 'appstore', title: '业务后台', description: '已生成前后端基础工程和常用管理入口' },
        { icon: 'api', title: '接口契约', description: '统一返回结构、分页结果和异常处理已预置' },
        { icon: 'safety-certificate', title: '权限基础', description: '登录认证、菜单权限和接口拦截可继续扩展' }
      ],
      capabilities: [
        { icon: 'database', title: 'CRUD 代码', description: '根据数据库表生成实体、DTO、服务、控制器和页面模板。', status: '可用', statusColor: 'green' },
        { icon: 'profile', title: '接口文档', description: '后端工程预留 OpenAPI / Knife4j 配置，便于联调和验收。', status: '待配置', statusColor: 'blue' },
        { icon: 'lock', title: '认证鉴权', description: 'JWT / Shiro 等组件按生成配置启用，建议替换默认密钥。', status: '需核对', statusColor: 'orange' },
        { icon: 'deployment-unit', title: '部署交付', description: '通过构建脚本产出后端包和前端静态资源，再按环境发布。', status: '建议执行', statusColor: 'purple' }
      ],
      runtimeChecks: [
        { icon: 'check-circle', type: 'ok', title: '后端服务', description: '确认应用端口、运行 profile 和日志目录' },
        { icon: 'check-circle', type: 'ok', title: '数据库连接', description: '确认数据源、账号权限和初始化数据' },
        { icon: 'exclamation-circle', type: 'warn', title: '认证配置', description: '替换默认账号、默认密码和 token 密钥' },
        { icon: 'exclamation-circle', type: 'warn', title: '前端代理', description: '确认 API baseURL 与部署路径一致' }
      ],
      deliveryItems: [
        { title: 'README', description: '记录工程结构、启动方式和常用命令。' },
        { title: '配置文件', description: '按 dev / test / prod 环境拆分并外置敏感信息。' },
        { title: '构建脚本', description: '使用 package.sh 或 CI 生成可部署产物。' },
        { title: '验收清单', description: '完成登录、菜单、CRUD、接口文档和部署健康检查。' }
      ],
      nextSteps: [
        { title: '完善环境配置', description: '补齐数据库、缓存、消息队列和第三方服务配置。', color: 'blue' },
        { title: '替换默认安全项', description: '修改默认账号、密码、JWT 密钥和跨域白名单。', color: 'orange' },
        { title: '验证核心链路', description: '按登录、菜单、列表、新增、编辑、删除顺序做一次冒烟验证。', color: 'green' },
        { title: '准备部署产物', description: '构建后端包和前端 dist，补齐启动、停止、健康检查脚本。', color: 'gray' }
      ],
      quickActions: [
        { icon: 'dashboard', title: '工作台', path: '/dashboard/workplace', primary: true },
        { icon: 'line-chart', title: '分析页', path: '/dashboard/analysis' },
        { icon: 'setting', title: '个人设置', path: '/account/settings' }
      ]
    }
  },
  computed: {
#[[
    userInfo () {
      return this.$store.getters.userInfo || {}
    },
    displayName () {
      return this.userInfo.name || this.userInfo.userName || this.userInfo.nickname || '管理员'
    }
]]#
  },
  created () {
    this.avatar = this.userInfo.avatar || ''
  },
  methods: {
#[[
    go (path) {
      if (!path) {
        return
      }
      var route = this.$router.push(path)
      if (route && route.catch) {
        route.catch(this.ignoreNavigationError)
      }
    },
    ignoreNavigationError () {
    }
]]#
  }
}
</script>

<style lang="less" scoped>
.title {
  color: rgba(0, 0, 0, 0.85);
  font-size: 20px;
  line-height: 28px;
  font-weight: 500;
  margin-bottom: 8px;
}

.more-info {
  min-width: 420px;
}

.work-card {
  margin-bottom: 24px;
}

.overview-item {
  display: flex;
  min-height: 112px;
  padding: 20px;
  margin-bottom: 16px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}

.overview-icon {
  flex: 0 0 auto;
  margin-right: 16px;
  color: #1890ff;
  font-size: 28px;
  line-height: 32px;
}

.overview-title {
  color: rgba(0, 0, 0, 0.85);
  font-size: 16px;
  line-height: 24px;
  font-weight: 500;
}

.overview-desc,
.timeline-desc,
.check-desc {
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
  line-height: 22px;
}

.list-icon,
.delivery-icon {
  color: #1890ff;
  font-size: 20px;
}

.check-icon {
  margin-right: 8px;

  &.ok {
    color: #52c41a;
  }

  &.warn {
    color: #faad14;
  }
}

.timeline-title {
  color: rgba(0, 0, 0, 0.85);
  font-weight: 500;
}

.quick-actions {
  display: grid;
  grid-template-columns: 1fr;
  grid-row-gap: 12px;
}

.mobile {
  .more-info {
    min-width: 0;
    padding-top: 16px;
  }

  .overview-item {
    min-height: auto;
  }
}
</style>
