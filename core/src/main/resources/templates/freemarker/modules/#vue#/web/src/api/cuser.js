import { axios } from '@/utils/request'

const modulePath = '/v1/user'

const api = {
  userList: modulePath + '/page',
  addUser: modulePath
}

export default api

export function getUserList (parameter) {
  return axios({
    url: api.userList,
    method: 'get',
    params: parameter
  })
}

export function getUserInfo (userId) {
  return axios({
    url: modulePath + '/' + userId,
    method: 'get'
  })
}

export function addUser (parameter) {
  return axios({
    url: api.addUser,
    method: 'post',
    params: parameter
  })
}

export function editUser (userId, parameter) {
  return axios({
    url: modulePath + '/' + userId,
    method: 'put',
    params: parameter
  })
}

export function delUser (userId) {
  return axios({
    url: modulePath + '/' + userId,
    method: 'delete'
  })
}
