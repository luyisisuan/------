<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import authService from '../services/authService';
import { ElMessage } from 'element-plus';
import { User, Lock, Tickets, InfoFilled, Select as CheckIcon, UserFilled, OfficeBuilding } from '@element-plus/icons-vue'; // 引入更多图标

const router = useRouter();
const authStore = useAuthStore();

const registerFormRef = ref(null);

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  fullName: '',
  managerId: null,
});

const managers = ref([]);
const isLoadingManagers = ref(false);
const managerFetchError = ref(null);
const clientErrors = ref({}); // 保留用于非常规的或前端特有的校验提示

// Element Plus 表单校验规则
const registerRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 40, message: '长度在 6 到 40 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: (rule, value, callback) => {
        if (value === '') {
          callback(new Error('请再次输入密码'));
        } else if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致!'));
        } else {
          callback();
        }
      }, trigger: 'blur'
    },
  ],
  fullName: [
    { required: true, message: '请输入您的真实姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
  ],
  // managerId 可以是可选的，所以不一定需要校验规则，除非业务规定必填
});


const fetchManagers = async () => {
  isLoadingManagers.value = true;
  managerFetchError.value = null;
  try {
    const data = await authService.getPotentialManagers();
    managers.value = data.map(user => ({
        id: user.id,
        displayText: `${user.fullName || user.username} (${Array.isArray(user.roles) ? user.roles.map(r => r.replace('ROLE_', '')).join(', ') : 'N/A'})`
    }));
  } catch (err) {
    console.error("获取经理列表失败 (RegisterPage):", err);
    managerFetchError.value = err.response?.data?.message || err.message || "无法加载经理列表。";
    managers.value = [];
  } finally {
    isLoadingManagers.value = false;
  }
};

onMounted(() => {
  fetchManagers();
});

const handleRegister = async () => {
  clientErrors.value = {}; // 清空旧的自定义客户端错误
  if (authStore.registrationStatus) {
    authStore.registrationStatus.error = null;
    authStore.registrationStatus.isSuccess = false;
  }

  if (!registerFormRef.value) return;
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      const requestData = {
        username: registerForm.username,
        email: registerForm.email,
        password: registerForm.password,
        fullName: registerForm.fullName,
        managerId: registerForm.managerId ? Number(registerForm.managerId) : null,
      };
      const success = await authStore.register(requestData);
      if (success) {
        ElMessage.success('注册成功！请前往登录。');
        // 可以在成功后清空表单，或显示一个不同的成功视图
        // resetForm(); // 如果希望用户可以继续注册下一个
      } else {
        // 错误信息已由 authStore.registrationStatus.error 设置
        // Element Plus 的表单校验会自动处理大部分字段错误
        // 对于非字段相关的服务端错误，可以在模板中用 el-alert 显示 authStore.registrationStatus.error
        if (authStore.registrationStatus.error) {
            ElMessage.error(authStore.registrationStatus.error || '注册失败，请稍后再试。');
        }
      }
    } else {
      ElMessage.error('表单校验失败，请检查输入项。');
      return false;
    }
  });
};

const resetFormFields = () => {
  if (registerFormRef.value) {
    registerFormRef.value.resetFields(); // 使用 Element Plus 表单的重置方法
  }
  // 手动重置不在表单模型中的数据或自定义错误
  registerForm.managerId = null; // 确保 managerId 也被重置
  clientErrors.value = {};
};

const resetFullFormState = () => {
    resetFormFields();
    if (authStore.registrationStatus) {
        authStore.registrationStatus.error = null;
        authStore.registrationStatus.isSuccess = false;
    }
};

</script>

