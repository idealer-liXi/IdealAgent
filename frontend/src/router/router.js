import { createRouter, createWebHistory } from 'vue-router'
import Welcome from '../components/Welcome.vue'
import Auth from '../components/Auth.vue'
import AiConfig from '../components/AiConfig.vue'
import Chat from '../components/Chat.vue'
import Work from '../components/Work.vue'
import AgentAdmin from '../components/AgentAdmin.vue'
import AgentFlow from '../components/AgentFlow.vue'
import AgentCanvas from '../components/AgentCanvas.vue'
import AgentDisplay from '../components/AgentDisplay.vue'
import McpDisplay from '../components/McpDisplay.vue'
import { getToken, isAdmin } from '../utils/auth'

const routes = [
  { path: '/auth', name: 'auth', component: Auth },
  { path: '/', redirect: '/welcome' },
  { path: '/welcome', name: 'welcome', component: Welcome, meta: { requiresAuth: true } },
  { path: '/config', name: 'config', component: AiConfig, meta: { requiresAuth: true, adminOnly: true } },
  { path: '/chat', name: 'chat', component: Chat, meta: { requiresAuth: true } },
  { path: '/work', name: 'work', component: Work, meta: { requiresAuth: true } },
  { path: '/agent-display', name: 'agent-display', component: AgentDisplay, meta: { requiresAuth: true } },
  { path: '/mcp-display', name: 'mcp-display', component: McpDisplay, meta: { requiresAuth: true } },
  { path: '/agents', name: 'agents', component: AgentAdmin, meta: { requiresAuth: true, adminOnly: true } },
  { path: '/agents/flow', name: 'agent-flow', component: AgentFlow, meta: { requiresAuth: true, adminOnly: true } },
  { path: '/agents/canvas', name: 'agent-canvas', component: AgentCanvas, meta: { requiresAuth: true, adminOnly: true } }
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
  if (to.meta.adminOnly && !isAdmin()) {
    next('/welcome')
    return
  }
  if (to.path === '/auth' && getToken()) {
    next('/welcome')
    return
  }
  next()
})

export default router
