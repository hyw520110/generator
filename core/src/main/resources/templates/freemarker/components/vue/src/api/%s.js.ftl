/* eslint-disable no-undef */
import { axios } from '@/utils/request'

const moudulePath = '/${table.beanName!}'

const api = {
  pageList: moudulePath + '/page',
  add${table.beanName!}: moudulePath + '/add'
}

export default api

export function getList (parameter) {
  return axios({
    url: api.pageList,
    method: 'get',
    params: parameter
  })
}

export function getInfo (<#if table.primarykeyFieldsNames!?length == 0>id<#else>${table.primarykeyFieldsNames!}</#if>) {
  return axios({
    url: moudulePath + '/' + <#list table.primarykeyFields as field>${field.propertyName}<#if field?has_next> + "," + </#if></#list>,
    method: 'get'
  })
}
export function add${table.beanName!} (parameter) {
  return axios({
    url: api.add${table.beanName!},
    method: 'post',
    data: parameter
  })
}
export function edit${table.beanName!} (parameter) {
  console.log('parameter', parameter)
  return axios({
    url: moudulePath,
    method: 'put',
    data: parameter
  })
}
export function del${table.beanName!} (<#if table.primarykeyFieldsNames!?length == 0>id<#else>${table.primarykeyFieldsNames!}</#if>) {
  console.log('parameter', <#if table.primarykeyFieldsNames!?length == 0>id<#else>${table.primarykeyFieldsNames!}</#if>)
  return axios({
    url: moudulePath + '/' + <#list table.primarykeyFields as field>${field.propertyName}<#if field?has_next> + "," + </#if></#list>,
    method: 'delete'
  })
}