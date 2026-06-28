/* eslint-disable no-undef */
import { axios } from '@/utils/request'

const modulePath = '/${table.beanName}'
const primaryKeyFields = ${table.primaryKeyJsArray}
const hasPrimaryKey = primaryKeyFields.length > 0

function rejectNoPrimaryKey () {
  return Promise.reject(new Error('当前表未定义主键，不能执行按主键查询、编辑或删除'))
}

function buildPrimaryKeyPath (record) {
  if (!hasPrimaryKey) {
    return ''
  }
  if (record && typeof record === 'object') {
    return primaryKeyFields.map(key => encodeURIComponent(record[key])).join('/')
  }
  return encodeURIComponent(record)
}

function buildPrimaryKeyPayload (record) {
  if (!hasPrimaryKey) {
    return {}
  }
  if (primaryKeyFields.length === 1) {
    return record && typeof record === 'object' ? record[primaryKeyFields[0]] : record
  }
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
  add${table.beanName}: modulePath,
  batchDelete${table.beanName}: modulePath + '/batch'
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
  if (!hasPrimaryKey) {
    return rejectNoPrimaryKey()
  }
  return axios({
    url: modulePath + '/' + buildPrimaryKeyPath(record),
    method: 'get'
  })
}

export function add${table.beanName} (parameter) {
  return axios({
    url: api.add${table.beanName},
    method: 'post',
    data: parameter
  })
}

export function edit${table.beanName} (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    data: parameter
  })
}

export function del${table.beanName} (record) {
  if (!hasPrimaryKey) {
    return rejectNoPrimaryKey()
  }
  return axios({
    url: modulePath + '/' + buildPrimaryKeyPath(record),
    method: 'delete'
  })
}

export function batchDel${table.beanName} (records) {
  if (!hasPrimaryKey) {
    return rejectNoPrimaryKey()
  }
  return axios({
    url: api.batchDelete${table.beanName},
    method: 'delete',
    data: records.map(buildPrimaryKeyPayload)
  })
}
