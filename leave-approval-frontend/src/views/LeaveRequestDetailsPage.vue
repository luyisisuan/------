<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import leaveService from '../services/leaveService';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const leaveRequest = ref(null);
const isLoading = ref(true);
const error = ref(null);

const leaveRequestId = computed(() => route.params.id);

const formatLeaveType = (type) => {
  const types = {
    ANNUAL_LEAVE: '年假', SICK_LEAVE: '病假',
    PERSONAL_LEAVE: '事假', MATERNITY_LEAVE: '产假',
    PATERNITY_LEAVE: '陪产假', BEREAVEMENT_LEAVE: '丧假',
    UNPAID_LEAVE: '无薪假', OTHER: '其他',
  };
  return types[type] || type;
};

const formatLeaveStatus = (status) => {
  const statuses = {
    PENDING_APPROVAL: '待审批', APPROVED: '已批准',
    REJECTED: '已驳回', CANCELLED: '已取消',
    PROCESSING: '处理中', // 如果您后端有这个状态
  };
  return statuses[status] || status;
};

const formatDecision = (decision) => {
  const decisions = { APPROVED: '批准', REJECTED: '驳回', CANCELLED: '已取消' }; // 添加CANCELLED的格式化
  return decisions[decision] || decision;
};

const fetchLeaveRequestDetails = async (id) => {
  isLoading.value = true;
  error.value = null;
  leaveRequest.value = null;
  if (!id) {
    error.value = '无效的请假申请ID。';
    isLoading.value = false;
    return;
  }
  try {
    // 确保 leaveService.getLeaveRequestDetails(id) 返回的数据包含 currentApprover 对象
    // 例如: { ..., currentApprover: { id: 1, fullName: '审批人张三', username: 'zhangsan' } }
    leaveRequest.value = await leaveService.getLeaveRequestDetails(id);
  } catch (err) {
    if (err.response?.status === 404) {
      error.value = '未找到指定的请假申请。';
    } else {
      error.value = err.response?.data?.message || err.message || '获取请假详情失败，请稍后再试。';
    }
  } finally {
    isLoading.value = false;
  }
};

watch(leaveRequestId, (newId) => {
  fetchLeaveRequestDetails(newId);
}, { immediate: true });

const canCurrentUserActOnRequest = computed(() => {
  if (!authStore.isAuthenticated || !leaveRequest.value) return false;
  // 确保比较的是 leaveRequest.value.statusEnum （如果后端返回的是枚举键）
  // 或者确保 leaveRequest.value.status 是 'PENDING_APPROVAL' 字符串
  const statusToCheck = leaveRequest.value.statusEnum || leaveRequest.value.status;

  if (authStore.isAdmin) {
    return statusToCheck === 'PENDING_APPROVAL';
  }
  const cur = leaveRequest.value.currentApprover;
  return cur && cur.id === authStore.currentUser.id && statusToCheck === 'PENDING_APPROVAL';
});

const selectedAction = ref('');
const approvalComments = ref('');
const isActionModalVisible = ref(false);
const isSubmittingAction = ref(false);
const actionError = ref(null);

const openActionModal = (action) => {
  selectedAction.value = action;
  approvalComments.value = '';
  actionError.value = null;
  isActionModalVisible.value = true;
};

const closeActionModal = () => {
  isActionModalVisible.value = false;
};

const submitApprovalAction = async () => {
  if (!selectedAction.value) return;
  isSubmittingAction.value = true;
  actionError.value = null;
  try {
    const dto = { decision: selectedAction.value, comments: approvalComments.value.trim() };
    // 后端 processApproval 返回更新后的 leaveRequest 对象
    const updatedLeaveRequest = await leaveService.processApproval(leaveRequest.value.id, dto);
    leaveRequest.value = updatedLeaveRequest; // 更新本地数据以刷新UI
    alert(`请假申请已成功 ${formatDecision(selectedAction.value)}！`);
    closeActionModal();
  } catch (err) {
    actionError.value = err.response?.data?.message || '审批操作失败，请稍后再试。';
  } finally {
    isSubmittingAction.value = false;
  }
};
</script>

