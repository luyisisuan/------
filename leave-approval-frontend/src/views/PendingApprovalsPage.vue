<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import leaveService from '../services/leaveService';
import { useAuthStore } from '../stores/auth';
import { ElMessage, ElMessageBox } from 'element-plus';
import { View, Check, Close, Refresh } from '@element-plus/icons-vue';

const router = useRouter();
const authStore = useAuthStore();

const pendingRequests = ref([]);
const paginationData = ref({
  content: [], totalPages: 0, totalElements: 0,
  number: 0, size: 10,
});
const isLoadingPage = ref(false);
const pageError = ref(null);

const formatLeaveType = (type) => {
  const types = {
    ANNUAL_LEAVE: '年假', SICK_LEAVE: '病假', PERSONAL_LEAVE: '事假', MATERNITY_LEAVE: '产假',
    PATERNITY_LEAVE: '陪产假', BEREAVEMENT_LEAVE: '丧假', UNPAID_LEAVE: '无薪假', OTHER: '其他',
  };
  return types[type] || type;
};

// 获取待我审批的请假申请列表
const fetchPendingApprovals = async (page = 0, size = 10) => {
  isLoadingPage.value = true;
  pageError.value = null;
  try {
    const data = await leaveService.getPendingMyApprovalRequests({
      page, size, sort: 'createdAt,asc' // 通常按提交时间升序，先提交的先审
    });
    pendingRequests.value = data.content || [];
    paginationData.value = {
      content: data.content || [],
      totalPages: data.totalPages || 0,
      totalElements: data.totalElements || 0,
      number: data.number || 0,
      size: data.size || size,
    };
  } catch (err) {
    console.error('PendingApprovalsPage: 获取待审批列表失败:', err);
    pageError.value = err.response?.data?.message || err.message || '获取待审批列表失败。';
    pendingRequests.value = [];
  } finally {
    isLoadingPage.value = false;
  }
};

onMounted(() => {
  // 可以在这里添加一个检查，如果用户没有审批权限，则提示或重定向
  if (authStore.canApprove) { // 假设 authStore 有 canApprove 计算属性
    fetchPendingApprovals(0, paginationData.value.size);
  } else if (authStore.isAuthenticated) { // 已登录但无权限
    pageError.value = "您没有权限执行审批操作。";
  } else { // 未登录
    pageError.value = "请先登录以查看待审批任务。";
    // 可以考虑重定向到登录页 router.push('/login');
  }
});

// 审批操作模态框相关
const selectedRequestForAction = ref(null);
const approvalAction = ref(''); // 'APPROVED' 或 'REJECTED'
const approvalComments = ref('');
const isActionDialogVisible = ref(false); // 使用 Dialog
const isSubmittingAction = ref(false);
const actionErrorInDialog = ref(null); // 模态框内的错误

const openApprovalDialog = (request, actionType) => {
  selectedRequestForAction.value = request;
  approvalAction.value = actionType;
  approvalComments.value = '';
  actionErrorInDialog.value = null;
  isActionDialogVisible.value = true;
};

const handleDialogClose = () => {
  // 清理模态框状态，如果需要的话
  selectedRequestForAction.value = null;
  approvalComments.value = '';
  actionErrorInDialog.value = null;
};

const submitApprovalAction = async () => {
  if (!selectedRequestForAction.value || !approvalAction.value) return;
  isSubmittingAction.value = true;
  actionErrorInDialog.value = null;
  try {
    await leaveService.processApproval(selectedRequestForAction.value.id, {
      decision: approvalAction.value,
      comments: approvalComments.value.trim(),
    });
    ElMessage.success(`请假申请 #${selectedRequestForAction.value.id} 已成功${approvalAction.value === 'APPROVED' ? '批准' : '驳回'}！`);
    isActionDialogVisible.value = false; // 关闭对话框
    fetchPendingApprovals(paginationData.value.number, paginationData.value.size); // 刷新列表
  } catch (err) {
    console.error('PendingApprovalsPage: 审批操作失败:', err);
    actionErrorInDialog.value = err.response?.data?.message || err.message || '审批操作失败。';
    // ElMessage.error(actionErrorInDialog.value); // 也可以在对话框外提示
  } finally {
    isSubmittingAction.value = false;
  }
};

