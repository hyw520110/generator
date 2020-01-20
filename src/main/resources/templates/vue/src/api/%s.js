/* eslint-disable no-undef */
import { axios } from '@/utils/request'

const moudulePath = '/v1/${table.beanName}'

const api = {
  add: moudulePath + '/add',
  edit: moudulePath + '/update',
  del: moudulePath + '/del',
  list: moudulePath + '/page'
}

export default api

export function getInfo (#if(""=="${table.getPrimarykeyFieldsNames()}")id#else${table.getPrimarykeyFieldsNames()}#end) {
  return axios({
    url: moudulePath + '/' + #foreach($field in ${table.primarykeyFields})${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}) + ","#end#end,
    method: 'get'
  })
}
export function add (parameter) {
  return axios({
    url: api.add,
    method: 'post',
    data: parameter
  })
}
export function edit (parameter) {
  return axios({
    url: api.edit,
    method: 'put',
    data: parameter
  })
}
export function del (#if(""=="${table.getPrimarykeyFieldsNames()}")id#else${table.getPrimarykeyFieldsNames()}#end) {
  return axios({
    url: api.del + '/' + #foreach($field in ${table.primarykeyFields})${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}) + ","#end#end,
    method: 'delete',
    data: parameter
  })
}
export function getList (parameter) {
  return axios({
    url: api.list,
    method: 'get',
    params: parameter
  })
}