<template>
  <div class="leave-details-container">
    <!-- 顶部返回 + 标题 + 状态 -->
    <div class="details-header">
      <button class="btn-back" @click="router.back()">← 返回</button>
      <h2>请假申请详情 (ID: {{ leaveRequest?.id }})</h2>
      <!-- 确保使用正确的状态字段，可能是 leaveRequest.statusEnum 或 leaveRequest.status -->
      <span v-if="leaveRequest" class="status-badge">{{ formatLeaveStatus(leaveRequest.statusEnum || leaveRequest.status) }}</span>
    </div>

    <div v-if="isLoading" class="loading-spinner">正在加载请假详情...</div>
    <div v-else-if="error" class="alert alert-danger">{{ error }}</div>
    <div v-else-if="leaveRequest" class="leave-details-card">
      <!-- 基本信息 -->
      <section class="request-info">
        <h3>基本信息</h3>
        <div class="info-grid">
          <div><strong>申请人:</strong> {{ leaveRequest.applicant?.fullName || leaveRequest.applicant?.username || 'N/A' }}</div>
          <div><strong>请假类型:</strong> {{ formatLeaveType(leaveRequest.leaveType) }}</div>
          <div><strong>开始日期:</strong> {{ leaveRequest.startDate }}</div>
          <div><strong>结束日期:</strong> {{ leaveRequest.endDate }}</div>
          <div><strong>请假天数:</strong> {{ leaveRequest.leaveDurationInDays }} 天</div>
          <div><strong>提交时间:</strong> {{ new Date(leaveRequest.createdAt).toLocaleString('zh-CN') }}</div>

          <!-- 新增：显示当前审批人 -->
          <div v-if="leaveRequest.currentApprover">
            <strong>当前审批人:</strong> {{ leaveRequest.currentApprover.fullName || leaveRequest.currentApprover.username || 'N/A' }}
          </div>
          <!-- 如果是待审批状态但没有明确的审批人，给一个通用提示 -->
          <div v-else-if="(leaveRequest.statusEnum || leaveRequest.status) === 'PENDING_APPROVAL'">
            <strong>当前审批人:</strong> <span>处理中/待分配</span>
          </div>
          <!-- 如果不是 PENDING_APPROVAL 状态，且没有 currentApprover (例如已批准/已驳回)，则不显示“当前审批人”行 -->


          <div class="full-width">
            <strong>请假理由:</strong>
            <pre>{{ leaveRequest.reason }}</pre>
          </div>
          <div v-if="leaveRequest.attachmentsPath" class="full-width">
            <strong>附件:</strong>
            <a :href="leaveRequest.attachmentsPath" target="_blank">查看附件</a>
          </div>
        </div>
      </section>

      <!-- 审批历史 -->
      <section class="approval-history">
        <h3>审批历史</h3>
        <ul v-if="leaveRequest.approvalHistory?.length">
          <!-- 确保 h.approver 存在且有 fullName 或 username -->
          <!-- 确保 h.timestamp 或 h.approvedAt 存在 -->
          <li v-for="h in leaveRequest.approvalHistory" :key="h.id" class="history-item">
            <div class="history-actor">
              <strong>{{ h.approver?.fullName || h.approver?.username || '系统' }}</strong>
              于 {{ new Date(h.timestamp || h.approvedAt).toLocaleString('zh-CN') }} <!-- 使用后端实际返回的时间戳字段 -->
            </div>
            <div :class="`history-decision-${h.decision.toLowerCase()}`">
              {{ formatDecision(h.decision) }}
            </div>
            <div v-if="h.comments" class="history-comments">意见: {{ h.comments }}</div>
          </li>
        </ul>
        <p v-else>暂无审批记录。</p>
      </section>

      <!-- 操作按钮 -->
      <section v-if="canCurrentUserActOnRequest" class="action-buttons">
        <button class="btn btn-approve" @click="openActionModal('APPROVED')" :disabled="isSubmittingAction">
          批准申请
        </button>
        <button class="btn btn-reject" @click="openActionModal('REJECTED')" :disabled="isSubmittingAction">
          驳回申请
        </button>
      </section>
    </div>

    <div v-else class="no-data">无法加载请假申请数据。</div>

    <!-- 审批模态框 -->
    <div v-if="isActionModalVisible" class="modal-overlay" @click.self="closeActionModal">
      <div class="modal-content">
        <h3>审批操作 - {{ formatDecision(selectedAction) }}</h3>
        <form @submit.prevent="submitApprovalAction">
          <div class="form-group">
            <label>审批意见 (可选)：</label>
            <textarea v-model="approvalComments" rows="4" placeholder="请输入您的审批意见..."></textarea>
          </div>
          <div v-if="actionError" class="alert alert-danger">{{ actionError }}</div>
          <div class="modal-actions">
            <button type="submit" class="btn btn-primary" :disabled="isSubmittingAction">
              <span v-if="isSubmittingAction" class="spinner"></span>
              确认
            </button>
            <button type="button" class="btn btn-secondary" @click="closeActionModal" :disabled="isSubmittingAction">
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<!-- CSS 样式部分保持不变 -->
<style scoped>
.leave-details-container {
  max-width: 800px;
  margin: 2rem auto;
  padding: 1rem;
}

