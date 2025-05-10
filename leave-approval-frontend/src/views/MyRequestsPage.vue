<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router'; // 引入 useRouter
import leaveService from '../services/leaveService';
import { ElMessage, ElMessageBox } from 'element-plus'; // 引入 Element Plus 消息和确认框
import { View, CloseBold, Refresh } from '@element-plus/icons-vue'; // 引入图标

const router = useRouter(); // 获取 router 实例

const myRequests = ref([]);
const paginationData = ref({
  content: [], totalPages: 0, totalElements: 0, number: 0, size: 10,
});
const isLoading = ref(false);
const error = ref(null);

const formatLeaveType = (type) => {
  const types = {
    ANNUAL_LEAVE: '年假', SICK_LEAVE: '病假', PERSONAL_LEAVE: '事假', MATERNITY_LEAVE: '产假',
    PATERNITY_LEAVE: '陪产假', BEREAVEMENT_LEAVE: '丧假', UNPAID_LEAVE: '无薪假', OTHER: '其他',
  };
  return types[type] || type;
};

const getStatusType = (status) => {
  const typeMap = {
    PENDING_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'danger',
    CANCELLED: 'info', PROCESSING: 'primary',
  };
  return typeMap[status] || 'info';
};

const formatLeaveStatus = (status) => {
  const statuses = {
    PENDING_APPROVAL: '待审批', APPROVED: '已批准', REJECTED: '已驳回',
    CANCELLED: '已取消', PROCESSING: '处理中',
  };
  return statuses[status] || status;
};

const fetchMyRequests = async (page = 0, size = 10) => {
  isLoading.value = true;
  error.value = null;
  try {
    const responseData = await leaveService.getMyLeaveRequests({
      page: page, size: size, sort: 'createdAt,desc'
    });
    myRequests.value = responseData.content || [];
    paginationData.value = {
      content: responseData.content || [],
      totalPages: responseData.totalPages || 0,
      totalElements: responseData.totalElements || 0,
      number: responseData.number || 0,
      size: responseData.size || size,
    };
  } catch (err) {
    console.error('MyRequestsPage: 获取我的申请列表失败:', err);
    error.value = err.response?.data?.message || err.message || '获取申请列表失败。';
    myRequests.value = [];
    paginationData.value.content = [];
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchMyRequests(0, paginationData.value.size);
});

const canCancelRequest = (status) => status === 'PENDING_APPROVAL';

const handleCancelRequest = async (requestId) => {
  try {
    await ElMessageBox.confirm(
      `您确定要取消ID为 ${requestId} 的请假申请吗？此操作无法撤销。`,
      '确认取消',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '再想想',
        type: 'warning',
        draggable: true,
      }
    );
    // 用户点击了“确定取消”
    isLoading.value = true;
    await leaveService.cancelLeaveRequest(requestId);
    ElMessage.success('请假申请已成功取消！');
    fetchMyRequests(paginationData.value.number, paginationData.value.size);
  } catch (action) {
    // 用户点击了“再想想”或关闭了对话框 (action === 'cancel')
    // 或者API调用失败 (action会是错误对象)
    if (action !== 'cancel' && action !== 'close') {
      console.error(`MyRequestsPage: 取消请假申请 ID ${requestId} 失败:`, action);
      ElMessage.error(action.response?.data?.message || action.message || '取消申请失败。');
    } else {
      ElMessage.info('取消操作已中止。');
    }
  } finally {
    isLoading.value = false;
  }
};

// Element Plus 分页组件事件处理
const handlePageChange = (newPage) => {
  fetchMyRequests(newPage - 1, paginationData.value.size); // el-pagination页码从1开始
};
const handleSizeChange = (newSize) => {
  fetchMyRequests(0, newSize); // 切换每页数量时，通常回到第一页
};

const viewDetails = (requestId) => {
  router.push({ name: 'LeaveDetails', params: { id: requestId } });
};

</script>

