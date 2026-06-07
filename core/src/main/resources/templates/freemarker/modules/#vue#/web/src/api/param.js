import { axios } from '@/utils/request'

const modulePath = '/v1/param'

const api = {
  paramList: modulePath + '/page',
  paramAll: modulePath + '/all',
  addParam: modulePath
}

export default api

export function getParamList (parameter) {
  return axios({
    url: api.paramList,
    method: 'get',
    params: parameter
  })
}

export function getParamAll (parameter) {
  return axios({
    url: api.paramAll,
    method: 'get',
    params: parameter
  })
}

export function getParamInfo (paramId) {
  return axios({
    url: modulePath + '/' + paramId,
    method: 'get'
  })
}

export function addParam (parameter) {
  return axios({
    url: api.addParam,
    method: 'post',
    params: parameter
  })
}

export function editParam (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    params: parameter
  })
}

export function delParam (paramId) {
  return axios({
    url: modulePath + '/' + paramId,
    method: 'delete'
  })
}
