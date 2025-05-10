// src/stores/auth.js
import { defineStore } from 'pinia';
import authService from '../services/authService'; // 确保导入 authService
import router from '../router'; // 导入 router 用于登录/登出后的导航

export const useAuthStore = defineStore('auth', {
  state: () => ({
    // 认证状态
    isAuthenticated: false,    // 用户是否已登录
    userToken: null,           // JWT Token
    currentUser: null,         // 当前用户对象 (例如 { id, username, fullName, email })
    userRoles: [],             // 当前用户的角色列表 (例如 ['ROLE_EMPLOYEE', 'ROLE_ADMIN'])

    // 登录过程的状态
    loginStatus: {
      isLoading: false,        // 是否正在登录
      error: null,             // 登录错误信息
    },

    // 注册过程的状态
    registrationStatus: {
      isLoading: false,        // 是否正在注册
      error: null,             // 注册错误信息
      isSuccess: false,        // 注册是否成功
    },

    // 标记是否已尝试从 localStorage 加载初始认证状态
    initialLoadDone: false,
  }),

  getters: {
    /**
     * 判断当前用户是否具有审批权限。
     * 审批权限通常包括团队领导、部门经理、HR 或管理员。
     * @param {object} state - 当前 store 的 state。
     * @returns {boolean} 如果用户具有审批权限则返回 true，否则返回 false。
     */
    canApprove: (state) => {
      if (!state.isAuthenticated || !Array.isArray(state.userRoles) || state.userRoles.length === 0) {
        // console.log('[AuthStore Getter] canApprove: User not authenticated or no roles defined, returning false.');
        return false; // 未认证或角色列表无效，则无审批权限
      }
      const approverRoles = ['ROLE_TEAM_LEAD', 'ROLE_DEPT_MANAGER', 'ROLE_HR', 'ROLE_ADMIN'];
      const hasApproverRole = state.userRoles.some(role => approverRoles.includes(role));
      // console.log(`[AuthStore Getter] canApprove: User roles: [${state.userRoles.join(', ')}], Has approver role: ${hasApproverRole}`);
      return hasApproverRole;
    },

    /**
     * 判断当前用户是否是管理员。
     * @param {object} state - 当前 store 的 state。
     * @returns {boolean} 如果用户是管理员则返回 true，否则返回 false。
     */
    isAdmin: (state) => {
      if (!state.isAuthenticated || !Array.isArray(state.userRoles) || state.userRoles.length === 0) {
        // console.log('[AuthStore Getter] isAdmin: User not authenticated or no roles defined, returning false.');
        return false; // 未认证或角色列表无效，则不是管理员
      }
      const isAdminRole = state.userRoles.includes('ROLE_ADMIN');
      // console.log(`[AuthStore Getter] isAdmin: User roles: [${state.userRoles.join(', ')}], Is admin: ${isAdminRole}`);
      return isAdminRole;
    },

    /**
     * 获取当前用户的 JWT Token (如果存在)。
     * @param {object} state - 当前 store 的 state。
     * @returns {string | null} JWT Token 或 null。
     */
    token: (state) => state.userToken,
  },

  actions: {
    /**
     * 初始化认证状态。
     * 尝试从 localStorage 加载之前保存的用户 Token、用户信息和角色信息。
     * 这个方法应该在应用启动时被调用一次 (例如在 main.js 中)。
     */
    async initialize() {
      // 防止在应用生命周期内重复执行不必要的初始化
      if (this.initialLoadDone) {
        console.log('authStore: Initialization already performed.');
        return;
      }

      console.log('authStore: Attempting to initialize authentication state from localStorage...');
      const token = localStorage.getItem('userToken');
      const userString = localStorage.getItem('currentUser');
      const rolesString = localStorage.getItem('userRoles');

      if (token && userString && rolesString) {
        try {
          const user = JSON.parse(userString);
          const roles = JSON.parse(rolesString);

          // 基本校验，确保解析出的 roles 是一个数组
          if (user && typeof user === 'object' && Array.isArray(roles)) {
            this.userToken = token;
            this.currentUser = user;
            this.userRoles = roles;
            this.isAuthenticated = true;
            console.log('authStore: Successfully initialized from localStorage. User:', this.currentUser.username);
            // 可选：在这里可以添加一步后端调用，验证 token 是否仍然有效，或者刷新 token
            // 例如: await this.verifyTokenAndRefreshUserInfo();
          } else {
            console.warn('authStore: Invalid user or roles data found in localStorage. Clearing auth state.');
            this.clearAuthData(); // 数据无效，执行登出逻辑
          }
        } catch (e) {
          console.error('authStore: Error parsing authentication data from localStorage:', e);
          this.clearAuthData(); // 解析失败也登出
        }
      } else {
        console.log('authStore: No authentication data found in localStorage.');
        // 确保在没有数据时，状态是清晰的未认证状态
        this.clearAuthData(false); // false 表示不需要跳转，因为可能正在初始化
      }
      this.initialLoadDone = true; // 标记初始化已完成
    },


    /**
     * 处理用户登录。
     * @param {object} credentials - 包含 username 和 password 的对象。
     * @returns {Promise<boolean>} 返回 true 表示登录成功，false 表示登录失败。
     */
    async login(credentials) {
      this.loginStatus.isLoading = true;
      this.loginStatus.error = null;
      // 在尝试登录前，先确保任何旧的认证状态被清除，避免状态不一致
      this.clearAuthData(false); // false 表示不立即跳转，因为登录失败时也不应该跳转

      try {
        // 调用认证服务进行登录 API 调用
        const responseData = await authService.login(credentials); // 假设 authService.login 返回 JwtResponse

        // 保存认证信息到 state
        this.userToken = responseData.token;
        this.currentUser = { // 根据后端 JwtResponse 的字段构建 currentUser 对象
             id: responseData.id,
             username: responseData.username,
             email: responseData.email,
             // 确保 fullName 有回退值，如果后端可能不返回该字段
             fullName: responseData.fullName || responseData.username
        };
        // 确保 userRoles 总是数组
        this.userRoles = Array.isArray(responseData.roles) ? responseData.roles : [];
        this.isAuthenticated = true;

        // 保存认证信息到 localStorage，以便页面刷新后恢复状态
        localStorage.setItem('userToken', this.userToken);
        localStorage.setItem('currentUser', JSON.stringify(this.currentUser));
        localStorage.setItem('userRoles', JSON.stringify(this.userRoles));

        console.log('authStore: Login successful. User:', this.currentUser.username);

        // 登录成功后跳转
        // 检查路由中是否有 redirect 查询参数，以便跳转回用户最初想访问的页面
        const redirectPath = router.currentRoute.value.query.redirect || '/dashboard';
        router.push(redirectPath); // 使用 router 实例进行导航

        return true; // 返回 true 表示登录成功

      } catch (error) {
        console.error('authStore: Login failed:', error);
        // 从错误响应中提取错误信息
        if (error.response && error.response.data && error.response.data.message) {
           this.loginStatus.error = error.response.data.message;
        } else if (error.message) { // 网络错误或其他 Axios 错误
           this.loginStatus.error = error.message;
        } else {
           this.loginStatus.error = '登录失败，请检查您的凭据或网络连接。';
        }
        // 登录失败时，确保所有认证状态都是清除的
        this.clearAuthData(false);
        return false; // 返回 false 表示登录失败

      } finally {
        this.loginStatus.isLoading = false;
      }
    },

    /**
     * 内部方法：清除前端认证状态和 localStorage 中的数据。
     * @param {boolean} [performRedirect=true] - 是否在清除后执行重定向到登录页。
     */
    clearAuthData(performRedirect = true) {
      this.isAuthenticated = false;
      this.userToken = null;
      this.currentUser = null;
      this.userRoles = [];
      localStorage.removeItem('userToken');
      localStorage.removeItem('currentUser');
      localStorage.removeItem('userRoles');
      console.log('authStore: Authentication data cleared from state and localStorage.');
      if (performRedirect) {
        router.push('/login'); // 确保重定向到登录页
      }
    },

    /**
     * 处理用户登出。
     * 调用内部的 clearAuthData 方法，并确保用户被重定向到登录页。
     */
    async logout() {
      // 如果后端有登出API，可以在这里调用 authService.logoutApi();
      // try { await authService.logoutApi(); } catch(e) { console.error("API logout failed", e); }
      this.clearAuthData(true); // true 表示需要重定向到登录页
    },

    /**
     * 处理用户注册。
     * @param {object} userData - 包含 username, email, password, fullName 的对象。
     * @returns {Promise<boolean>} 返回 true 表示注册成功，false 表示注册失败。
     */
    async register(userData) {
      this.registrationStatus.isLoading = true;
      this.registrationStatus.error = null;
      this.registrationStatus.isSuccess = false;

      try {
        const responseData = await authService.register(userData); // 假设返回 MessageResponse

        console.log('authStore: Registration API call successful:', responseData.message);
        this.registrationStatus.isSuccess = true; // 设置成功状态
        // 注册成功后，不自动登录，让用户手动登录
        return true; // 返回 true 表示注册API调用成功

      } catch (error) {
        console.error('authStore: Registration failed:', error);
        // 从错误响应中提取错误信息
        if (error.response && error.response.data) {
           if (error.response.data.message) {
              // 通用错误，如用户名/邮箱已存在
              this.registrationStatus.error = error.response.data.message;
           } else if (typeof error.response.data === 'object') {
               // 字段级别校验错误 (如果后端返回这种结构)
               const fieldErrors = error.response.data;
               let errorMessage = '注册失败：';
               for (const field in fieldErrors) {
                   errorMessage += `${field}: ${fieldErrors[field]}; `;
               }
               this.registrationStatus.error = errorMessage;
           } else {
               // 其他格式的后端错误
               this.registrationStatus.error = '注册时发生未知服务端错误。';
           }
        } else {
           // 网络错误或其他非HTTP错误
           this.registrationStatus.error = '网络或服务器连接失败，请稍后再试。';
        }
        this.registrationStatus.isSuccess = false; // 注册失败
        return false; // 返回 false 表示注册API调用失败

      } finally {
        this.registrationStatus.isLoading = false;
      }
    },
  },
});