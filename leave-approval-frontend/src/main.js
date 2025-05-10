// src/main.js
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import './assets/main.css'; // 全局样式

// --- Element Plus 引入开始 ---
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css'; // 引入 Element Plus 核心 CSS
// 如果您需要 Element Plus 的暗黑模式，可以取消下面这行的注释
// import 'element-plus/theme-chalk/dark/css-vars.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue'; // 引入所有 Element Plus 图标
// --- Element Plus 引入结束 ---


// 创建 Vue 应用
const app = createApp(App);

// --- 全局注册 Element Plus 图标开始 ---
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}
// --- 全局注册 Element Plus 图标结束 ---

// 安装 Pinia
const pinia = createPinia();
app.use(pinia);

// 安装路由
app.use(router);

// --- 全局注册 Element Plus 组件库开始 ---
app.use(ElementPlus);
// --- 全局注册 Element Plus 组件库结束 ---


// （可选）应用启动时检查 Token 有效性
// import { useAuthStore } from './stores/auth';
// const authStore = useAuthStore(); // 如果在这里使用，pinia 需要先被 app.use
// authStore.checkAuthStatus?.();

// 挂载应用
app.mount('#app');