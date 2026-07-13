<template>
  <a-card :bordered="false">
    <a-steps class="steps" :current="currentTab">
      <a-step title="全局设置" />
      <a-step title="组件选择" />
      <a-step title="数据源&代码生成" />
    </a-steps>
    <div class="content">
      <step1 v-if="currentTab === 0" @nextStep="nextStep"/>
      <step2 v-if="currentTab === 1" @nextStep="nextStep" @prevStep="prevStep"/>
      <step3 v-if="currentTab === 2" @prevStep="prevStep" @finish="finish"/>
    </div>
  </a-card>
</template>

<script>
import Step1 from './Step1.vue'
import Step2 from './Step2.vue'
import Step3 from './Step3.vue'

/** 生成向导当前步骤的会话存储键。 */
const GENERATOR_CURRENT_STEP_KEY = 'generator.currentStep'
/** 第 2 步表单的会话草稿存储键。 */
const STEP2_DRAFT_KEY = 'generator.step2Draft'
/** 生成向导的首个步骤。 */
const FIRST_STEP = 0
/** 生成向导的最后步骤。 */
const LAST_STEP = 2

export default {
  name: 'StepForm',
  components: {
    Step1,
    Step2,
    Step3
  },
  data () {
    return {
      description: '设置生成器所需配置后，执行代码生成',
      currentTab: this.restoreCurrentStep(),

      // form
      form: null
    }
  },
  methods: {

    restoreCurrentStep () {
      try {
        const savedStep = Number.parseInt(sessionStorage.getItem(GENERATOR_CURRENT_STEP_KEY), 10)
        return Number.isInteger(savedStep) && savedStep >= FIRST_STEP && savedStep <= LAST_STEP
          ? savedStep
          : FIRST_STEP
      } catch (error) {
        console.warn('读取生成向导当前步骤失败:', error)
        return FIRST_STEP
      }
    },

    saveCurrentStep () {
      try {
        sessionStorage.setItem(GENERATOR_CURRENT_STEP_KEY, String(this.currentTab))
      } catch (error) {
        console.warn('保存生成向导当前步骤失败:', error)
      }
    },

    // handler
    nextStep () {
      if (this.currentTab < LAST_STEP) {
        this.currentTab += 1
        this.saveCurrentStep()
      }
    },
    prevStep () {
      if (this.currentTab > FIRST_STEP) {
        this.currentTab -= 1
        this.saveCurrentStep()
      }
    },
    finish () {
      this.currentTab = FIRST_STEP
      this.saveCurrentStep()
      try {
        sessionStorage.removeItem(STEP2_DRAFT_KEY)
      } catch (error) {
        console.warn('清理第2步配置草稿失败:', error)
      }
    }
  }
}
</script>

<style lang="less" scoped>
  .steps {
    max-width: 750px;
    margin: 16px auto;
  }
</style>
