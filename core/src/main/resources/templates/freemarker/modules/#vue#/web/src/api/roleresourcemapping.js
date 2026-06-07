import { axios } from '@/utils/request'

const modulePath = '/v1/roleResourceMapping'

const api = {
  roleresourcemappingList: modulePath + '/page',
  roleresourcemappingAll: modulePath + '/all',
  addRoleResourceMapping: modulePath
}

export default api

export function getRoleResourceMappingList (parameter) {
  return axios({
    url: api.roleresourcemappingList,
    method: 'get',
    params: parameter
  })
}

export function getRoleResourceMappingAll (parameter) {
  return axios({
    url: api.roleresourcemappingAll,
    method: 'get',
    params: parameter
  })
}

export function getRoleResourceMappingInfo (id) {
  return axios({
    url: modulePath + '/' + id,
    method: 'get'
  })
}

export function addRoleResourceMapping (parameter) {
  return axios({
    url: api.addRoleResourceMapping,
    method: 'post',
    params: parameter
  })
}

export function editRoleResourceMapping (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    params: parameter
  })
}

export function delRoleResourceMapping (id) {
  return axios({
    url: modulePath + '/' + id,
    method: 'delete'
  })
}