.details-header {
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
}
.btn-back {
  background: #fff;
  border: 1px solid #ccc;
  padding: 0.4rem 0.8rem;
  border-radius: 4px;
  cursor: pointer;
  transition: background .2s;
}
.btn-back:hover {
  background: #eee;
}
.details-header h2 {
  flex: 1;
  text-align: center;
  font-size: 1.6rem;
  color: #333;
  margin: 0;
}
.status-badge {
  padding: 0.3rem 0.8rem;
  border-radius: 12px;
  background: #ffd666; /* 默认一个颜色，可以根据状态改变 */
  color: #664e27;
  font-weight: 500;
}

/* 卡片容器 */
.leave-details-card {
  background: #fff;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

/* 基本信息 & 历史标题 */
.request-info h3,
.approval-history h3 {
  font-size: 1.3rem;
  color: #495057;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #eee;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  font-size: 1rem;
  line-height: 1.6;
}
.info-grid > div {
  padding: 0.5rem 0;
}
.info-grid strong {
  color: #343a40;
  margin-right: 0.5em;
}
.full-width {
  grid-column: 1 / -1;
}
.info-grid pre {
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8f9fa;
  padding: 0.5rem;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  margin: 0;
}

/* 审批历史列表 */
.approval-history ul {
  list-style: none;
  padding: 0;
}
.history-item {
  background: #fdfdfd;
  border: 1px solid #f1f1f1;
  border-radius: 4px;
  padding: 1rem;
  margin-bottom: 0.5rem;
}
.history-actor {
  font-size: 0.9rem;
  color: #6c757d;
}
.history-actor strong {
  color: #343a40;
}
.history-decision-approved {
  color: #28a745;
  font-weight: bold;
  margin-top: 0.5rem;
}
.history-decision-rejected {
  color: #dc3545;
  font-weight: bold;
  margin-top: 0.5rem;
}
.history-decision-cancelled { /* 为取消状态添加样式 */
  color: #6c757d;
  font-weight: bold;
  margin-top: 0.5rem;
}
.history-comments {
  margin-top: 0.5rem;
  padding-left: 1rem;
  border-left: 3px solid #007bff;
  color: #495057;
  white-space: pre-wrap;
}

/* 操作按钮 */
.action-buttons {
  margin-top: 2rem;
  text-align: right;
  border-top: 1px solid #e9ecef;
  padding-top: 1rem;
}
.action-buttons .btn {
  margin-left: 1rem;
  padding: 0.6rem 1.2rem;
  font-size: 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background .2s;
}
.btn-approve {
  background: #28a745;
  color: #fff;
}
.btn-approve:hover:not(:disabled) {
  background: #218838;
}
.btn-reject {
  background: #dc3545;
  color: #fff;
}
.btn-reject:hover:not(:disabled) {
  background: #c82333;
}
.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 通用提示 */
.loading-spinner,
.no-data {
  text-align: center;
  padding: 2rem;
  color: #6c757d;
}
.alert {
  padding: 1rem;
  border-radius: 4px;
  margin-bottom: 1rem;
}
.alert-danger {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1050;
}
.modal-content {
  background: #fff;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 5px 20px rgba(0,0,0,0.1);
  width: 100%;
  max-width: 400px;
}
.modal-content h3 {
  margin: 0 0 1rem;
}
.form-group label {
  display: block;
  margin-bottom: 0.5rem;
}
.form-group textarea {
  width: 100%;
  padding: 0.6rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  resize: vertical;
}
.modal-actions {
  text-align: right;
  margin-top: 1rem;
}
.modal-actions .btn {
  margin-left: 0.5rem;
}
.btn-primary {
  background: #007bff;
  color: #fff;
}
.btn-primary:hover:not(:disabled) {
  background: #0056b3;
}
.btn-secondary {
  background: #6c757d;
  color: #fff;
}
.btn-secondary:hover:not(:disabled) {
  background: #545b62;
}
.spinner { /* 添加了 spinner 样式 */
  display: inline-block;
  width: 1em;
  height: 1em;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff; /* 或者用您的主题色 */
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  vertical-align: middle;
  margin-right: 0.5em;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>