<template>
  <div class="register-page-wrapper-el">
    <el-card class="register-card-el" shadow="always">
      <template #header>
        <div class="card-header-el">
          <el-icon :size="32" color="var(--el-color-primary)"><UserFilled /></el-icon>
          <span>创建您的账户</span>
        </div>
      </template>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
        class="register-form-el"
        @submit.prevent="handleRegister"
        hide-required-asterisk
      >
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="设置您的登录名"
                :prefix-icon="User"
                clearable
                size="large"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="电子邮箱" prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="请输入有效的邮箱地址"
                :prefix-icon="InfoFilled"
                clearable
                size="large"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="设置密码" prop="password">
              <el-input
                type="password"
                v-model="registerForm.password"
                placeholder="至少6位字符"
                :prefix-icon="Lock"
                show-password
                clearable
                size="large"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                type="password"
                v-model="registerForm.confirmPassword"
                placeholder="再次输入密码"
                :prefix-icon="Lock"
                show-password
                clearable
                size="large"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="您的姓名" prop="fullName">
          <el-input
            v-model="registerForm.fullName"
            placeholder="请输入您的真实姓名"
            :prefix-icon="Tickets"
            clearable
            size="large"
          />
        </el-form-item>

        <el-form-item label="选择直属经理 (可选)" prop="managerId">
          <el-select
            v-model="registerForm.managerId"
            placeholder="-- 如果有，请选择您的直属经理 --"
            clearable
            filterable
            size="large"
            style="width: 100%;"
            :loading="isLoadingManagers"
            :prefix-icon="OfficeBuilding"
          >
            <el-option
              v-for="manager in managers"
              :key="manager.id"
              :label="manager.displayText"
              :value="manager.id"
            />
          </el-select>
          <div v-if="isLoadingManagers" class="el-form-item__help">正在加载经理列表...</div>
          <div v-if="managerFetchError" class="el-form-item__error" style="display: block;">{{ managerFetchError }}</div>
        </el-form-item>

        <el-form-item v-if="authStore.registrationStatus?.error && !clientErrors.username && !clientErrors.email && !clientErrors.password && !clientErrors.confirmPassword && !clientErrors.fullName" class="form-item-error">
          <el-alert
            :title="authStore.registrationStatus.error"
            type="error"
            show-icon
            :closable="false"
          />
        </el-form-item>

        <div v-if="authStore.registrationStatus?.isSuccess" class="form-item-success">
           <el-alert
            title="注册成功！"
            type="success"
            show-icon
            :closable="false"
            description="您的账户已创建。现在您可以前往登录页面了。"
          >
             <template #default>
                <span v-if="registerForm.managerId">您选择的经理是：{{ managers.find(m => m.id === Number(registerForm.managerId))?.displayText || '未知' }}。</span>
                请 <el-link type="primary" @click="router.push('/login')">前往登录</el-link>
             </template>
          </el-alert>
        </div>


        <el-form-item class="register-button-container">
          <el-button
            type="primary"
            native-type="submit"
            :loading="authStore.registrationStatus?.isLoading"
            :disabled="authStore.registrationStatus?.isSuccess"
            class="register-submit-btn-el"
            size="large"
            round
          >
            {{ authStore.registrationStatus?.isLoading ? '正在注册...' : '完成注册' }}
          </el-button>
        </el-form-item>

         <el-form-item v-if="authStore.registrationStatus?.isSuccess" class="register-button-container">
            <el-button @click="resetFullFormState" size="large" round class="register-again-btn-el">注册新账户</el-button>
        </el-form-item>

      </el-form>
      <div class="form-footer-links-el">
        <el-text type="info" size="small">已有账户?</el-text>
        <el-link type="primary" @click="router.push('/login')" style="margin-left: 8px;">立即登录</el-link>
      </div>
    </el-card>
    <footer class="page-footer-credit-el">
      <p>© {{ new Date().getFullYear() }} 请假审批系统. All rights reserved.</p>
    </footer>
  </div>
</template>

<style scoped>
.register-page-wrapper-el {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-image: linear-gradient(to top, #cfd9df 0%, #e2ebf0 100%); /* 更柔和的背景 */
  padding: 20px;
  overflow-y: auto; /* 如果内容过多，允许页面滚动 */
}

.register-card-el {
  width: 100%;
  max-width: 650px; /* 注册表单可以稍微宽一些以容纳两列表单项 */
  border-radius: 12px;
  border: none;
  animation: fadeInCard 0.7s ease-out forwards;
  opacity: 0;
  margin-bottom: 2rem; /* 给页脚留出空间 */
}

@keyframes fadeInCard {
  from { opacity: 0; transform: translateY(20px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.card-header-el {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px 0;
  text-align: center;
}
.card-header-el .el-icon { /* 直接对 el-icon 应用样式 */
  color: var(--el-color-primary); /* 使用 Element Plus 主色 */
}
.card-header-el span {
  margin-top: 12px;
  font-size: 1.5rem; /* 调整标题 */
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.register-form-el {
  margin-top: 15px;
}

/* Element Plus 表单项和输入框微调 */
.el-form-item {
  margin-bottom: 20px; /* 统一表单项间距 */
}
.el-form-item__label {
  padding-bottom: 4px !important; /* 减小标签和输入框间距 */
  font-weight: 500 !important;
  color: var(--el-text-color-regular) !important;
}
.el-input--large .el-input__wrapper,
.el-select--large .el-select__wrapper {
  padding: 1px 15px;
  border-radius: 8px !important;
  /* box-shadow: var(--el-box-shadow-light) !important; */
}
.el-select .el-input__prefix .el-icon, /* el-select 也使用 prefix-icon */
.el-input__prefix .el-icon {
  font-size: 16px;
  color: var(--el-text-color-placeholder);
}


.el-alert {
  margin-bottom: 20px;
}

.register-submit-btn-el, .register-again-btn-el {
  width: 100%;
  font-weight: 500;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
}
.register-submit-btn-el:hover:not(.is-disabled),
.register-again-btn-el:hover:not(.is-disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.25);
}
.register-again-btn-el {
  margin-top: 10px; /* “注册新账户”按钮与上方间距 */
  background-color: var(--el-color-info-light-3); /* 使用不同颜色 */
  color: var(--el-color-white);
}
.register-again-btn-el:hover:not(.is-disabled) {
  background-color: var(--el-color-info);
  box-shadow: 0 4px 10px rgba(var(--el-color-info-rgb, 144, 147, 153), 0.25);
}


.form-item-error .el-alert, .form-item-success .el-alert {
  padding: 8px 12px;
}

.form-footer-links-el {
  margin-top: 20px;
  text-align: center;
}
.form-footer-links-el .el-link {
  font-size: 0.9rem;
  vertical-align: baseline;
}

.el-form-item__help { /* Element Plus Select 加载提示 */
  font-size: 0.85em;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.el-form-item__error { /* Element Plus Select 错误提示 */
  font-size: 0.85em;
  /* color: var(--el-color-danger); 由Element Plus自带 */
  margin-top: 4px;
}

.page-footer-credit-el {
  /* position: absolute; */ /* 改为相对定位，使其在内容下方 */
  padding: 1.5rem 0;
  width: 100%;
  text-align: center;
  font-size: 0.8rem;
  color: var(--el-text-color-secondary);
}
</style>