import { axios } from '@/utils/request'

const modulePath = '/v1/userGroupMapping'

const api = {
  usergroupmappingList: modulePath + '/page',
  usergroupmappingAll: modulePath + '/all',
  addUserGroupMapping: modulePath
}

export default api

export function getUserGroupMappingList (parameter) {
  return axios({
    url: api.usergroupmappingList,
    method: 'get',
    params: parameter
  })
}

export function getUserGroupMappingAll (parameter) {
  return axios({
    url: api.usergroupmappingAll,
    method: 'get',
    params: parameter
  })
}

export function getUserGroupMappingInfo (mappingId) {
  return axios({
    url: modulePath + '/' + mappingId,
    method: 'get'
  })
}

export function addUserGroupMapping (parameter) {
  return axios({
    url: api.addUserGroupMapping,
    method: 'post',
    params: parameter
  })
}

export function editUserGroupMapping (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    params: parameter
  })
}

export function delUserGroupMapping (mappingId) {
  return axios({
    url: modulePath + '/' + mappingId,
    method: 'delete'
  })
}
