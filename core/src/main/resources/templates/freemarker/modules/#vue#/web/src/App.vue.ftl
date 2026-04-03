<template>
  <a-config-provider :locale="locale">
    <div id="app">
      <router-view/>
    </div>
  </a-config-provider>
</template>

<script>
import { domTitle, setDocumentTitle } from '@/utils/domUtil'
import { i18nRender } from '@/locales'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'

export default {
  setup () {
    const { getLocaleMessage } = useI18n()
    const store = useStore()
    
    return {
      getLocaleMessage,
      store
    }
  },
  computed: {
    locale () {
      // 只是为了切换语言时，更新标题
      const { title } = this.$route.meta
      title && (setDocumentTitle(`${r"${i18nRender(title)}"} - ${r"${domTitle}"}`))

      const lang = this.store?.getters?.lang || 'en-US'
      const messages = this.getLocaleMessage(lang)
      return messages?.antLocale
    }
  }
}
</script>
