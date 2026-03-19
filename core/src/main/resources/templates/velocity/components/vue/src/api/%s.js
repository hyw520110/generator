/* eslint-disable no-undef */
import { axios } from '@/utils/request'

const moudulePath = '/${table.beanName}'

const api = {
  pageList: moudulePath + '/page',
  add${table.beanName}: moudulePath + '/add'
}

export default api

export function getList (parameter) {
  return axios({
    url: api.pageList,
    method: 'get',
    params: parameter
  })
}

export function getInfo (#if(""=="${table.getPrimarykeyFieldsNames()}")id#else${table.getPrimarykeyFieldsNames()}#end) {
  return axios({
    url: moudulePath + '/' + #foreach($field in ${table.primarykeyFields})${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}) + ","#end#end,
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
  console.log('parameter', parameter)
  return axios({
    url: moudulePath,
    method: 'put',
    data: parameter
  })
}
export function del${table.beanName} (#if(""=="${table.getPrimarykeyFieldsNames()}")id#else${table.getPrimarykeyFieldsNames()}#end) {
  console.log('parameter', #if(""=="${table.getPrimarykeyFieldsNames()}")id#else${table.getPrimarykeyFieldsNames()}#end)
  return axios({
    url: moudulePath + '/' + #foreach($field in ${table.primarykeyFields})${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}) + ","#end#end,
    method: 'delete'
  })
}
