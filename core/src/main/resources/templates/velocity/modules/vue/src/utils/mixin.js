// import Vue from 'vue'
import { computed } from 'vue'
import { useStore } from 'vuex'
import { deviceEnquire, DEVICE_TYPE } from '@/utils/device'
import { mapState } from 'vuex'

// const mixinsComputed = Vue.config.optionMergeStrategies.computed
// const mixinsMethods = Vue.config.optionMergeStrategies.methods

// Vue 3 Composable for app state
export function useAppMixin () {
  const store = useStore()

  const layoutMode = computed(() => store.state.app.layout)
  const navTheme = computed(() => store.state.app.theme)
  const primaryColor = computed(() => store.state.app.color)
  const colorWeak = computed(() => store.state.app.weak)
  const fixedHeader = computed(() => store.state.app.fixedHeader)
  const fixSiderbar = computed(() => store.state.app.fixSiderbar)
  const fixSidebar = computed(() => store.state.app.fixSiderbar)
  const contentWidth = computed(() => store.state.app.contentWidth)
  const autoHideHeader = computed(() => store.state.app.autoHideHeader)
  const sidebarOpened = computed(() => store.state.app.sidebar)
  const multiTab = computed(() => store.state.app.multiTab)

  const isTopMenu = computed(() => layoutMode.value === 'topmenu')
  const isSideMenu = computed(() => !isTopMenu.value)
  
  const device = computed(() => store.state.app.device)
  const isMobile = computed(() => device.value === DEVICE_TYPE.MOBILE)
  const isDesktop = computed(() => device.value === DEVICE_TYPE.DESKTOP)
  const isTablet = computed(() => device.value === DEVICE_TYPE.TABLET)

  return {
    layoutMode,
    navTheme,
    primaryColor,
    colorWeak,
    fixedHeader,
    fixSiderbar,
    fixSidebar,
    contentWidth,
    autoHideHeader,
    sidebarOpened,
    multiTab,
    isTopMenu,
    isSideMenu,
    device,
    isMobile,
    isDesktop,
    isTablet
  }
}

export function useDeviceMixin () {
  const store = useStore()

  const device = computed(() => store.state.app.device)
  const isMobile = computed(() => device.value === DEVICE_TYPE.MOBILE)
  const isDesktop = computed(() => device.value === DEVICE_TYPE.DESKTOP)
  const isTablet = computed(() => device.value === DEVICE_TYPE.TABLET)

  return {
    device,
    isMobile,
    isDesktop,
    isTablet
  }
}

// Vue 2 Options API mixins (for backward compatibility)
const mixin = {
  computed: {
    ...mapState({
      layoutMode: state => state.app.layout,
      navTheme: state => state.app.theme,
      primaryColor: state => state.app.color,
      colorWeak: state => state.app.weak,
      fixedHeader: state => state.app.fixedHeader,
      fixSiderbar: state => state.app.fixSiderbar,
      fixSidebar: state => state.app.fixSiderbar,
      contentWidth: state => state.app.contentWidth,
      autoHideHeader: state => state.app.autoHideHeader,
      sidebarOpened: state => state.app.sidebar,
      multiTab: state => state.app.multiTab
    })
  },
  methods: {
    isTopMenu () {
      return this.layoutMode === 'topmenu'
    },
    isSideMenu () {
      return !this.isTopMenu()
    }
  }
}

const mixinDevice = {
  computed: {
    ...mapState({
      device: state => state.app.device
    })
  },
  methods: {
    isMobile () {
      return this.device === DEVICE_TYPE.MOBILE
    },
    isDesktop () {
      return this.device === DEVICE_TYPE.DESKTOP
    },
    isTablet () {
      return this.device === DEVICE_TYPE.TABLET
    }
  }
}

const AppDeviceEnquire = {
  mounted () {
    const { $store } = this
    deviceEnquire(deviceType => {
      switch (deviceType) {
        case DEVICE_TYPE.DESKTOP:
          $store.commit('TOGGLE_DEVICE', 'desktop')
          $store.dispatch('setSidebar', true)
          break
        case DEVICE_TYPE.TABLET:
          $store.commit('TOGGLE_DEVICE', 'tablet')
          $store.dispatch('setSidebar', false)
          break
        case DEVICE_TYPE.MOBILE:
        default:
          $store.commit('TOGGLE_DEVICE', 'mobile')
          $store.dispatch('setSidebar', true)
          break
      }
    })
  }
}

export { mixin, AppDeviceEnquire, mixinDevice }