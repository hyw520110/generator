<template>
  <div class="home">
    <section class="summary">
      <div>
        <p class="eyebrow">Generated Project</p>
        <h1>${projectName!}</h1>
        <p class="description">${description!'后台管理系统'}</p>
      </div>
      <div class="status">
        <span>Java ${javaVersion!}</span>
        <span>${templateFamily!'boot2'}</span>
        <span>${security!'NONE'}</span>
      </div>
    </section>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :md="8" v-for="item in metrics" :key="item.label">
        <a-card :bordered="false">
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-label">{{ item.label }}</div>
        </a-card>
      </a-col>
    </a-row>

    <a-card :bordered="false" title="技术栈" class="panel">
      <a-descriptions :column="{ xs: 1, md: 2 }" size="small">
        <a-descriptions-item label="后端">Spring Boot ${springboot_version!'未配置'}</a-descriptions-item>
        <a-descriptions-item label="安全方案">${security!'NONE'}</a-descriptions-item>
        <a-descriptions-item label="命名空间">${namespace!'javax'}</a-descriptions-item>
        <a-descriptions-item label="构建工具">${projectBuilder!'MAVEN'}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card :bordered="false" title="快捷入口" class="panel">
      <a-space wrap>
        <a-button type="primary" href="/swagger-ui/index.html">接口文档</a-button>
        <a-button href="/actuator/health">健康检查</a-button>
        <a-button href="/api/auth/login">登录接口</a-button>
      </a-space>
    </a-card>
  </div>
</template>

<script>
export default {
  name: 'Home',
  data () {
    return {
      metrics: [
        { label: '模块数量', value: '${(modules?size)!0}' },
        { label: '模板代际', value: '${templateFamily!'boot2'}' },
        { label: '字节码版本', value: '${bytecodeRelease!javaVersion!}' }
      ]
    }
  }
}
</script>

<style scoped>
.home {
  max-width: 1120px;
  margin: 0 auto;
  padding: 24px;
}
.summary {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
  margin-bottom: 20px;
}
.eyebrow {
  margin: 0 0 6px;
  color: #64748b;
  font-size: 12px;
  text-transform: uppercase;
}
h1 {
  margin: 0;
  font-size: 28px;
}
.description {
  margin: 8px 0 0;
  color: #475569;
}
.status {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}
.status span {
  padding: 4px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
}
.metric-value {
  font-size: 24px;
  font-weight: 600;
}
.metric-label {
  margin-top: 4px;
  color: #64748b;
}
.panel {
  margin-top: 16px;
}
</style>
