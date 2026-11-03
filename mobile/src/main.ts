import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import 'vant/lib/index.css'
import './views/assets/styles/global.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')

document.title = import.meta.env.VITE_APP_TITLE || '继续教育学分管理'
