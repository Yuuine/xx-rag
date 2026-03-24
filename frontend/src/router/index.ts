import { createRouter, createWebHistory } from 'vue-router'
import ChatWorkspace from '../pages/ChatWorkspace.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'chat', component: ChatWorkspace },
    { path: '/docs', redirect: '/' }
  ]
})

export default router
