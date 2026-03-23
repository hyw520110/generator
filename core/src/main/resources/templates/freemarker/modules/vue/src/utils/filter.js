import moment from 'moment'
import 'moment/locale/zh-cn'
moment.locale('zh-cn')

export function NumberFormat (value) {
  if (!value) {
    return '0'
  }
  const intPartFormat = value.toString().replace(/(\d)(?=(?:\d{3})+$)/g, '$1,') // 将整数部分逢三一断
  return intPartFormat
}

export function dayjs (dataStr, pattern = 'YYYY-MM-DD HH:mm:ss') {
  return moment(dataStr).format(pattern)
}

export function momentFilter (dataStr, pattern = 'YYYY-MM-DD HH:mm:ss') {
  return moment(dataStr).format(pattern)
}

const filters = {
  NumberFormat,
  dayjs,
  moment: momentFilter
}

export default filters