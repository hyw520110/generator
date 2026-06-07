import { axios } from '@/utils/request'

const modulePath = '/v1/role'

const api = {
  roleList: modulePath + '/page',
  roleAll: modulePath + '/all',
  addRole: modulePath
}

export default api

export function getRoleList (parameter) {
  return axios({
    url: api.roleList,
    method: 'get',
    params: parameter
  })
}

export function getUserRoles (userId) {
  return axios({
    url: modulePath + '/' + userId + '/list',
    method: 'get'
  })
}

export function getGroupRoles (groupId) {
  return axios({
    url: modulePath + '/group/' + groupId + '/list',
    method: 'get'
  })
}

export function getRoleAll (parameter) {
  return axios({
    url: api.roleAll,
    method: 'get',
    params: parameter
  })
}

export function getRoleInfo (roleId) {
  return axios({
    url: modulePath + '/' + roleId,
    method: 'get'
  })
}

export function addRole (parameter) {
  return axios({
    url: api.addRole,
    method: 'post',
    params: parameter
  })
}

export function editRole (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    params: parameter
  })
}

export function delRole (roleId) {
  return axios({
    url: modulePath + '/' + roleId,
    method: 'delete'
  })
}
