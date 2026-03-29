import * as storageModule from 'store'
import {
  SIDEBAR_TYPE,
  TOGGLE_MOBILE_TYPE,
  TOGGLE_NAV_THEME,
  TOGGLE_LAYOUT,
  TOGGLE_FIXED_HEADER,
  TOGGLE_FIXED_SIDEBAR,
  TOGGLE_CONTENT_WIDTH,
  TOGGLE_HIDE_HEADER,
  TOGGLE_COLOR,
  TOGGLE_WEAK,
  TOGGLE_MULTI_TAB,
  APP_LANGUAGE
} from '@/store/mutation-types'
import { loadLanguageAsync } from '@/locales'

// 获取 store 的默认导出
const storage = storageModule.default || storageModule

const app = {
  state: {
    sideCollapsed: false,
    sidebar: true,  // 侧边栏展开状态
    isMobile: false,
    device: 'desktop',  // 设备类型
    theme: 'dark',
    layout: 'sidemenu',
    contentWidth: 'Fluid',
    fixedHeader: false,
    fixedSidebar: true,
    autoHideHeader: false,
    color: '#1890FF',
    weak: false,
    multiTab: true,
    lang: 'zh-CN',
    _antLocale: {}
  },
  mutations: {
    [SIDEBAR_TYPE]: (state, type) => {
      state.sideCollapsed = type
      storage.set(SIDEBAR_TYPE, type)
    },
    [TOGGLE_MOBILE_TYPE]: (state, isMobile) => {
      state.isMobile = isMobile
    },
    [TOGGLE_NAV_THEME]: (state, theme) => {
      state.theme = theme
      storage.set(TOGGLE_NAV_THEME, theme)
    },
    [TOGGLE_LAYOUT]: (state, mode) => {
      state.layout = mode
      storage.set(TOGGLE_LAYOUT, mode)
    },
    [TOGGLE_FIXED_HEADER]: (state, mode) => {
      state.fixedHeader = mode
      storage.set(TOGGLE_FIXED_HEADER, mode)
    },
    [TOGGLE_FIXED_SIDEBAR]: (state, mode) => {
      state.fixedSidebar = mode
      storage.set(TOGGLE_FIXED_SIDEBAR, mode)
    },
    [TOGGLE_CONTENT_WIDTH]: (state, type) => {
      state.contentWidth = type
      storage.set(TOGGLE_CONTENT_WIDTH, type)
    },
    [TOGGLE_HIDE_HEADER]: (state, type) => {
      state.autoHideHeader = type
      storage.set(TOGGLE_HIDE_HEADER, type)
    },
    [TOGGLE_COLOR]: (state, color) => {
      state.color = color
      storage.set(TOGGLE_COLOR, color)
    },
    [TOGGLE_WEAK]: (state, mode) => {
      state.weak = mode
      storage.set(TOGGLE_WEAK, mode)
    },
    [APP_LANGUAGE]: (state, lang, antd = {}) => {
      state.lang = lang
      state._antLocale = antd
      storage.set(APP_LANGUAGE, lang)
    },
    [TOGGLE_MULTI_TAB]: (state, bool) => {
      storage.set(TOGGLE_MULTI_TAB, bool)
      state.multiTab = bool
    },
    TOGGLE_DEVICE: (state, device) => {
      state.device = device
    },
    SET_SIDEBAR: (state, sidebar) => {
      state.sidebar = sidebar
      state.sideCollapsed = !sidebar
    }
  },
  actions: {
    setSidebar ({ commit }, sidebar) {
      commit('SET_SIDEBAR', sidebar)
    },
    setLang ({ commit }, lang) {
      return new Promise((resolve, reject) => {
        commit(APP_LANGUAGE, lang)
        loadLanguageAsync(lang).then(() => {
          resolve()
        }).catch((e) => {
          reject(e)
        })
      })
    }
  }
}

export default app