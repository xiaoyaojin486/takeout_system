import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'
import 'vant/lib/index.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
// 全量注册 Vant 组件（移动端项目体积可控，换取开发便利）
app.use(Vant)

app.mount('#app')