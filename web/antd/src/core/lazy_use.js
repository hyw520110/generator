// Vue 3 插件注册 - 按需加载组件

// base library
import {
  ConfigProvider,
  Layout,
  Input,
  InputNumber,
  Button,
  Switch,
  Radio,
  Checkbox,
  Select,
  Card,
  Form,
  Row,
  Col,
  Modal,
  Table,
  Tabs,
  Badge,
  Popover,
  Dropdown,
  List,
  Avatar,
  Breadcrumb,
  Steps,
  Spin,
  Menu,
  Drawer,
  Tooltip,
  Alert,
  Tag,
  Divider,
  DatePicker,
  TimePicker,
  Upload,
  Progress,
  Skeleton,
  Popconfirm,
  PageHeader,
  Statistic,
  Descriptions,
  message,
  notification
} from 'ant-design-vue'
// import Viser from 'viser-vue' // Vue 2 only

// ext library
// import VueCropper from 'vue-cropper' // Vue 2 only
import Dialog from '@/components/Dialog'
import MultiTab from '@/components/MultiTab'
import PageLoading from '@/components/PageLoading'
import PermissionHelper from '@/utils/helper/permission'
import actionDirective from './directives/action'

export default {
  install (app) {
    // 注册 Ant Design 组件
    app.use(ConfigProvider)
    app.use(Layout)
    app.use(Input)
    app.use(InputNumber)
    app.use(Button)
    app.use(Switch)
    app.use(Radio)
    app.use(Checkbox)
    app.use(Select)
    app.use(Card)
    app.use(Form)
    app.use(Row)
    app.use(Col)
    app.use(Modal)
    app.use(Table)
    app.use(Tabs)
    app.use(Badge)
    app.use(Popover)
    app.use(Dropdown)
    app.use(List)
    app.use(Avatar)
    app.use(Breadcrumb)
    app.use(Steps)
    app.use(Spin)
    app.use(Menu)
    app.use(Drawer)
    app.use(Tooltip)
    app.use(Alert)
    app.use(Tag)
    app.use(Divider)
    app.use(DatePicker)
    app.use(TimePicker)
    app.use(Upload)
    app.use(Progress)
    app.use(Skeleton)
    app.use(Popconfirm)
    app.use(PageHeader)
    app.use(Statistic)
    app.use(Descriptions)

    // 全局属性
    app.config.globalProperties.$confirm = Modal.confirm
    app.config.globalProperties.$message = message
    app.config.globalProperties.$notification = notification
    app.config.globalProperties.$info = Modal.info
    app.config.globalProperties.$success = Modal.success
    app.config.globalProperties.$error = Modal.error
    app.config.globalProperties.$warning = Modal.warning

    // 其他库
    // app.use(Viser) // Vue 2 only
    app.use(Dialog)
    app.use(MultiTab)
    app.use(PageLoading)
    app.use(PermissionHelper)
    // app.use(VueCropper) // Vue 2 only
    
    // 注册指令
    app.directive('action', actionDirective)
  }
}