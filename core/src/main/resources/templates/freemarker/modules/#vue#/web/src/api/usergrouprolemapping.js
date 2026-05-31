import { axios } from '@/utils/request'

const modulePath = '/v1/userGroupRoleMapping'

const api = {
  usergrouprolemappingList: modulePath + '/page',
  usergrouprolemappingAll: modulePath + '/all',
  addUserGroupRoleMapping: modulePath
}

export default api

export function getUserGroupRoleMappingList (parameter) {
  return axios({
    url: api.usergrouprolemappingList,
    method: 'get',
    params: parameter
  })
}

export function getUserGroupRoleMappingAll (parameter) {
  return axios({
    url: api.usergrouprolemappingAll,
    method: 'get',
    params: parameter
  })
}

export function getUserGroupRoleMappingInfo (mappingId) {
  return axios({
    url: modulePath + '/' + mappingId,
    method: 'get'
  })
}

export function addUserGroupRoleMapping (parameter) {
  return axios({
    url: api.addUserGroupRoleMapping,
    method: 'post',
    params: parameter
  })
}

export function editUserGroupRoleMapping (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    params: parameter
  })
}

export function delUserGroupRoleMapping (mappingId) {
  console.log('mappingId', mappingId)
  return axios({
    url: modulePath + '/' + mappingId,
    method: 'delete'
  })
}
