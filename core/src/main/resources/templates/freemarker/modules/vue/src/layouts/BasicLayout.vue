<template>
  <a-layout :class="['layout', device]">
    <!-- SideMenu -->
    <a-drawer
      v-if="isMobile"
      placement="left"
      :wrapClassName="`drawer-sider ${navTheme}`"
      :closable="false"
      :open="collapsed"
      @close="drawerClose"
    >
      <side-menu
        mode="inline"
        :menus="menus"
        :theme="navTheme"
        :collapsed="false"
        :collapsible="true"
        @menuSelect="menuSelect"
      ></side-menu>
    </a-drawer>

    <side-menu
      v-else-if="isSideMenu"
      mode="inline"
      :menus="menus"
      :theme="navTheme"
      :collapsed="collapsed"
      :collapsible="true"
    ></side-menu>

    <a-layout :class="[layoutMode, `content-width-${contentWidth}`]" :style="{ paddingLeft: contentPaddingLeft, minHeight: '100vh' }">
      <!-- layout header -->
      <global-header
        :mode="layoutMode"
        :menus="menus"
        :theme="navTheme"
        :collapsed="collapsed"
        :device="device"
        @toggle="toggle"
      />

      <!-- layout content -->
      <a-layout-content :style="{ height: '100%', margin: '24px 24px 0', paddingTop: fixedHeader ? '64px' : '0' }">
        <multi-tab v-if="multiTab"></multi-tab>
        <transition name="page-transition">
          <route-view />
        </transition>
      </a-layout-content>

      <!-- layout footer -->
      <a-layout-footer>
        <global-footer />
      </a-layout-footer>

      <!-- Setting Drawer (show in development mode) -->
      <setting-drawer v-if="!production"></setting-drawer>
    </a-layout>
  </a-layout>

</template>

<script>
import { defineComponent, ref, computed, watch, onMounted, nextTick } from 'vue'
import { useStore } from 'vuex'
import { triggerWindowResizeEvent } from '@/utils/util'
import { useAppMixin, useDeviceMixin } from '@/utils/mixin'
import config from '@/config/defaultSettings'

import RouteView from './RouteView.vue'
import MultiTab from '@/components/MultiTab'
import SideMenu from '@/components/Menu/SideMenu'
import GlobalHeader from '@/components/GlobalHeader'
import GlobalFooter from '@/components/GlobalFooter'
import SettingDrawer from '@/components/SettingDrawer'

export default defineComponent({
  name: 'BasicLayout',
  components: {
    RouteView,
    MultiTab,
    SideMenu,
    GlobalHeader,
    GlobalFooter,
    SettingDrawer
  },
  setup () {
    const store = useStore()
    const { isMobile, isSideMenu, device, sidebarOpened, fixSidebar, layoutMode, contentWidth, navTheme, fixedHeader, multiTab } = useAppMixin()

    const production = config.production
    const collapsed = ref(false)

    const mainMenu = computed(() => store.state.permission.addRouters)
    
    const menus = computed(() => {
      const rootRoute = mainMenu.value.find(item => item.path === '/')
      return rootRoute?.children || []
    })

    const contentPaddingLeft = computed(() => {
      if (!fixSidebar.value || isMobile.value) {
        return '0'
      }
      if (sidebarOpened.value) {
        return '256px'
      }
      return '80px'
    })

    watch(sidebarOpened, (val) => {
      collapsed.value = !val
    })

    onMounted(() => {
      collapsed.value = !sidebarOpened.value
      
      const userAgent = navigator.userAgent
      if (userAgent.indexOf('Edge') > -1) {
        nextTick(() => {
          collapsed.value = !collapsed.value
          setTimeout(() => {
            collapsed.value = !collapsed.value
          }, 16)
        })
      }
    })

    const toggle = () => {
      collapsed.value = !collapsed.value
      store.dispatch('setSidebar', !collapsed.value)
      triggerWindowResizeEvent()
    }

    const menuSelect = () => {
      if (device.value !== 'desktop') {
        collapsed.value = false
      }
    }

    const drawerClose = () => {
      collapsed.value = false
    }

    return {
      production,
      collapsed,
      menus,
      contentPaddingLeft,
      isMobile,
      isSideMenu,
      device,
      sidebarOpened,
      fixSidebar,
      layoutMode,
      contentWidth,
      navTheme,
      fixedHeader,
      multiTab,
      toggle,
      menuSelect,
      drawerClose
    }
  }
})
</script>

<style lang="less">
@import url('../components/global.less');

/*
 * The following styles are auto-applied to elements with
 * transition="page-transition" when their visibility is toggled
 * by Vue.js.
 *
 * You can easily play with the page transition by editing
 * these styles.
 */

.page-transition-enter {
  opacity: 0;
}

.page-transition-leave-active {
  opacity: 0;
}

.page-transition-enter .page-transition-container,
.page-transition-leave-active .page-transition-container {
  -webkit-transform: scale(1.1);
  transform: scale(1.1);
}
</style>