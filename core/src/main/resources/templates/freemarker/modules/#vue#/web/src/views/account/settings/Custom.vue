<script>
import { h } from 'vue'
import { colorList } from '@/components/SettingDrawer/settingConfig'
import ASwitch from 'ant-design-vue/es/switch'
import AList from 'ant-design-vue/es/list'
import AListItem from 'ant-design-vue/es/list/Item'
import { mixin } from '@/utils/mixin'

export default {
  components: {
    AListItem,
    AList,
    ASwitch
  },
  mixins: [mixin],
  data () {
    return {
    }
  },
  filters: {
    themeFilter (theme) {
      const themeMap = {
        'dark': '暗色',
        'light': '白色'
      }
      return themeMap[theme]
    }
  },
  methods: {
    colorFilter (color) {
      const c = colorList.filter(o => o.color === color)[0]
      return c && c.key
    },

    onChange (checked) {
      if (checked) {
        this.$store.dispatch('ToggleTheme', 'dark')
      } else {
        this.$store.dispatch('ToggleTheme', 'light')
      }
    }
  },
  render () {
    return h('div', [
      h(AList, { itemLayout: 'horizontal' }, () => [
        h(AListItem, null, {
          actions: () => [
            h(ASwitch, {
              checkedChildren: '暗色',
              unCheckedChildren: '白色',
              checked: this.navTheme === 'dark',
              onChange: this.onChange
            })
          ],
          default: () => [
            h(AListItem.Meta, null, {
              title: () => h('a', '风格配色'),
              description: () => h('span', '整体风格配色设置')
            })
          ]
        }),
        h(AListItem, null, {
          default: () => [
            h(AListItem.Meta, null, {
              title: () => h('a', '主题色'),
              description: () => h('span', [
                '页面风格配色： ',
                h('a', { innerHTML: this.colorFilter(this.primaryColor) })
              ])
            })
          ]
        })
      ])
    ])
  }
}
</script>

<style scoped>

</style>
