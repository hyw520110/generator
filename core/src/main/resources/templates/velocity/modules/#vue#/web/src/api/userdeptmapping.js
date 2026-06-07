import { axios } from '@/utils/request'

const modulePath = '/v1/userDeptMapping'

const api = {
  userdeptmappingList: modulePath + '/page',
  userdeptmappingAll: modulePath + '/all',
  addUserDeptMapping: modulePath
}

export default api

export function getUserDeptMappingList (parameter) {
  return axios({
    url: api.userdeptmappingList,
    method: 'get',
    params: parameter
  })
}

export function getUserDeptMappingAll (parameter) {
  return axios({
    url: api.userdeptmappingAll,
    method: 'get',
    params: parameter
  })
}

export function getUserDeptMappingInfo (mappingId) {
  return axios({
    url: modulePath + '/' + mappingId,
    method: 'get'
  })
}

export function addUserDeptMapping (parameter) {
  return axios({
    url: api.addUserDeptMapping,
    method: 'post',
    params: parameter
  })
}

export function editUserDeptMapping (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    params: parameter
  })
}

export function delUserDeptMapping (mappingId) {
  return axios({
    url: modulePath + '/' + mappingId,
    method: 'delete'
  })
}
