import { createRouter, createWebHistory } from 'vue-router'
import AuthView from '@/views/AuthView.vue'
import DashboardView from '@/views/DashboardView.vue'
import { getToken } from '@/lib/auth/storage'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
  { path: '/', name: 'auth', component: AuthView },
  { path: '/app', name: 'app', component: DashboardView },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// Simple auth guard using token from storage
router.beforeEach((to) => {
  const token = getToken();
  const isAuthed = !!token;
  if (to.path === '/' && isAuthed) return '/app';
  if (to.path.startsWith('/app') && !isAuthed) return '/';
  return true;
})

export default router
