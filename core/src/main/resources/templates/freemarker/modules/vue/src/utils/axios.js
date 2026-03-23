const VueAxios = {
  vm: {},
  // eslint-disable-next-line no-unused-vars
  install (app, instance) {
    if (this.installed) {
      return
    }
    this.installed = true

    if (!instance) {
      // eslint-disable-next-line no-console
      console.error('You have to install axios')
      return
    }

    app.config.globalProperties.axios = instance
    app.config.globalProperties.$http = instance
  }
}

export {
  VueAxios
}
