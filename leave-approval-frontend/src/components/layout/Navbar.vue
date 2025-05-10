<script setup>
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router'; // 引入 useRoute 获取当前路由
import { useAuthStore } from '../../stores/auth';
import { ElMessage } from 'element-plus';
import {
  Ship, // 更换一个有代表性的Logo图标，例如“航行、引领”
  House,
  DocumentAdd,
  Files,
  CircleCheck,
  Setting,
  ArrowDown, // 用于下拉菜单
  SwitchButton,
  UserFilled,
  Promotion,
  User as UserIcon // 用户图标
} from '@element-plus/icons-vue';

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute(); // 获取当前路由信息

const currentUserDisplay = computed(() => {
  if (authStore.currentUser) {
    return authStore.currentUser.fullName || authStore.currentUser.username || '用户';
  }
  return '游客';
});

const canApprove = computed(() => {
  if (!authStore.isAuthenticated || !authStore.userRoles) return false;
  const approverRoles = ['ROLE_TEAM_LEAD', 'ROLE_DEPT_MANAGER', 'ROLE_HR', 'ROLE_ADMIN'];
  return authStore.userRoles.some(role => approverRoles.includes(role));
});

const isAdmin = computed(() => {
  if (!authStore.isAuthenticated || !authStore.userRoles) return false;
  return authStore.userRoles.includes('ROLE_ADMIN');
});

const handleLogout = async () => {
  try {
    await authStore.logout();
    ElMessage.success('您已成功登出。');
  } catch (error) {
    ElMessage.error('登出过程中发生错误。');
  }
};

// el-menu 的激活状态现在依赖于当前路由路径
const activeMenuIndex = computed(() => route.path);

</script>

<template>
  <el-menu
    :default-active="activeMenuIndex"
    class="app-navbar"
    mode="horizontal"
    :ellipsis="false"
    router
  >
    <div class="navbar-logo-container">
      <el-menu-item index="/" class="navbar-logo-item">
        <el-icon :size="28" class="logo-icon"><Ship /></el-icon>
        <span class="logo-text">请假审批系统</span>
      </el-menu-item>
    </div>

    <div class="navbar-menu-items">
      <template v-if="authStore.isAuthenticated">
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/submit-leave">
          <el-icon><DocumentAdd /></el-icon>
          <span>提交请假</span>
        </el-menu-item>
        <el-menu-item index="/my-requests">
          <el-icon><Files /></el-icon>
          <span>我的申请</span>
        </el-menu-item>
        <el-menu-item v-if="canApprove" index="/pending-approvals">
          <el-icon><CircleCheck /></el-icon>
          <span>待我审批</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/admin/users">
          <el-icon><Setting /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </template>
    </div>

    <div class="navbar-actions">
      <template v-if="authStore.isAuthenticated">
        <el-dropdown class="user-dropdown" trigger="click" placement="bottom-end">
          <span class="el-dropdown-link user-dropdown-trigger">
            <el-avatar :size="32" :icon="UserIcon" class="user-avatar" />
            <span class="user-name">{{ currentUserDisplay }}</span>
            <el-icon class="el-icon--right arrow-icon"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :icon="UserFilled" disabled>个人中心</el-dropdown-item>
              <el-dropdown-item :icon="SwitchButton" @click="handleLogout" divided>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <template v-else>
        <el-button type="primary" plain round @click="router.push('/login')" class="auth-button">
          <el-icon style="margin-right: 6px;"><UserFilled /></el-icon>登录
        </el-button>
        <el-button type="success" plain round @click="router.push('/register')" class="auth-button register-button">
          <el-icon style="margin-right: 6px;"><Promotion /></el-icon>注册
        </el-button>
      </template>
    </div>
  </el-menu>
</template>

