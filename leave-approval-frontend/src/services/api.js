import axios from 'axios';
import { useAuthStore } from '../stores/auth'; // 导入 auth store
// import router from '../router'; // 如果需要在拦截器中处理路由跳转

const apiClient = axios.create({
  baseURL: '/api', // Vite 会将这个代理到 http://localhost:8080/api
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 在 Pinia store 初始化完成之后才能在拦截器内部安全地使用 useAuthStore()
    // 如果在模块加载时（顶层）就调用 useAuthStore() 可能会因为 store 未准备好而出错
    // 通常在 Vue 应用的上下文中（如组件内或 action 内）使用是安全的
    // 对于拦截器，一个常见的做法是确保 store 已经初始化
    // 或者，如果 token 直接存在于 localStorage，也可以先从那里读取
    const authStore = useAuthStore(); // 在拦截器函数内部调用以获取当前store实例
    if (authStore.token) {
      config.headers['Authorization'] = `Bearer ${authStore.token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器 (可以根据需要添加更详细的错误处理)
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => { // 将此函数设为 async 以便可以使用 await
    const originalRequest = error.config;
    const authStore = useAuthStore();

    if (error.response) {
      // 如果是 401 未授权错误，并且不是由于正在尝试刷新 token（如果实现了刷新逻辑）
      if (error.response.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true; // 标记此请求已尝试过一次
        console.warn('[API Interceptor] Unauthorized (401). Logging out.');
        authStore.logout(); // 触发登出 action
        // router.push('/login'); // logout action 内部已经处理了跳转
        // 也可以选择尝试刷新 token，如果你的后端支持
        // try {
        //   await authStore.refreshToken(); // 假设有这个 action
        //   return apiClient(originalRequest); // 重试原始请求
        // } catch (refreshError) {
        //   authStore.logout();
        //   return Promise.reject(refreshError);
        // }
      } else if (error.response.status === 403) {
        console.warn('[API Interceptor] Forbidden (403). User does not have permission.');
        // alert('您没有权限执行此操作。'); // 简单的用户提示
        // router.push('/unauthorized'); // 或者跳转到无权限页面
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;