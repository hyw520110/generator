import { axios } from '@/utils/request'

const modulePath = '/v1/gen'
/** 全量代码生成允许的最长等待时间（10 分钟）。 */
const CODE_GENERATION_TIMEOUT_MS = 10 * 60 * 1000

const api = {
  tableList: modulePath + '/tables',
  sqlFiles: modulePath + '/sql-files',
  databases: modulePath + '/databases',
  step1: modulePath + '/step1',
  step2: modulePath + '/step2',
  genCode: modulePath + '/exec',
  genDoc: modulePath + '/doc',
  downloads: modulePath + '/downloads',
  download: modulePath + '/download',
  relations: modulePath + '/relations',
  validateOutputDir: modulePath + '/validateOutputDir',
  config: modulePath + '/config'
}

export function getTableList (parameter) {
  return axios({
    url: api.tableList,
    method: 'post',
    params: parameter,
    timeout: 10000
  })
}

export function uploadSqlFiles (files) {
  const data = new FormData()
  files.forEach(file => data.append('files', file))
  return axios({
    url: api.sqlFiles,
    method: 'post',
    data,
    timeout: 120000
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
    params: parameter,
    timeout: CODE_GENERATION_TIMEOUT_MS
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
