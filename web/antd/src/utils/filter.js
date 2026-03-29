import moment from 'moment'
import 'moment/dist/locale/zh-cn'
moment.locale('zh-cn')

export const NumberFormat = (value) => {
  if (!value) {
    return '0'
  }
  const intPartFormat = value.toString().replace(/(\d)(?=(?:\d{3})+$)/g, '$1,')
  return intPartFormat
}

export const dayjs = (dataStr, pattern = 'YYYY-MM-DD HH:mm:ss') => {
  return moment(dataStr).format(pattern)
}

export const momentFilter = (dataStr, pattern = 'YYYY-MM-DD HH:mm:ss') => {
  return moment(dataStr).format(pattern)
}

export default {
  install (app) {
    app.config.globalProperties.$filters = {
      NumberFormat,
      dayjs,
      moment: momentFilter
    }
  }
}