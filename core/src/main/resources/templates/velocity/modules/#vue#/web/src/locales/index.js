import { createI18n } from 'vue-i18n'
import * as storageModule from 'store'
import moment from 'moment'

// 获取 store 的默认导出
const storage = storageModule.default || storageModule

// default lang
import enUS from './lang/en-US.js'

export const defaultLang = 'en-US'

const messages = {
  'en-US': {
    ...enUS
  }
}

const i18n = createI18n({
  silentTranslationWarn: true,
  legacy: false,
  locale: defaultLang,
  fallbackLocale: defaultLang,
  messages
})

const loadedLanguages = [defaultLang]

function setI18nLanguage (lang) {
  i18n.global.locale.value = lang
  // request.headers['Accept-Language'] = lang
  document.querySelector('html').setAttribute('lang', lang)
  return lang
}

export function loadLanguageAsync (lang = defaultLang) {
  return new Promise(resolve => {
    // 缓存语言设置
    storage.set('lang', lang)
    if (i18n.global.locale.value !== lang) {
      if (!loadedLanguages.includes(lang)) {
        return import(/* @vite-ignore */ `./lang/${lang}.js`).then(msg => {
          const locale = msg.default
          i18n.global.setLocaleMessage(lang, locale)
          loadedLanguages.push(lang)
          moment.updateLocale(locale.momentName, locale.momentLocale)
          return setI18nLanguage(lang)
        })
      }
      return resolve(setI18nLanguage(lang))
    }
    return resolve(lang)
  })
}

export function i18nRender (key) {
  return i18n.global.t(`${key}`)
}

export default i18n