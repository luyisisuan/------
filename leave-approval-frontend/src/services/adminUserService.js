// src/services/adminUserService.js
import api from './api'; // 你的 Axios 实例

class AdminUserService {
  /**
   * 获取所有用户列表 (分页)
   * @param {object} params - 分页和排序参数 (e.g., { page: 0, size: 10, sort: 'id,asc' })
   * @returns {Promise<object>} - 包含分页信息的用户列表 (Page<UserDto>)
   */
  async getAllUsers(params = {}) {
    try {
      const response = await api.get('/admin/users', { params });
      return response.data;
    } catch (error) {
      console.error('Error fetching all users:', error);
      throw error;
    }
  }

  /**
   * 管理员创建新用户
   * @param {object} userData - 用户数据 (AdminUserCreateRequest DTO)
   * @returns {Promise<object>} - 创建的用户信息 (UserDto)
   */
  async createUser(userData) {
    try {
      const response = await api.post('/admin/users', userData);
      return response.data;
    } catch (error) { // <--- 修改点：添加了开始的花括号 {
      console.error('Error creating user:', error);
      throw error;
    } // <--- 这个是 catch 块的结束花括号
  }

  /**
   * 管理员更新用户信息
   * @param {number} userId - 用户ID
   * @param {object} userData - 要更新的用户数据 (UserUpdateRequest DTO)
   * @returns {Promise<object>} - 更新后的用户信息 (UserDto)
   */
  async updateUser(userId, userData) {
    try {
      const response = await api.put(`/admin/users/${userId}`, userData);
      return response.data;
    } catch (error) {
      console.error(`Error updating user ${userId}:`, error);
      throw error;
    }
  }

  /**
   * 管理员删除用户
   * @param {number} userId - 用户ID
   * @returns {Promise<object>} - 删除成功的消息 (MessageResponse)
   */
  async deleteUser(userId) {
    try {
      const response = await api.delete(`/admin/users/${userId}`);
      return response.data;
    } catch (error) {
      console.error(`Error deleting user ${userId}:`, error);
      throw error;
    }
  }

  /**
   * 管理员根据ID获取单个用户信息
   * @param {number} userId - 用户ID
   * @returns {Promise<object>} - 用户信息 (UserDto)
   */
  async getUserById(userId) {
     try {
        const response = await api.get(`/admin/users/${userId}`);
        return response.data;
     } catch (error) {
        console.error(`Error fetching user ${userId}:`, error);
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
      const response = await api.get('/auth/potential-managers');
      return response.data;
    } catch (error) {
      console.error('AdminUserService: Error fetching potential managers:', error);
      throw error;
    }
  }
  // --- 新增方法结束 ---

  // 你可以在这里添加获取所有可用角色的方法，如果后端提供了API
  // async getAvailableRoles() { ... }
}

export default new AdminUserService();