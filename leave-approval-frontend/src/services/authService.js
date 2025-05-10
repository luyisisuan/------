// src/services/authService.js
import api from './api'; // 确保导入配置好的 Axios 实例，其路径相对于当前文件

class AuthService {
  /**
   * 用户登录
   * @param {object} credentials - 包含 username 和 password 的对象
   * @returns {Promise<object>} - 登录成功后返回的 JWT 响应对象 (JwtResponse)
   */
  async login(credentials) {
    try {
      const response = await api.post('/auth/login', credentials);
      return response.data;
    } catch (error) {
      console.error('AuthService: Error during login:', error);
      throw error;
    }
  }

  /**
   * 用户注册
   * @param {object} userData - 包含 username, email, password, fullName, 和可选的 managerId 的对象
   * @returns {Promise<object>} - 注册成功后返回的消息对象 (MessageResponse)
   */
  async register(userData) {
    try {
      // userData 现在应该包含 managerId (如果前端传递了)
      const response = await api.post('/auth/register', userData);
      return response.data;
    } catch (error) {
      console.error('AuthService: Error during registration:', error);
      throw error;
    }
  }

  // --- 新增方法：获取可选的经理列表 ---
  /**
   * 获取可作为经理的用户列表。
   * @returns {Promise<Array<object>>} - 一个包含用户DTO的数组，每个对象至少有 id, fullName, username, roles。
   */
  async getPotentialManagers() {
    try {
      // 调用我们后端在 AuthController 中创建的 /api/auth/potential-managers 端点
      // 注意：如果这个API需要认证（例如，只有登录用户才能查看），
      // 您的 api 实例 (Axios 实例) 应该配置了自动携带JWT Token的拦截器。
      // 如果您的 api 实例没有自动处理 token，您可能需要在这里手动添加 Authorization header，
      // 例如从 authStore 获取 token。
      const response = await api.get('/auth/potential-managers');
      return response.data; // 后端应该返回 UserDto 列表
    } catch (error) {
      console.error('AuthService: Error fetching potential managers:', error);
      throw error; // 将错误向上抛出，以便调用方（如Vue组件）可以捕获和处理
    }
  }
  // --- 新增方法结束 ---

  // 如果需要，可以在这里添加其他与认证相关的 API 调用方法，例如：
  // async getCurrentUser() { ... }
  // async logoutApi() { ... }
}

export default new AuthService();