import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/index.scss'

/**
 * 应用入口
 * 说明：Element Plus 采用全局注册 + 全量样式引入（不使用 unplugin 自动导入），
 * 避免构建期因插件解析顺序产生的不确定性。
 */
const app = createApp(App)

// 状态管理
app.use(createPinia())
// 路由
app.use(router)
// UI 库（中文语言包）
app.use(ElementPlus, { locale: zhCn })

// 全局注册 Element Plus 图标
Object.entries(ElementPlusIconsVue).forEach(([name, component]) => {
  app.component(name, component as never)
})

// 全局错误兜底：避免白屏，控制台保留堆栈便于排查
app.config.errorHandler = (err, _instance, info) => {
  // eslint-disable-next-line no-console
  console.error('[GlobalError]', info, err)
}

app.mount('#app')

export default app
