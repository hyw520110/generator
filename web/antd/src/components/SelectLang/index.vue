<template>
  <a-dropdown :placement="'bottomRight'">
    <span :class="prefixCls">
      <global-outlined :title="i18nRender('navBar.lang')" />
    </span>
    <template #overlay>
      <a-menu class="menu ant-pro-header-menu" :selectedKeys="[currentLang]" @click="changeLang">
        <a-menu-item v-for="locale in locales" :key="locale">
          <span role="img" :aria-label="languageLabels[locale]">
            {{ languageIcons[locale] }}
          </span>
          {{ ' ' + languageLabels[locale] }}
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script>
import { GlobalOutlined } from '@ant-design/icons-vue'
import { i18nRender } from '@/locales'
import i18nMixin from '@/store/i18n-mixin'

const locales = ['zh-CN', 'zh-TW', 'en-US', 'pt-BR']
const languageLabels = {
  'zh-CN': '简体中文',
  'zh-TW': '繁体中文',
  'en-US': 'English',
  'pt-BR': 'Português'
}
const languageIcons = {
  'zh-CN': '🇨🇳',
  'zh-TW': '🇭🇰',
  'en-US': '🇺🇸',
  'pt-BR': '🇧🇷'
}

export default {
  name: 'SelectLang',
  components: {
    GlobalOutlined
  },
  mixins: [i18nMixin],
  props: {
    prefixCls: {
      type: String,
      default: 'ant-pro-drop-down'
    }
  },
  data () {
    return {
      locales,
      languageLabels,
      languageIcons
    }
  },
  methods: {
    i18nRender,
    changeLang ({ key }) {
      this.setLang(key)
    }
  }
}
</script>
