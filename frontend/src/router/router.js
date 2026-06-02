import { createRouter, createWebHistory } from 'vue-router'
import Welcome from '../components/Welcome.vue'
import Auth from '../components/Auth.vue'
import AiConfig from '../components/AiConfig.vue'
import Chat from '../components/Chat.vue'
import { getToken } from '../utils/auth'

const routes = [
  { path: '/auth', name: 'auth', component: Auth },
  { path: '/', redirect: '/welcome' },
  { path: '/welcome', name: 'welcome', component: Welcome, meta: { requiresAuth: true } },
  { path: '/config', name: 'config', component: AiConfig, meta: { requiresAuth: true } },
  { path: '/chat', name: 'chat', component: Chat, meta: { requiresAuth: true } }
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
    next('/welcome')
    return
  }
  next()
})

export default router
