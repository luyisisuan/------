<script setup>
import { computed } from 'vue';
import { useAuthStore } from '../stores/auth';
// 导入 Element Plus 图标 (根据您实际使用的图标进行调整)
import {
  DocumentAdd, // 对应“提交新申请”
  Files,       // 对应“我的历史申请”
  CircleCheck, // 对应“待我审批”
  User,        // 对应“用户管理” (如果用User图标)
  Setting,     // 或者用 Setting 图标表示管理
  House,       // 仪表盘/主页图标 (可选，用于头部)
} from '@element-plus/icons-vue';

const authStore = useAuthStore();

const welcomeMessage = computed(() => {
  if (authStore.currentUser && (authStore.currentUser.fullName || authStore.currentUser.username)) {
    return `欢迎回来, ${authStore.currentUser.fullName || authStore.currentUser.username}!`;
  }
  return '欢迎访问请假审批系统';
});

const userRoleDisplay = computed(() => {
  if (authStore.userRoles && authStore.userRoles.length > 0) {
    return authStore.userRoles
      .map(role => {
        const cleanedRole = role.replace('ROLE_', '');
        const roleMap = {
          ADMIN: '管理员', HR: '人事', DEPT_MANAGER: '部门经理',
          TEAM_LEAD: '团队领导', EMPLOYEE: '员工'
        };
        return roleMap[cleanedRole] || (cleanedRole.charAt(0).toUpperCase() + cleanedRole.slice(1).toLowerCase());
      })
      .join(' / ');
  }
  return '访客';
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

const quickActions = computed(() => [
  {
    to: '/submit-leave',
    icon: DocumentAdd, // 使用 Element Plus 图标组件
    title: '发起新申请',
    description: '开始一个新的请假流程。',
    show: authStore.isAuthenticated,
    type: 'success' // Element Plus button/card type
  },
  {
    to: '/my-requests',
    icon: Files,
    title: '我的申请记录',
    description: '查看您提交的所有请假申请。',
    show: authStore.isAuthenticated,
    type: 'primary'
  },
  {
    to: '/pending-approvals',
    icon: CircleCheck,
    title: '待处理审批',
    description: '查看并处理分配给您的审批任务。',
    show: canApprove.value,
    type: 'warning'
  },
  {
    to: '/admin/users',
    icon: Setting, // 或 User
    title: '用户管理中心',
    description: '管理系统用户账户及权限设置。',
    show: isAdmin.value,
    type: 'danger' // 只是示例颜色，可以自定义
  },
]);
</script>

<template>
  <div class="dashboard-container-el">
    <el-card shadow="never" class="header-card-el">
      <div class="dashboard-header-el">
        <el-icon :size="48" class="header-icon-el"><House /></el-icon>
        <div>
          <h1>{{ welcomeMessage }}</h1>
          <p v-if="authStore.isAuthenticated" class="role-info-el">
            当前身份: <el-tag effect="dark" round size="small">{{ userRoleDisplay }}</el-tag>
          </p>
        </div>
      </div>
    </el-card>

    <section v-if="authStore.isAuthenticated" class="quick-actions-section-el">
      <h2 class="section-title-el">快速导航</h2>
      <el-row :gutter="24" class="actions-grid-el">
        <el-col
          v-for="action in quickActions.filter(a => a.show)"
          :key="action.to"
          :xs="24" :sm="12" :md="8" :lg="6"
        >
          <router-link :to="action.to" class="action-router-link">
            <el-card shadow="hover" class="action-card-el" :body-style="{ padding: '0px' }">
              <div class="action-card-content">
                <div :class="['action-icon-area', `action-icon-area--${action.type || 'default'}`]">
                  <el-icon :size="40" class="action-icon-el">
                    <component :is="action.icon" />
                  </el-icon>
                </div>
                <div class="action-text-area">
                  <h3>{{ action.title }}</h3>
                  <p>{{ action.description }}</p>
                </div>
              </div>
            </el-card>
          </router-link>
        </el-col>
      </el-row>
    </section>

    <section v-else class="guest-info-el">
       <el-empty description="您当前未登录">
        <el-button type="primary" @click="$router.push('/login')">前往登录</el-button>
      </el-empty>
    </section>
  </div>
</template>

<style scoped>
.dashboard-container-el {
  max-width: 1200px;
  margin: 2rem auto;
  padding: 1.5rem;
}

.header-card-el {
  margin-bottom: 2.5rem;
  border-radius: 12px;
  /* background-image: linear-gradient(to right, #6a11cb 0%, #2575fc 100%); */
  background-color: var(--el-color-primary-light-9); /* Element Plus 浅主色 */
  border: none;
}

.dashboard-header-el {
  display: flex;
  align-items: center;
  padding: 20px; /* 调整内边距 */
  color: var(--el-color-primary); /* Element Plus 主色 */
}

.header-icon-el {
  margin-right: 25px;
  opacity: 0.8;
}

.dashboard-header-el h1 {
  font-size: 2.2rem;
  margin: 0 0 8px 0;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.role-info-el {
  font-size: 1rem;
  margin: 0;
  color: var(--el-text-color-secondary);
}
.role-info-el .el-tag {
  margin-left: 8px;
  transform: translateY(-1px); /* 微调使其与文本对齐 */
}


.quick-actions-section-el {
  margin-bottom: 2rem;
}

.section-title-el {
  font-size: 1.6rem;
  color: var(--el-text-color-primary);
  margin-bottom: 2rem;
  text-align: center;
  font-weight: 500;
}

.actions-grid-el .el-col {
  margin-bottom: 24px; /* 确保小屏幕堆叠时也有间距 */
}

.action-router-link {
  text-decoration: none;
  display: block; /* 使整个区域可点击 */
  height: 100%;
}

.action-card-el {
  border-radius: 10px;
  transition: transform 0.25s ease-out, box-shadow 0.25s ease-out;
  height: 100%; /* 使卡片在el-col中高度一致 */
  display: flex; /* 用于内部布局 */
  flex-direction: column; /* 使内容垂直排列 */
}

.action-card-el:hover {
  transform: translateY(-5px);
  box-shadow: var(--el-box-shadow-light); /* Element Plus 浅阴影 */
}

.action-card-content {
  display: flex;
  flex-direction: column; /* 图标区和文本区垂直排列 */
  align-items: center; /* 整体内容居中 */
  text-align: center;
  padding: 25px 20px; /* 卡片内容区域内边距 */
  flex-grow: 1; /* 使内容区域填满卡片 */
}

.action-icon-area {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  color: #fff; /* 图标颜色设为白色，背景由类型决定 */
  transition: background-color 0.3s ease;
}

/* 根据 action.type 设置不同的背景色 */
.action-icon-area--primary { background-color: var(--el-color-primary); }
.action-icon-area--success { background-color: var(--el-color-success); }
.action-icon-area--warning { background-color: var(--el-color-warning); }
.action-icon-area--danger  { background-color: var(--el-color-danger); }
.action-icon-area--default { background-color: var(--el-color-info); } /* 默认颜色 */

.action-icon-el {
  /* color 在 .action-icon-area 中已设为 #fff */
}

.action-text-area h3 {
  font-size: 1.15rem;
  margin: 0 0 8px 0;
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.action-text-area p {
  font-size: 0.85rem;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  margin: 0;
  min-height: 40px; /* 给描述一点最小高度，防止卡片高度不一 */
}

.guest-info-el {
  margin-top: 3rem;
  text-align: center;
}
.guest-info-el .el-button {
  margin-top: 1rem;
}

.page-footer-credit-el { /* 页脚样式与之前登录页类似，可以全局定义 */
  position: fixed;
  bottom: 1rem;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.8rem;
  color: var(--el-text-color-placeholder);
}
</style>