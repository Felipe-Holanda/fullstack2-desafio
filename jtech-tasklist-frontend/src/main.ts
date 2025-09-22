import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useAuthStore } from '@/stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Fetch current user on startup if token exists
const auth = useAuthStore()
if (auth.token) {
	auth.fetchMe().finally(() => app.mount('#app'))
} else {
	app.mount('#app')
}
