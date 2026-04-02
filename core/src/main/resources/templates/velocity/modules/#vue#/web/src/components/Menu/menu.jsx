import { resolveComponent, createVNode, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import Menu from 'ant-design-vue/es/menu'
import * as Icons from '@ant-design/icons-vue'

const { Item, SubMenu } = Menu

// 图标名称映射：将简短名称映射到 Ant Design 图标组件名
const iconMap = {
  dashboard: 'DashboardOutlined',
  form: 'FormOutlined',
  table: 'TableOutlined',
  profile: 'ProfileOutlined',
  'check-circle-o': 'CheckCircleOutlined',
  warning: 'WarningOutlined',
  user: 'UserOutlined',
  setting: 'SettingOutlined',
  home: 'HomeOutlined',
  mail: 'MailOutlined',
  appstore: 'AppstoreOutlined',
  menu: 'MenuOutlined',
  'menu-fold': 'MenuFoldOutlined',
  'menu-unfold': 'MenuUnfoldOutlined',
  down: 'DownOutlined',
  up: 'UpOutlined',
  left: 'LeftOutlined',
  right: 'RightOutlined',
  plus: 'PlusOutlined',
  minus: 'MinusOutlined',
  close: 'CloseOutlined',
  check: 'CheckOutlined',
  edit: 'EditOutlined',
  delete: 'DeleteOutlined',
  search: 'SearchOutlined',
  reload: 'ReloadOutlined',
  lock: 'LockOutlined',
  unlock: 'UnlockOutlined',
  question: 'QuestionOutlined',
  info: 'InfoOutlined',
  'info-circle': 'InfoCircleOutlined',
  'question-circle': 'QuestionCircleOutlined',
  'exclamation-circle': 'ExclamationCircleOutlined',
  'check-circle': 'CheckCircleOutlined',
  'close-circle': 'CloseCircleOutlined',
  bell: 'BellOutlined',
  calendar: 'CalendarOutlined',
  cloud: 'CloudOutlined',
  download: 'DownloadOutlined',
  upload: 'UploadOutlined',
  'cloud-download': 'CloudDownloadOutlined',
  'cloud-upload': 'CloudUploadOutlined',
  star: 'StarOutlined',
  heart: 'HeartOutlined',
  eye: 'EyeOutlined',
  environment: 'EnvironmentOutlined',
  camera: 'CameraOutlined',
  save: 'SaveOutlined',
  team: 'TeamOutlined',
  solution: 'SolutionOutlined',
  phone: 'PhoneOutlined',
  filter: 'FilterOutlined',
  folder: 'FolderOutlined',
  'folder-open': 'FolderOpenOutlined',
  'folder-add': 'FolderAddOutlined',
  file: 'FileOutlined',
  'file-text': 'FileTextOutlined',
  'file-add': 'FileAddOutlined',
  laptop: 'LaptopOutlined',
  desktop: 'DesktopOutlined',
  mobile: 'MobileOutlined',
  tablet: 'TabletOutlined',
  global: 'GlobalOutlined',
  link: 'LinkOutlined',
  notification: 'NotificationOutlined',
  tag: 'TagOutlined',
  tags: 'TagsOutlined',
  share: 'ShareAltOutlined',
  shop: 'ShopOutlined',
  gift: 'GiftOutlined',
  idcard: 'IdcardOutlined',
  wallet: 'WalletOutlined',
  bank: 'BankOutlined',
  trophy: 'TrophyOutlined',
  api: 'ApiOutlined',
  database: 'DatabaseOutlined',
  server: 'ServerOutlined',
  code: 'CodeOutlined',
  build: 'BuildOutlined',
  bug: 'BugOutlined',
  skin: 'SkinOutlined',
  tool: 'ToolOutlined',
  thunderbolt: 'ThunderboltOutlined',
  robot: 'RobotOutlined',
  rocket: 'RocketOutlined',
  safety: 'SafetyOutlined',
  security: 'SecurityScanOutlined',
  insurance: 'InsuranceOutlined',
  experiment: 'ExperimentOutlined',
  fire: 'FireOutlined',
  crown: 'CrownOutlined',
  gold: 'GoldOutlined',
  dollar: 'DollarOutlined',
  euro: 'EuroOutlined',
  pound: 'PoundOutlined',
  calculator: 'CalculatorOutlined',
  stock: 'StockOutlined',
  fund: 'FundOutlined',
  'area-chart': 'AreaChartOutlined',
  'bar-chart': 'BarChartOutlined',
  'line-chart': 'LineChartOutlined',
  'pie-chart': 'PieChartOutlined',
  'dot-chart': 'DotChartOutlined',
  'radar-chart': 'RadarChartOutlined'
}

