<script lang="jsx">
import { defineComponent, ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'

export default defineComponent({
  name: 'MultiTab',
  setup () {
    const router = useRouter()
    const route = useRoute()
    
    const fullPathList = ref([])
    const pages = ref([])
    const activeKey = ref('')

    const selectedLastPath = () => {
      activeKey.value = fullPathList.value[fullPathList.value.length - 1]
    }

    const remove = (targetKey) => {
      pages.value = pages.value.filter(page => page.fullPath !== targetKey)
      fullPathList.value = fullPathList.value.filter(path => path !== targetKey)
      if (!fullPathList.value.includes(activeKey.value)) {
        selectedLastPath()
      }
    }

    const onEdit = (targetKey, action) => {
      if (action === 'remove') {
        remove(targetKey)
      }
    }

    const closeThat = (e) => {
      remove(e)
    }

    const closeLeft = (e) => {
      const currentIndex = fullPathList.value.indexOf(e)
      if (currentIndex > 0) {
        const toRemove = fullPathList.value.slice(0, currentIndex)
        toRemove.forEach(item => remove(item))
      } else {
        message.info('左侧没有标签')
      }
    }

    const closeRight = (e) => {
      const currentIndex = fullPathList.value.indexOf(e)
      if (currentIndex < (fullPathList.value.length - 1)) {
        const toRemove = fullPathList.value.slice(currentIndex + 1)
        toRemove.forEach(item => remove(item))
      } else {
        message.info('右侧没有标签')
      }
    }

    const closeAll = (e) => {
      fullPathList.value.forEach((item) => {
        if (item !== e) {
          remove(item)
        }
      })
    }

    const closeMenuClick = ({ key, domEvent }) => {
      const vkey = domEvent.target.getAttribute('data-vkey')
      switch (key) {
        case 'close-right':
          closeRight(vkey)
          break
        case 'close-left':
          closeLeft(vkey)
          break
        case 'close-all':
          closeAll(vkey)
          break
        default:
        case 'close-that':
          closeThat(vkey)
          break
      }
    }

    const renderTabPaneMenu = (e) => {
      return (
        <a-menu onClick={closeMenuClick}>
          <a-menu-item key="close-that" data-vkey={e}>关闭当前标签</a-menu-item>
          <a-menu-item key="close-right" data-vkey={e}>关闭右侧</a-menu-item>
          <a-menu-item key="close-left" data-vkey={e}>关闭左侧</a-menu-item>
          <a-menu-item key="close-all" data-vkey={e}>关闭全部</a-menu-item>
        </a-menu>
      )
    }

    const renderTabPane = (title, keyPath) => {
      const menu = renderTabPaneMenu(keyPath)
      return (
        <a-dropdown overlay={menu} trigger={['contextmenu']}>
          <span style={{ userSelect: 'none' }}>{title}</span>
        </a-dropdown>
      )
    }

    onMounted(() => {
      pages.value.push(route)
      fullPathList.value.push(route.fullPath)
      selectedLastPath()
    })

    watch(() => route, (newVal) => {
      activeKey.value = newVal.fullPath
      if (fullPathList.value.indexOf(newVal.fullPath) < 0) {
        fullPathList.value.push(newVal.fullPath)
        pages.value.push(newVal)
      }
    }, { deep: true })

    watch(activeKey, (newPathKey) => {
      router.push({ path: newPathKey })
    })

    return {
      fullPathList,
      pages,
      activeKey,
      onEdit,
      renderTabPane
    }
  },
  render () {
    const { onEdit, pages, activeKey, renderTabPane } = this
    const panes = pages.map(page => {
      return (
        <a-tab-pane
          style={{ height: 0 }}
          tab={renderTabPane(page.meta?.title || page.name, page.fullPath)}
          key={page.fullPath}
          closable={pages.length > 1}
        />
      )
    })

    return (
      <div class="ant-pro-multi-tab">
        <div class="ant-pro-multi-tab-wrapper">
          <a-tabs
            hideAdd
            type="editable-card"
            v-model={[activeKey, 'value']}
            tabBarStyle={{ background: '#FFF', margin: 0, paddingLeft: '16px', paddingTop: '1px' }}
            onEdit={onEdit}>
            {panes}
          </a-tabs>
        </div>
      </div>
    )
  }
})
</script>

<style lang="less" scoped>
.ant-pro-multi-tab {
  margin: -23px -24px 24px -24px;
  
  &-wrapper {
    background: #fff;
  }
}
</style>