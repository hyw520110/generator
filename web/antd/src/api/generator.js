import { axios } from '@/utils/request'

const moudulePath = '/v1/gen'

const api = {
  tableList: moudulePath + '/tables',
  databases: moudulePath + '/databases',
  step1: moudulePath + '/step1',
  step2: moudulePath + '/step2',
  genCode: moudulePath + '/exec',
  genDoc: moudulePath + '/doc',
  downloads: moudulePath + '/downloads',
  download: moudulePath + '/download',
  relations: moudulePath + '/relations',
  validateOutputDir: moudulePath + '/validateOutputDir',
  config: moudulePath + '/config'
}

export function getTableList (parameter) {
  return axios({
    url: api.tableList,
    method: 'post',
    params: parameter,
    timeout: 10000
  })
}

export function getDatabases (parameter) {
  return axios({
    url: api.databases,
    method: 'post',
    params: parameter,
    timeout: 5000
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
    params: parameter
  })
}

export function genDoc (parameter) {
  return axios({
    url: api.genDoc,
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data: parameter
  })
}

export function getDownloads (parameter) {
  return axios({
    url: api.downloads,
    method: 'get',
    params: parameter
  })
}

export function getTableRelations (parameter) {
  return axios({
    url: api.relations,
    method: 'get',
    params: parameter,
    timeout: 30000
  })
}

export function deleteFile (parameter) {
  return axios({
    url: api.download,
    method: 'delete',
    params: parameter
  })
}

export function validateOutputDir (parameter) {
  return axios({
    url: api.validateOutputDir,
    method: 'get',
    params: parameter,
    timeout: 5000
  })
}

export function getConfig () {
  return axios({
    url: api.config,
    method: 'get',
    timeout: 5000
  })
}