export default {
  name: 'SMenu',
  props: {
    menu: {
      type: Array,
      required: true
    },
    theme: {
      type: String,
      required: false,
      default: 'dark'
    },
    mode: {
      type: String,
      required: false,
      default: 'inline'
    },
    collapsed: {
      type: Boolean,
      required: false,
      default: false
    }
  },
  data () {
    return {
      openKeys: [],
      selectedKeys: [],
      cachedOpenKeys: []
    }
  },
  computed: {
    rootSubmenuKeys: vm => {
      const keys = []
      vm.menu.forEach(item => keys.push(item.path))
      return keys
    }
  },
  created () {
    // 获取 router 实例
    this.router = this.$router
  },
  mounted () {
    this.updateMenu()
  },
  watch: {
    collapsed (val) {
      if (val) {
        this.cachedOpenKeys = this.openKeys.concat()
        this.openKeys = []
      } else {
        this.openKeys = this.cachedOpenKeys
      }
    },
    $route: function () {
      this.updateMenu()
    },
    // 监听菜单数据变化，确保动态加载菜单后也能正确展开
    menu: {
      handler () {
        this.$nextTick(() => {
          this.updateMenu()
        })
      },
      immediate: true
    }
  },
  methods: {
    // select menu item
    onOpenChange (openKeys) {
      // 在水平模式下时执行，并且不再执行后续
      if (this.mode === 'horizontal') {
        this.openKeys = openKeys
        return
      }
      // 非水平模式时
      const latestOpenKey = openKeys.find(key => !this.openKeys.includes(key))
      if (!this.rootSubmenuKeys.includes(latestOpenKey)) {
        this.openKeys = openKeys
      } else {
        this.openKeys = latestOpenKey ? [latestOpenKey] : []
      }
    },
    updateMenu () {
      const routes = this.$route.matched.concat()
      const { hidden } = this.$route.meta
      if (routes.length >= 3 && hidden) {
        routes.pop()
        this.selectedKeys = [routes[routes.length - 1].path]
      } else {
        this.selectedKeys = [routes.pop().path]
      }
      const openKeys = []
      if (this.mode === 'inline') {
        routes.forEach(item => {
          openKeys.push(item.path)
        })
      }
      
      // 默认展开所有有子菜单的菜单项
      this.menu.forEach(item => {
        if (item.children && item.children.length > 0 && item.path) {
          if (!openKeys.includes(item.path)) {
            openKeys.push(item.path)
          }
        }
      })

      this.collapsed ? (this.cachedOpenKeys = openKeys) : (this.openKeys = openKeys)
    },

    // render
    renderItem (menu) {
      if (!menu.hidden) {
        return menu.children && !menu.hideChildrenInMenu ? this.renderSubMenu(menu) : this.renderMenuItem(menu)
      }
      return null
    },
    renderMenuItem (menu) {
      const target = menu.meta.target || null
      const href = menu.path

      if (menu.children && menu.hideChildrenInMenu) {
        // 把有子菜单的 并且 父菜单是要隐藏子菜单的
        // 都给子菜单增加一个 hidden 属性
        // 用来给刷新页面时， selectedKeys 做控制用
        menu.children.forEach(item => {
          item.meta = Object.assign(item.meta, { hidden: true })
        })
      }

      // 对于外部链接，使用 a 标签
      if (target) {
        return (
          <Item {...{ key: menu.path }}>
            <a href={href} target={target}>
              {this.renderIcon(menu.meta.icon)}
              <span>{menu.meta.title}</span>
            </a>
          </Item>
        )
      }

      // 对于内部路由，使用 router-link
      return (
        <Item {...{ key: menu.path }}>
          <router-link to={{ name: menu.name }}>
            {this.renderIcon(menu.meta.icon)}
            <span>{menu.meta.title}</span>
          </router-link>
        </Item>
      )
    },
    renderSubMenu (menu) {
      const itemArr = []
      if (!menu.hideChildrenInMenu) {
        menu.children.forEach(item => itemArr.push(this.renderItem(item)))
      }
      
      // 使用 title 插槽的正确方式
      const titleSlot = () => (
        <span>
          {this.renderIcon(menu.meta.icon)}
          <span>{menu.meta.title}</span>
        </span>
      )
      
      return (
        <SubMenu key={menu.path} v-slots={{ title: titleSlot }}>
          {itemArr}
        </SubMenu>
      )
    },
    renderIcon (icon) {
      if (icon === 'none' || icon === undefined || icon === null) {
        return null
      }
      
      // 如果是对象（SVG 组件），直接渲染
      if (typeof icon === 'object') {
        const IconComponent = icon
        return <IconComponent />
      }
      
      // 如果是字符串，映射到对应的图标组件
      const iconName = iconMap[icon] || this.getIconComponentName(icon)
      
      // 尝试从全局注册的组件中获取
      const IconComponent = resolveComponent(iconName)
      
      if (IconComponent && typeof IconComponent !== 'string') {
        return <IconComponent />
      }
      
      // 如果映射的组件不存在，尝试直接使用图标名作为组件名
      const directIconName = icon.charAt(0).toUpperCase() + icon.slice(1) + 'Outlined'
      const DirectIconComponent = Icons[directIconName]
      
      if (DirectIconComponent) {
        return <DirectIconComponent />
      }
      
      // 最后尝试从 Icons 中获取
      const GlobalIcon = Icons[iconName]
      if (GlobalIcon) {
        return <GlobalIcon />
      }
      
      return null
    },
    getIconComponentName (iconName) {
      // 处理带连字符的图标名，如 'check-circle-o' -> 'CheckCircleOutlined'
      if (iconName.includes('-')) {
        const parts = iconName.split('-')
        // 处理 -o 后缀（outline）
        const isOutline = parts[parts.length - 1] === 'o'
        const nameParts = isOutline ? parts.slice(0, -1) : parts
        const componentName = nameParts.map(part => part.charAt(0).toUpperCase() + part.slice(1)).join('') + 'Outlined'
        return componentName
      }
      return iconName.charAt(0).toUpperCase() + iconName.slice(1) + 'Outlined'
    },
    // 根据路径查找菜单项
    findRouteByPath (path, menus) {
      for (const menu of menus) {
        if (menu.path === path) {
          return menu
        }
        if (menu.children && menu.children.length > 0) {
          const found = this.findRouteByPath(path, menu.children)
          if (found) return found
        }
      }
      return null
    }
  },

  render () {
    const { mode, theme, menu } = this
    
    const menuTree = menu.map(item => {
      if (item.hidden) {
        return null
      }
      return this.renderItem(item)
    })
    
    // Vue 3 + Ant Design Vue 4.x JSX: 直接传递 props
    return (
      <Menu
        mode={mode}
        theme={theme}
        openKeys={this.openKeys}
        selectedKeys={this.selectedKeys}
        onSelect={obj => {
          this.selectedKeys = obj.selectedKeys
          this.$emit('select', obj)
        }}
        onOpenChange={this.onOpenChange}
      >
        {menuTree}
      </Menu>
    )
  }
}