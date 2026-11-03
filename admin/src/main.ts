import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import { applySkinVariables } from './theme'
import './style.css'

dayjs.locale('zh-cn')
applySkinVariables()

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Antd)
app.provide('locale', zhCN)
app.mount('#app')
