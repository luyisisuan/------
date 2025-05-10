import { createRouter, createWebHistory } from 'vue-router';
import LoginPage from '../views/LoginPage.vue';
import DashboardPage from '../views/DashboardPage.vue';
import RegisterPage from '../views/RegisterPage.vue';
import SubmitLeavePage from '../views/SubmitLeavePage.vue';
import MyRequestsPage from '../views/MyRequestsPage.vue';
import PendingApprovalsPage from '../views/PendingApprovalsPage.vue';
import AdminUsersPage from '../views/AdminUsersPage.vue'; // 确保路径正确
import LeaveRequestDetailsPage from '../views/LeaveRequestDetailsPage.vue'; // <<--- 导入请假详情页面组件
// import NotFoundPage from '../views/NotFoundPage.vue'; // 假设的404页面

import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
    meta: { requiresGuest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterPage,
    meta: { requiresGuest: true }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: DashboardPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/',
    name: 'Home',
    redirect: () => {
      const authStore = useAuthStore();
      return authStore.isAuthenticated ? { name: 'Dashboard' } : { name: 'Login' };
    }
  },
  {
    path: '/submit-leave',
    name: 'SubmitLeave',
    component: SubmitLeavePage,
    meta: { requiresAuth: true }
  },
  {
    path: '/my-requests',
    name: 'MyRequests',
    component: MyRequestsPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/pending-approvals',
    name: 'PendingApprovals',
    component: PendingApprovalsPage,
    meta: { requiresAuth: true, requiresApprover: true }
  },
  {
     path: '/admin/users',
     name: 'AdminUsers',
     component: AdminUsersPage,
     meta: { requiresAuth: true, requiresAdmin: true }
  },
  { // <<--- 添加请假详情页路由规则
    path: '/leave-details/:id', // 使用路径参数 :id
    name: 'LeaveDetails',      // 给路由命名
    component: LeaveRequestDetailsPage,
    props: true, // 将路由参数（如 :id）作为 props 传递给组件
    meta: { requiresAuth: true } // 查看详情通常需要用户已登录
  },
  // 可选的404路由，放在最后
  // {
  //   path: '/:catchAll(.*)',
  //   name: 'NotFound',
  //   component: NotFoundPage
  // }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

// 全局前置路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore();

  // 确保 authStore 在路由守卫执行前已从 localStorage 初始化状态
  if (!authStore.initialLoadDone && typeof authStore.initialize === 'function') {
      await authStore.initialize();
  }

  const isAuthenticated = authStore.isAuthenticated;
  const requiresAuth = to.meta.requiresAuth;
  const requiresGuest = to.meta.requiresGuest;
  const requiresApprover = to.meta.requiresApprover;
  const requiresAdmin = to.meta.requiresAdmin;

  // 日志可以保留用于调试
  // console.log(`[Router Guard] Navigating to: ${to.fullPath}, From: ${from.fullPath}, Authenticated: ${isAuthenticated}, CanApprove: ${authStore.canApprove}, IsAdmin: ${authStore.isAdmin}, Target Meta:`, to.meta);

  if (requiresAuth && !isAuthenticated) {
    // 需要认证但未登录，重定向到登录页
    next({ name: 'Login', query: { redirect: to.fullPath } });
  } else if (requiresGuest && isAuthenticated) {
    // 访问仅限未认证用户页面但已登录，重定向到仪表盘
    next({ name: 'Dashboard' });
  } else if (requiresApprover && (!isAuthenticated || !authStore.canApprove)) {
    // 需要审批权限但用户未登录或无权限，重定向
    next(isAuthenticated ? { name: 'Dashboard' } : { name: 'Login', query: { redirect: to.fullPath } });
  } else if (requiresAdmin && (!isAuthenticated || !authStore.isAdmin)) {
    // 需要管理员权限但用户未登录或非管理员，重定向
    next(isAuthenticated ? { name: 'Dashboard' } : { name: 'Login', query: { redirect: to.fullPath } });
  }
  else {
    // 其他情况，允许导航
    next();
  }
});

export default router;