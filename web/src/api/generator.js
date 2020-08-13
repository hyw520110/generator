import { axios } from '@/utils/request'

const moudulePath = '/v1/gen'

const api = {
  tableList: moudulePath + '/tables',
  step1: moudulePath + '/step1',
  step2: moudulePath + '/step2',
  genCode: moudulePath + '/exec'
}

export function getTableList (parameter) {
  return axios({
    url: api.tableList,
    method: 'post',
    params: parameter
  })
}
export function step1 (parameter) {
  return axios({
    url: api.step1,
    method: 'post',
    params: parameter
  })
}
export function step2 (parameter) {
  return axios({
    url: api.step2,
    method: 'post',
    params: parameter
  })
}
export function genCode (parameter) {
  return axios({
    url: api.genCode,
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: parameter
  })
}
