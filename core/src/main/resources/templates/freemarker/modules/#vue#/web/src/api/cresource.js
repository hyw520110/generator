import { axios } from '@/utils/request'

const modulePath = '/resource'

const api = {
  resourceList: modulePath + '/list',
  addResource: modulePath
}

export default api

export function getResourceList (parameter) {
  return axios({
    url: api.resourceList,
    method: 'get',
    params: parameter
  })
}

export function getResourceInfo (resourceId) {
  return axios({
    url: modulePath + '/' + resourceId,
    method: 'get'
  })
}

export function addResource (parameter) {
  return axios({
    url: api.addResource,
    method: 'post',
    params: parameter
  })
}

export function editResource (resourceId, parameter) {
  console.log('resourceId', resourceId)
  return axios({
    url: modulePath + '/' + resourceId,
    method: 'put',
    params: parameter
  })
}

export function delResource (resourceId) {
  console.log('resourceId', resourceId)
  return axios({
    url: modulePath + '/' + resourceId,
    method: 'delete'
  })
}

export function getUserResources (userId) {
  return axios({
    url: modulePath + '/' + userId + '/list',
    method: 'get'
  })
}

export function getRoleResources (roleId) {
  return axios({
    url: modulePath + '/role/' + roleId + '/list',
    method: 'get'
  })
}
