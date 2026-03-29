<template>
  <a-card :bordered="false">
    <a-form :model="formState1" :self-update="true" @submit="handleForm1Submit" ref="form1Ref">
      <a-row class="form-row" :gutter="16">
        <a-col :lg="6" :md="12" :sm="24">
          <a-form-item label="仓库名" name="name">
            <a-input placeholder="请输入仓库名称" v-model:value="formState1.name"/>
          </a-form-item>
        </a-col>
        <a-col :xl="{span: 7, offset: 1}" :lg="{span: 8}" :md="{span: 12}" :sm="24">
          <a-form-item label="仓库域名" name="url">
            <a-input addonBefore="http://" addonAfter=".com" placeholder="请输入" v-model:value="formState1.url"/>
          </a-form-item>
        </a-col>
        <a-col :xl="{span: 9, offset: 1}" :lg="{span: 10}" :md="{span: 24}" :sm="24">
          <a-form-item label="仓库管理员" name="owner">
            <a-select placeholder="请选择管理员" v-model:value="formState1.owner">
              <a-select-option value="王同学">王同学</a-select-option>
              <a-select-option value="李同学">李同学</a-select-option>
              <a-select-option value="黄同学">黄同学</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row class="form-row" :gutter="16">
        <a-col :lg="6" :md="12" :sm="24">
          <a-form-item label="审批人" name="approver">
            <a-select placeholder="请选择审批员" v-model:value="formState1.approver">
              <a-select-option value="王晓丽">王晓丽</a-select-option>
              <a-select-option value="李军">李军</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xl="{span: 7, offset: 1}" :lg="{span: 8}" :md="{span: 12}" :sm="24">
          <a-form-item label="生效日期" name="dateRange">
            <a-range-picker style="width: 100%" v-model:value="formState1.dateRange"/>
          </a-form-item>
        </a-col>
        <a-col :xl="{span: 9, offset: 1}" :lg="{span: 10}" :md="{span: 24}" :sm="24">
          <a-form-item label="仓库类型" name="type">
            <a-select placeholder="请选择仓库类型" v-model:value="formState1.type">
              <a-select-option value="公开">公开</a-select-option>
              <a-select-option value="私密">私密</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item v-if="showSubmit">
        <a-button htmlType="submit">Submit</a-button>
      </a-form-item>
    </a-form>

    <a-divider/>

    <a-form :model="formState2" @submit="handleForm2Submit" ref="form2Ref">
      <a-row class="form-row" :gutter="16">
        <a-col :lg="6" :md="12" :sm="24">
          <a-form-item label="任务名" name="name">
            <a-input placeholder="请输入任务名称" v-model:value="formState2.name"/>
          </a-form-item>
        </a-col>
        <a-col :xl="{span: 7, offset: 1}" :lg="{span: 8}" :md="{span: 12}" :sm="24">
          <a-form-item label="任务描述" name="description">
            <a-input placeholder="请输入任务描述" v-model:value="formState2.description"/>
          </a-form-item>
        </a-col>
        <a-col :xl="{span: 9, offset: 1}" :lg="{span: 10}" :md="{span: 24}" :sm="24">
          <a-form-item label="执行人" name="owner">
            <a-select placeholder="请选择执行人" v-model:value="formState2.owner">
              <a-select-option value="黄丽丽">黄丽丽</a-select-option>
              <a-select-option value="李大刀">李大刀</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item v-if="showSubmit">
        <a-button htmlType="submit">Submit</a-button>
      </a-form-item>
    </a-form>

    <a-divider/>

    <a-form :model="formState3" @submit="handleForm3Submit" ref="form3Ref">
      <a-row class="form-row" :gutter="16">
        <a-col :lg="6" :md="12" :sm="24">
          <a-form-item label="项目名" name="name">
            <a-input placeholder="请输入项目名称" v-model:value="formState3.name"/>
          </a-form-item>
        </a-col>
        <a-col :xl="{span: 7, offset: 1}" :lg="{span: 8}" :md="{span: 12}" :sm="24">
          <a-form-item label="项目描述" name="description">
            <a-input placeholder="请输入项目描述" v-model:value="formState3.description"/>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item v-if="showSubmit">
        <a-button htmlType="submit">Submit</a-button>
      </a-form-item>
    </a-form>

    <a-divider/>

    <a-form :model="formState4" @submit="handleForm4Submit" ref="form4Ref">
      <a-row class="form-row" :gutter="16">
        <a-col :lg="6" :md="12" :sm="24">
          <a-form-item label="部门名" name="name">
            <a-input placeholder="请输入部门名称" v-model:value="formState4.name"/>
          </a-form-item>
        </a-col>
        <a-col :xl="{span: 7, offset: 1}" :lg="{span: 8}" :md="{span: 12}" :sm="24">
          <a-form-item label="部门描述" name="description">
            <a-input placeholder="请输入部门描述" v-model:value="formState4.description"/>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item v-if="showSubmit">
        <a-button htmlType="submit">Submit</a-button>
      </a-form-item>
    </a-form>

    <a-divider/>

    <a-form :model="formState5" @submit="handleForm5Submit" ref="form5Ref">
      <a-row class="form-row" :gutter="16">
        <a-col :lg="6" :md="12" :sm="24">
          <a-form-item label="团队名" name="name">
            <a-input placeholder="请输入团队名称" v-model:value="formState5.name"/>
          </a-form-item>
        </a-col>
        <a-col :xl="{span: 7, offset: 1}" :lg="{span: 8}" :md="{span: 12}" :sm="24">
          <a-form-item label="团队描述" name="description">
            <a-input placeholder="请输入团队描述" v-model:value="formState5.description"/>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item v-if="showSubmit">
        <a-button htmlType="submit">Submit</a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script>
import { ref, reactive } from 'vue'

export default {
  name: 'BigForm',
  props: {
    showSubmit: {
      type: Boolean,
      default: false
    }
  },
  setup () {
    const form1Ref = ref()
    const form2Ref = ref()
    const form3Ref = ref()
    const form4Ref = ref()
    const form5Ref = ref()
    
    const formState1 = reactive({
      name: '',
      url: '',
      owner: undefined,
      approver: undefined,
      dateRange: null,
      type: undefined
    })
    
    const formState2 = reactive({
      name: '',
      description: '',
      owner: undefined
    })
    
    const formState3 = reactive({
      name: '',
      description: ''
    })
    
    const formState4 = reactive({
      name: '',
      description: ''
    })
    
    const formState5 = reactive({
      name: '',
      description: ''
    })
    
    const handleForm1Submit = (e) => {
      e.preventDefault()
      console.log('Form 1 values:', formState1)
    }
    
    const handleForm2Submit = (e) => {
      e.preventDefault()
      console.log('Form 2 values:', formState2)
    }
    
    const handleForm3Submit = (e) => {
      e.preventDefault()
      console.log('Form 3 values:', formState3)
    }
    
    const handleForm4Submit = (e) => {
      e.preventDefault()
      console.log('Form 4 values:', formState4)
    }
    
    const handleForm5Submit = (e) => {
      e.preventDefault()
      console.log('Form 5 values:', formState5)
    }
    
    return {
      form1Ref,
      form2Ref,
      form3Ref,
      form4Ref,
      form5Ref,
      formState1,
      formState2,
      formState3,
      formState4,
      formState5,
      handleForm1Submit,
      handleForm2Submit,
      handleForm3Submit,
      handleForm4Submit,
      handleForm5Submit
    }
  }
}
</script>

<style scoped>
.form-row {
  margin-bottom: 16px;
}
</style>