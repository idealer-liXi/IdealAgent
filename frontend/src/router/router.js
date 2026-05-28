import { createRouter, createWebHistory } from 'vue-router'
import Welcome from '../components/Welcome.vue'
import Auth from '../components/Auth.vue'
import { getToken } from '../utils/auth'

const routes = [
  { path: '/auth', name: 'auth', component: Auth },
  { path: '/', name: 'welcome', component: Welcome, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !getToken()) {
    next('/auth')
    return
  }
  if (to.path === '/auth' && getToken()) {
    next('/')
    return
  }
  next()
})

export default router
