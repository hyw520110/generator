import { axios } from '@/utils/request'

const modulePath = '/v1/gallery'

const api = {
  galleryList: modulePath + '/page',
  galleryAll: modulePath + '/all',
  addGallery: modulePath
}

export default api

export function getGalleryList (parameter) {
  return axios({
    url: api.galleryList,
    method: 'get',
    params: parameter
  })
}

export function getGalleryAll (parameter) {
  return axios({
    url: api.galleryAll,
    method: 'get',
    params: parameter
  })
}

export function getGalleryInfo (galleryId) {
  return axios({
    url: modulePath + '/' + galleryId,
    method: 'get'
  })
}

export function addGallery (parameter) {
  return axios({
    url: api.addGallery,
    method: 'post',
    params: parameter
  })
}

export function editGallery (parameter) {
  return axios({
    url: modulePath,
    method: 'put',
    params: parameter
  })
}

export function delGallery (galleryId) {
  return axios({
    url: modulePath + '/' + galleryId,
    method: 'delete'
  })
}
