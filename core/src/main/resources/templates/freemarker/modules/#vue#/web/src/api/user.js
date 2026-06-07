import { axios } from '@/utils/request'
import md5 from 'md5'

const modulePath = '/v1/user'

const api = {
  userList: modulePath + '/page',
  userAll: modulePath + '/all',
  addUser: modulePath,
  reset: modulePath + '/reset'
}

export default api

export function getUserList (parameter) {
  return axios({
    url: api.userList,
    method: 'get',
    params: parameter
  })
}

export function getUserAll (parameter) {
  return axios({
    url: api.userAll,
    method: 'get',
    params: parameter
  })
}

export function getUserByGroupId (groupId) {
  return axios({
    url: modulePath + '/group/' + groupId + '/list',
    method: 'get'
  })
}

export function getUserInfo (userId) {
  return axios({
    url: modulePath + '/' + userId,
    method: 'get'
  })
}

export function addUser (parameter) {
  parameter.password = md5(parameter.password)
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

export function resetPwd (parameter) {
  parameter.password = md5(parameter.password)
  return axios({
    url: api.reset,
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
