import { axios } from '@/utils/request'

const modulePath = '/v1/role'

const api = {
  roleList: modulePath + '/page',
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

export function addRole (parameter) {
  return axios({
    url: api.addRole,
    method: 'post',
    params: parameter
  })
}

export function editRole (roleId, parameter) {
  console.log('roleId', roleId)
  return axios({
    url: modulePath + '/' + roleId,
    method: 'put',
    params: parameter
  })
}

export function delRole (roleId) {
  console.log('roleId', roleId)
  return axios({
    url: modulePath + '/' + roleId,
    method: 'delete'
  })
}
