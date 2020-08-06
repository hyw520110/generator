import router from './router'

import NProgress from 'nprogress' // progress bar
import 'nprogress/nprogress.css' // progress bar style

NProgress.configure({ showSpinner: false }) // NProgress Configuration

router.beforeEach((to, from, next) => {
  if (to.path === '/') {
	next({ path: '/dashboard/workplace' })
  } else {
    NProgress.done()
  }
})

router.afterEach(() => {
  NProgress.done() // finish progress bar
})