<template>
  <div class="my-requests-page-el">
    <el-card shadow="never" class="page-header-card-el">
      <h1>我的请假申请</h1>
      <el-button type="primary" :icon="Refresh" @click="fetchMyRequests(paginationData.number, paginationData.size)" :loading="isLoading" round>
        刷新列表
      </el-button>
    </el-card>

    <div v-if="error" style="margin-top: 20px;">
      <el-alert :title="error" type="error" show-icon :closable="false" />
    </div>

    <el-table
      :data="myRequests"
      v-loading="isLoading && myRequests.length === 0"
      style="width: 100%; margin-top: 20px;"
      stripe
      border
      empty-text="您还没有提交过任何请假申请。"
      class="requests-table-el"
    >
      <el-table-column prop="id" label="ID" width="80" align="center" sortable />
      <el-table-column label="请假类型" width="120" align="center">
        <template #default="scope">
          <el-tag size="small" effect="light" round :type="getStatusType(scope.row.status) || 'info'">
            {{ formatLeaveType(scope.row.leaveType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="开始日期" width="130" align="center" sortable />
      <el-table-column prop="endDate" label="结束日期" width="130" align="center" sortable />
      <el-table-column prop="reason" label="请假理由" min-width="200" show-overflow-tooltip />
      <el-table-column label="当前状态" width="120" align="center">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)" effect="dark" round size="small">
            {{ formatLeaveStatus(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="180" align="center" sortable>
         <template #default="scope">
            {{ scope.row.createdAt ? new Date(scope.row.createdAt).toLocaleString('zh-CN', { hour12: false }) : '-' }}
          </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="scope">
          <el-button
            type="primary"
            link
            size="small"
            :icon="View"
            @click="viewDetails(scope.row.id)"
          >
            详情
          </el-button>
          <el-button
            v-if="canCancelRequest(scope.row.status)"
            type="danger"
            link
            size="small"
            :icon="CloseBold"
            @click="handleCancelRequest(scope.row.id)"
            :loading="isLoading && scope.row.id === 'CURRENTLY_CANCELLING_ID_PLACEHOLDER'"
          >
            取消申请
          </el-button>
        </template>
      </el-table-column>
       <template #empty>
         <el-empty description="您还没有提交过任何请假申请。">
            <el-button type="primary" @click="router.push('/submit-leave')">立即提交一个</el-button>
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
  </div>
</template>

<style scoped>
.my-requests-page-el {
  padding: 20px;
  background-color: var(--el-bg-color-page); /* Element Plus 页面背景色 */
}

.page-header-card-el {
  margin-bottom: 20px;
  border-radius: 8px;
}
.page-header-card-el .el-card__body { /* 覆盖 element card body的默认padding */
  padding: 15px 25px !important;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.page-header-card-el h1 {
  margin: 0;
  font-size: 1.75rem;
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.requests-table-el {
  border-radius: 8px;
  overflow: hidden; /* 确保圆角对表头生效 */
  box-shadow: var(--el-box-shadow-light);
}
/* 表头样式 */
.requests-table-el .el-table__header-wrapper th {
  background-color: var(--el-fill-color-light) !important; /* Element Plus 浅填充色 */
  color: var(--el-text-color-regular);
  font-weight: 500;
}
/* 单元格内边距 */
.requests-table-el .el-table__body td, .requests-table-el .el-table__header-wrapper th {
  padding: 10px 12px; /* 调整单元格内边距 */
}

/* 状态标签的额外间距 (如果需要) */
.el-tag + .el-tag {
  margin-left: 5px;
}

/* 操作按钮 */
.el-table__fixed-right .el-button + .el-button {
  margin-left: 8px;
}

.pagination-container-el {
  margin-top: 25px;
  display: flex;
  justify-content: flex-end; /* 分页控件右对齐 */
}

/* 空状态时，让按钮更突出 */
.el-table .el-empty .el-button {
  margin-top: 20px;
}

/* 如果有自定义的 loading 覆盖整个表格 */
.el-table--enable-row-transition .el-table__body td.el-table__cell {
  transition: background-color .25s ease; /* 使 hover 背景色过渡更平滑 */
}
</style>