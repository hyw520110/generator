import { axios } from '@/utils/request'

const modulePath = '/v1/media'

const api = {
  mediaList: modulePath + '/page',
  mediaAll: modulePath + '/all',
  addMedia: modulePath
}

export default api

export function getMediaList (parameter) {
  return axios({
    url: api.mediaList,
    method: 'get',
    params: parameter
  })
}

export function getMediaAll (parameter) {
  return axios({
    url: api.mediaAll,
    method: 'get',
    params: parameter
  })
}

export function getMediaInfo (id) {
  return axios({
    url: modulePath + '/' + id,
    method: 'get'
  })
}

export function addMedia (parameter) {
  return axios({
    url: api.addMedia,
    method: 'post',
    params: parameter
  })
}

export function editMedia (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    params: parameter
  })
}

export function delMedia (id) {
  console.log('id', id)
  return axios({
    url: modulePath + '/' + id,
    method: 'delete'
  })
}
