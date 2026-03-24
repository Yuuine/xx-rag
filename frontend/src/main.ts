import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/design-tokens.css'
import './styles/common.css'
import './styles/chat.css'
import './styles/docs.css'
import './styles/workspace.css'
import './styles/app.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
