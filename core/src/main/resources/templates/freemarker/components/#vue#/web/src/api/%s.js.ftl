/* eslint-disable no-undef */
import { axios } from '@/utils/request'

const modulePath = '/${table.beanName!}'
const primaryKeyFields = ${table.primaryKeyJsArray}

function buildPrimaryKeyPath (record) {
  if (record && typeof record === 'object') {
    return primaryKeyFields.map(key => encodeURIComponent(record[key])).join('/')
  }
  return encodeURIComponent(record)
}

function buildPrimaryKeyPayload (record) {
  if (record && typeof record === 'object') {
    return primaryKeyFields.reduce((payload, key) => {
      payload[key] = record[key]
      return payload
    }, {})
  }
  return { [primaryKeyFields[0]]: record }
}

const api = {
  pageList: modulePath + '/page',
  add${table.beanName?cap_first}: modulePath,
  batchDelete${table.beanName?cap_first}: modulePath + '/batch'
}

export default api

export function getList (parameter) {
  return axios({
    url: api.pageList,
    method: 'get',
    params: parameter
  })
}

export function getInfo (record) {
  return axios({
    url: modulePath + '/' + buildPrimaryKeyPath(record),
    method: 'get'
  })
}
export function add${table.beanName?cap_first} (parameter) {
  return axios({
    url: api.add${table.beanName?cap_first},
    method: 'post',
    data: parameter
  })
}
export function edit${table.beanName?cap_first} (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    data: parameter
  })
}
export function del${table.beanName?cap_first} (record) {
  return axios({
    url: modulePath + '/' + buildPrimaryKeyPath(record),
    method: 'delete'
  })
}

export function batchDel${table.beanName?cap_first} (records) {
  return axios({
    url: api.batchDelete${table.beanName?cap_first},
    method: 'delete',
    data: records.map(buildPrimaryKeyPayload)
  })
}