// 分页
const handlePageChange = (newPage) => {
  fetchPendingApprovals(newPage - 1, paginationData.value.size);
};
const handleSizeChange = (newSize) => {
  fetchPendingApprovals(0, newSize);
};

const viewDetails = (requestId) => {
  router.push({ name: 'LeaveDetails', params: { id: requestId } });
};

</script>

<template>
  <div class="pending-approvals-page-el">
    <el-card shadow="never" class="page-header-card-el">
      <div class="page-header-content-el">
        <h1>待我审批</h1>
        <el-button
          type="primary"
          :icon="Refresh"
          @click="fetchPendingApprovals(paginationData.number, paginationData.size)"
          :loading="isLoadingPage && pendingRequests.length === 0"
          round
        >
          刷新列表
        </el-button>
      </div>
      <p v-if="authStore.canApprove" class="header-subtitle">
        以下是分配给您等待处理的请假申请。
      </p>
    </el-card>

    <div v-if="pageError" style="margin-top: 20px;">
      <el-alert :title="pageError" type="error" show-icon :closable="false" />
    </div>

    <el-table
      :data="pendingRequests"
      v-loading="isLoadingPage && pendingRequests.length === 0"
      style="width: 100%; margin-top: 20px;"
      stripe
      border
      class="approvals-table-el"
    >
      <el-table-column prop="id" label="ID" width="80" align="center" sortable />
      <el-table-column label="申请人" min-width="120" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.applicant?.fullName || scope.row.applicant?.username || 'N/A' }}
        </template>
      </el-table-column>
      <el-table-column label="请假类型" width="120" align="center">
        <template #default="scope">
          <el-tag size="small" effect="light" round>
            {{ formatLeaveType(scope.row.leaveType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="开始日期" width="130" align="center" sortable />
      <el-table-column prop="endDate" label="结束日期" width="130" align="center" sortable />
      <el-table-column prop="reason" label="请假理由" min-width="200" show-overflow-tooltip />
      <el-table-column label="提交时间" width="180" align="center" sortable>
         <template #default="scope">
            {{ scope.row.createdAt ? new Date(scope.row.createdAt).toLocaleString('zh-CN', { hour12: false }) : '-' }}
          </template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="scope">
          <el-button-group>
            <el-button
              type="success"
              size="small"
              :icon="Check"
              @click="openApprovalDialog(scope.row, 'APPROVED')"
              :disabled="isSubmittingAction"
              plain
            >
              批准
            </el-button>
            <el-button
              type="danger"
              size="small"
              :icon="Close"
              @click="openApprovalDialog(scope.row, 'REJECTED')"
              :disabled="isSubmittingAction"
              plain
            >
              驳回
            </el-button>
            <el-button
              type="primary"
              link
              size="small"
              :icon="View"
              @click="viewDetails(scope.row.id)"
              style="margin-left: 8px;"
            >
              详情
            </el-button>
          </el-button-group>
        </template>
      </el-table-column>
      <template #empty>
         <el-empty description="目前没有需要您审批的请假申请。">
            <span v-if="!authStore.canApprove && authStore.isAuthenticated">您可能没有审批权限。</span>
          </el-empty>
      </template>
    </el-table>

    <div class="pagination-container-el" v-if="paginationData.totalPages > 0">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="paginationData.totalElements"
        :page-sizes="[10, 20, 50, 100]"
        :current-page="paginationData.number + 1"
        :page-size="paginationData.size"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 审批操作对话框 -->
    <el-dialog
      v-model="isActionDialogVisible"
      :title="`审批操作 - 请假ID: ${selectedRequestForAction?.id}`"
      width="500px"
      draggable
      destroy-on-close
      @closed="handleDialogClose"
      append-to-body
      custom-class="approval-dialog-el"
    >
      <div v-if="selectedRequestForAction" class="dialog-request-info">
        <p><strong>申请人:</strong> {{ selectedRequestForAction.applicant?.fullName }}</p>
        <p><strong>类型:</strong> {{ formatLeaveType(selectedRequestForAction.leaveType) }}</p>
        <p><strong>时长:</strong> {{ selectedRequestForAction.startDate }} 至 {{ selectedRequestForAction.endDate }}</p>
      </div>
      <el-divider />
      <el-form @submit.prevent="submitApprovalAction" label-position="top">
        <el-form-item :label="`您的审批决定: ${approvalAction === 'APPROVED' ? '批准' : '驳回'}`">
           <el-tag :type="approvalAction === 'APPROVED' ? 'success' : 'danger'" effect="dark" size="large" round>
            {{ approvalAction === 'APPROVED' ? '同意批准' : '决定驳回' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审批意见 (可选)">
          <el-input
            type="textarea"
            v-model="approvalComments"
            :rows="3"
            placeholder="请输入您的审批意见..."
            clearable
          />
        </el-form-item>
        <el-alert v-if="actionErrorInDialog" :title="actionErrorInDialog" type="error" show-icon :closable="false" style="margin-bottom: 15px;" />
      </el-form>
      <template #footer>
        <el-button @click="isActionDialogVisible = false" :disabled="isSubmittingAction">取 消</el-button>
        <el-button
          type="primary"
          @click="submitApprovalAction"
          :loading="isSubmittingAction"
          :icon="approvalAction === 'APPROVED' ? Check : Close"
        >
          确认{{ approvalAction === 'APPROVED' ? '批准' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
.pending-approvals-page-el {
  padding: 20px;
  background-color: var(--el-bg-color-page);
}

.page-header-card-el {
  margin-bottom: 25px;
  border-radius: 8px;
}
.page-header-content-el {
  padding: 15px 25px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.page-header-card-el h1 {
  margin: 0;
  font-size: 1.8rem;
  color: var(--el-text-color-primary);
  font-weight: 500;
}
.header-subtitle {
  padding: 0 25px 15px;
  margin:0;
  font-size: 0.9rem;
  color: var(--el-text-color-secondary);
}

.approvals-table-el {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: var(--el-box-shadow-light);
}
.approvals-table-el .el-table__header-wrapper th {
  background-color: var(--el-fill-color-light) !important;
  color: var(--el-text-color-regular);
  font-weight: 500;
}
.approvals-table-el .el-table__body td, .approvals-table-el .el-table__header-wrapper th {
  padding: 10px 12px;
}
.el-table__fixed-right .el-button-group .el-button {
  margin-left: 0; /* Element Plus ButtonGroup 会处理间距 */
}
.el-table__fixed-right .el-button-group .el-button:first-child {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}
.el-table__fixed-right .el-button-group .el-button:last-child:not(:first-child) {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}
.el-table__fixed-right .el-button-group + .el-button { /* 分组和单个按钮间距 */
   margin-left: 8px !important;
}


.pagination-container-el {
  margin-top: 25px;
  display: flex;
  justify-content: flex-end;
}

/* 对话框样式 */
.approval-dialog-el .el-dialog__header {
  padding: 15px 20px;
  margin-right: 0; /* 覆盖默认的 margin-right for el-dialog__headerbtn */
  border-bottom: 1px solid var(--el-border-color-light);
}
.approval-dialog-el .el-dialog__title {
  font-size: 1.15rem;
  font-weight: 500;
}
.approval-dialog-el .el-dialog__body {
  padding: 20px 25px;
}
.dialog-request-info {
  font-size: 0.9rem;
  color: var(--el-text-color-secondary);
  margin-bottom: 15px;
}
.dialog-request-info p {
  margin: 5px 0;
}
.dialog-request-info strong {
  color: var(--el-text-color-primary);
  margin-right: 5px;
}
.approval-dialog-el .el-form-item {
  margin-bottom: 18px;
}
.approval-dialog-el .el-form-item__label {
  padding-bottom: 4px !important;
  color: var(--el-text-color-regular) !important;
  font-weight: normal !important;
}
.approval-dialog-el .el-textarea__inner {
  border-radius: 6px;
}
.approval-dialog-el .el-dialog__footer {
  padding: 15px 25px;
  border-top: 1px solid var(--el-border-color-light);
}

</style>