import { axios } from '@/utils/request'

const modulePath = '/v1/userRoleMapping'

const api = {
  userrolemappingList: modulePath + '/page',
  userrolemappingAll: modulePath + '/all',
  addUserRoleMapping: modulePath
}

export default api

export function getUserRoleMappingList (parameter) {
  return axios({
    url: api.userrolemappingList,
    method: 'get',
    params: parameter
  })
}

export function getUserRoleMappingAll (parameter) {
  return axios({
    url: api.userrolemappingAll,
    method: 'get',
    params: parameter
  })
}

export function getUserRoleMappingInfo (id) {
  return axios({
    url: modulePath + '/' + id,
    method: 'get'
  })
}

export function addUserRoleMapping (parameter) {
  return axios({
    url: api.addUserRoleMapping,
    method: 'post',
    params: parameter
  })
}

export function editUserRoleMapping (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    params: parameter
  })
}

export function delUserRoleMapping (id) {
  return axios({
    url: modulePath + '/' + id,
    method: 'delete'
  })
}
