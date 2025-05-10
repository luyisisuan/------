// src/services/leaveService.js
import api from './api'; // 确保 api.js 已经配置好 axios 实例，并可能包含了 token 拦截器

class LeaveService {
  /**
   * 提交新的请假申请
   * @param {object} leaveRequestData - 包含请假类型、开始/结束日期、理由等的对象
   * @returns {Promise<object>} - 提交成功的请假申请详情 (LeaveRequestViewDto)
   */
  async submitLeave(leaveRequestData) {
    try {
      const response = await api.post('/leave-requests', leaveRequestData);
      return response.data; // 返回后端返回的 LeaveRequestViewDto
    } catch (error) {
      console.error('Error submitting leave request:', error);
      throw error; // 将错误向上抛出，以便组件处理
    }
  }

  /**
   * 获取当前用户提交的请假申请列表
   * @param {object} params - 分页和排序参数 (e.g., { page: 0, size: 10, sort: 'createdAt,desc' })
   * @returns {Promise<object>} - 包含分页信息的请假申请列表 (Page<LeaveRequestViewDto>)
   */
  async getMyLeaveRequests(params = {}) {
    try {
      const response = await api.get('/leave-requests/my-requests', { params });
      return response.data;
    } catch (error) {
      console.error('Error fetching my leave requests:', error);
      throw error;
    }
  }

  /**
   * 获取当前用户待审批的请假申请列表
   * @param {object} params - 分页、排序和状态参数 (e.g., { page: 0, size: 10, sort: 'createdAt,asc', status: 'PENDING_APPROVAL' })
   * @returns {Promise<object>} - 包含分页信息的请假申请列表 (Page<LeaveRequestViewDto>)
   */
   async getPendingMyApprovalRequests(params = {}) {
     try {
       const response = await api.get('/leave-requests/pending-approval', { params });
       return response.data;
     } catch (error) {
       console.error('Error fetching pending approval requests:', error);
       throw error;
     }

   } /**
       * 获取当前用户待审批的请假申请列表
       * @param {object} params - 分页、排序和状态参数 (e.g., { page: 0, size: 10, sort: 'createdAt,asc', status: 'PENDING_APPROVAL' })
       * @returns {Promise<object>} - 包含分页信息的请假申请列表 (Page<LeaveRequestViewDto>)
       */
       async getPendingMyApprovalRequests(params = {}) { // 方法名可以保持不变，或者也改成 getVisiblePendingRequests
         try {
           // **修改这里的 API 路径**
           const response = await api.get('/leave-requests/pending-approvals', { params }); // <<--- 从 /pending-my-approval 改为 /pending-approvals
           return response.data;
         } catch (error) {
           console.error('Error fetching pending approval requests:', error);
           throw error;
         }
       }

   /**
    * 获取单个请假申请详情
    * @param {number} leaveRequestId - 请假申请ID
    * @returns {Promise<object>} - 请假申请详情 (LeaveRequestViewDto)
    */
    async getLeaveRequestDetails(leaveRequestId) {
        try {
            const response = await api.get(`/leave-requests/${leaveRequestId}`);
            return response.data;
        } catch (error) {
            console.error(`Error fetching leave request details for ID ${leaveRequestId}:`, error);
            throw error;
        }
    }

   /**
    * 审批请假申请 (批准/驳回)
    * @param {number} leaveRequestId - 请假申请ID
    * @param {object} actionData - 包含 decision ('APPROVED'/'REJECTED') 和 comments 的对象
    * @returns {Promise<object>} - 更新后的请假申请详情 (LeaveRequestViewDto)
    */
   async processApproval(leaveRequestId, actionData) {
        try {
            const response = await api.post(`/leave-requests/${leaveRequestId}/action`, actionData);
            return response.data;
        } catch (error) {
            console.error(`Error processing approval for request ID ${leaveRequestId}:`, error);
            throw error;
        }
   }

   /**
    * 取消请假申请
    * @param {number} leaveRequestId - 请假申请ID
    * @returns {Promise<object>} - 更新后的请假申请详情 (LeaveRequestViewDto)
    */
    async cancelLeaveRequest(leaveRequestId) {
         try {
            const response = await api.post(`/leave-requests/${leaveRequestId}/cancel`);
            return response.data;
         } catch (error) {
             console.error(`Error cancelling leave request ID ${leaveRequestId}:`, error);
             throw error;
         }
    }

  // 可以添加获取请假类型列表的API调用，如果后端提供了
  // async getLeaveTypes() { ... }

}

// 导出一个 LeaveService 的实例
export default new LeaveService();