<style scoped>
/* 全局CSS变量（理想情况下在全局style.css中，这里为了演示） */
:root {
  --navbar-height: 64px;
  --navbar-bg-color: #ffffff; /* 白色背景 */
  --navbar-text-color: #303133; /* Element Plus 主要文字颜色 */
  --navbar-hover-bg-color: #f5f7fa; /* Element Plus 更浅的悬停背景 */
  --navbar-active-text-color: var(--el-color-primary); /* Element Plus 主色 */
  --navbar-border-color: var(--el-border-color-lighter); /* Element Plus 更浅的边框色 */
  --navbar-logo-text-color: var(--el-color-primary);
  --navbar-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  --navbar-transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.app-navbar.el-menu--horizontal {
  height: var(--navbar-height);
  padding: 0 30px; /* 增加左右内边距 */
  background-color: var(--navbar-bg-color);
  border-bottom: 1px solid var(--navbar-border-color);
  box-shadow: var(--navbar-shadow);
  display: flex; /* 使用 Flexbox 进行主轴对齐 */
  align-items: center; /* 垂直居中所有直接子元素 */
}

/* Logo 区域 */
.navbar-logo-container {
  display: flex; /* 确保 el-menu-item 能正常工作 */
  align-items: center;
}
.navbar-logo-item.el-menu-item {
  padding: 0 !important; /* 移除 el-menu-item 的默认 padding */
  border-bottom: none !important;
  background-color: transparent !important;
  height: var(--navbar-height);
  display: flex;
  align-items: center;
}
.navbar-logo-item:hover,
.navbar-logo-item.is-active {
  background-color: transparent !important;
}
.logo-icon {
  color: var(--navbar-logo-text-color);
  margin-right: 10px;
  font-size: 28px; /* 图标大小 */
  transition: transform 0.3s ease;
}
.navbar-logo-item:hover .logo-icon {
  transform: rotate(-10deg) scale(1.1);
}
.logo-text {
  font-size: 1.5rem; /* 调整Logo文字大小 */
  font-weight: 700; /* 加粗 */
  color: var(--navbar-logo-text-color);
  letter-spacing: 0.5px;
}

/* 中间菜单项区域 - 使用 Flexbox 填满剩余空间并将内容居中 */
.navbar-menu-items {
  flex-grow: 1;
  display: flex;
  justify-content: center; /* 菜单项水平居中 */
  align-items: center;
  height: 100%;
}
.navbar-menu-items > .el-menu-item {
  height: var(--navbar-height);
  line-height: var(--navbar-height);
  padding: 0 20px; /* 菜单项左右内边距 */
  border-bottom: none;
  font-size: 1rem; /* 菜单项字体大小 */
  font-weight: 500;
  color: var(--navbar-text-color);
  transition: var(--navbar-transition);
  position: relative; /* 为激活状态的下划线定位 */
}
.navbar-menu-items > .el-menu-item .el-icon {
  margin-right: 8px;
  vertical-align: -0.15em; /* 微调图标垂直对齐 */
}
.navbar-menu-items > .el-menu-item:hover,
.navbar-menu-items > .el-menu-item:focus {
  background-color: var(--navbar-hover-bg-color) !important;
  color: var(--navbar-active-text-color) !important;
}
.navbar-menu-items > .el-menu-item.is-active {
  color: var(--navbar-active-text-color) !important;
  background-color: transparent !important; /* 激活时背景透明 */
}
/* 激活状态的下划线 */
.navbar-menu-items > .el-menu-item.is-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60%; /* 下划线宽度 */
  height: 3px;
  background-color: var(--navbar-active-text-color);
  border-radius: 2px;
  transition: width 0.3s ease;
}
.navbar-menu-items > .el-menu-item:hover::after { /* 悬停时也显示一个细一点的线 */
  width: 40%;
}


/* 右侧操作区域 */
.navbar-actions {
  display: flex;
  align-items: center;
  height: 100%;
  margin-left: auto; /* 确保它在最右侧，即使中间菜单为空 */
}

.user-dropdown {
  display: flex;
  align-items: center;
  padding: 0 15px; /* 调整内边距 */
  height: 100%;
  cursor: pointer;
  transition: background-color 0.2s;
}
.user-dropdown:hover {
  background-color: var(--navbar-hover-bg-color);
}
.user-dropdown-trigger {
  display: flex;
  align-items: center;
  color: var(--navbar-text-color);
  outline: none;
}
.user-avatar {
  margin-right: 10px;
  background-color: var(--el-color-primary-light-7); /* 头像背景色 */
  color: var(--el-color-primary); /* 图标颜色 */
  border: 2px solid transparent;
}
.user-dropdown:hover .user-avatar {
  border-color: var(--el-color-primary-light-5);
}
.user-name {
  font-weight: 500;
  font-size: 0.95rem;
}
.arrow-icon {
  transition: transform 0.2s;
  margin-left: 6px;
}
.user-dropdown:hover .arrow-icon {
  transform: rotate(180deg);
}

/* Element Plus Dropdown Menu Item 的图标 */
:global(.el-dropdown-menu__item .el-icon) {
  margin-right: 10px !important;
  font-size: 16px !important;
}

/* 未登录时的认证按钮 */
.auth-button {
  margin-left: 12px;
  font-weight: 500;
  padding: 8px 18px; /* 调整按钮大小 */
}
.auth-button .el-icon {
  font-size: 16px; /* 按钮内图标大小 */
}
.register-button.el-button--success.is-plain {
  /* 如果使用 plain success，可以微调颜色 */
  /* color: var(--el-color-success); */
  /* border-color: var(--el-color-success-light-3); */
}
.register-button.el-button--success.is-plain:hover {
  /* background-color: var(--el-color-success-light-9); */
}
</style>