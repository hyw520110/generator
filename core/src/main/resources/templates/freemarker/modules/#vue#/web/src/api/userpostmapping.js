import { axios } from '@/utils/request'

const modulePath = '/v1/userPostMapping'

const api = {
  userpostmappingList: modulePath + '/page',
  userpostmappingAll: modulePath + '/all',
  addUserPostMapping: modulePath
}

export default api

export function getUserPostMappingList (parameter) {
  return axios({
    url: api.userpostmappingList,
    method: 'get',
    params: parameter
  })
}

export function getUserPostMappingAll (parameter) {
  return axios({
    url: api.userpostmappingAll,
    method: 'get',
    params: parameter
  })
}

export function getUserPostMappingInfo (mappingId) {
  return axios({
    url: modulePath + '/' + mappingId,
    method: 'get'
  })
}

export function addUserPostMapping (parameter) {
  return axios({
    url: api.addUserPostMapping,
    method: 'post',
    params: parameter
  })
}

export function editUserPostMapping (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    params: parameter
  })
}

export function delUserPostMapping (mappingId) {
  console.log('mappingId', mappingId)
  return axios({
    url: modulePath + '/' + mappingId,
    method: 'delete'
  })
}
