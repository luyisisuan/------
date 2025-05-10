<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import leaveService from '../services/leaveService';
import { ElMessage } from 'element-plus';
import { Calendar, Document, Refresh, Select as SubmitIcon } from '@element-plus/icons-vue'; // ChatLineSquare 未使用，已移除

const router = useRouter();

const leaveFormRef = ref(null);
const leaveForm = reactive({
  leaveType: '',
  startDate: '',
  endDate: '',
  reason: '',
});

const isLoading = ref(false);
const serverError = ref(null);
const submitSuccess = ref(false);

const leaveFormRules = reactive({
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startDate: [
    { required: true, message: '请选择开始日期', trigger: 'change' },
    { validator: (rule, value, callback) => {
        if (value) {
          const start = new Date(value);
          const today = new Date();
          today.setHours(0,0,0,0);
          if (start < today) {
            callback(new Error('开始日期不能早于今天'));
          }
        }
        if (value && leaveForm.endDate) {
          if (new Date(value) > new Date(leaveForm.endDate)) {
             if (leaveFormRef.value) {
                leaveFormRef.value.validateField('endDate', () => null);
             }
            callback(new Error('开始日期不能晚于结束日期'));
          } else {
             if (leaveFormRef.value && new Date(value) <= new Date(leaveForm.endDate) ) { // 只有当开始日期不晚于结束日期时才清除
                leaveFormRef.value.clearValidate('endDate');
             }
          }
        }
        callback();
      }, trigger: 'change'
    }
  ],
  endDate: [
    { required: true, message: '请选择结束日期', trigger: 'change' },
    { validator: (rule, value, callback) => {
        if (value && leaveForm.startDate) {
          if (new Date(value) < new Date(leaveForm.startDate)) {
            callback(new Error('结束日期不能早于开始日期'));
          }
        }
        if (value && leaveForm.startDate && new Date(value) >= new Date(leaveForm.startDate)) {
            if (leaveFormRef.value) {
                leaveFormRef.value.clearValidate('startDate');
            }
        }
        callback();
      }, trigger: 'change'
    }
  ],
  reason: [
    { required: true, message: '请填写请假理由', trigger: 'blur' },
    { min: 5, message: '理由至少需要5个字符', trigger: 'blur' },
  ],
});

const leaveTypes = ref([
    { value: 'ANNUAL_LEAVE', text: '年假' }, { value: 'SICK_LEAVE', text: '病假' },
    { value: 'PERSONAL_LEAVE', text: '事假' }, { value: 'MATERNITY_LEAVE', text: '产假' },
    { value: 'PATERNITY_LEAVE', text: '陪产假' }, { value: 'BEREAVEMENT_LEAVE', text: '丧假' },
    { value: 'UNPAID_LEAVE', text: '无薪假' }, { value: 'OTHER', text: '其他' },
]);

const handleSubmit = async () => {
  serverError.value = null;
  submitSuccess.value = false;
  if (!leaveFormRef.value) return;

  await leaveFormRef.value.validate(async (valid) => {
    if (valid) {
      isLoading.value = true;
      try {
        const requestData = {
          leaveType: leaveForm.leaveType,
          startDate: leaveForm.startDate,
          endDate: leaveForm.endDate,
          reason: leaveForm.reason.trim(),
        };
        await leaveService.submitLeave(requestData);
        submitSuccess.value = true;
        ElMessage.success('请假申请已成功提交！');
      } catch (error) {
        serverError.value = error.response?.data?.message || error.message || '提交申请失败。';
        ElMessage.error(serverError.value);
      } finally {
        isLoading.value = false;
      }
    } else {
      ElMessage.error('表单校验失败，请检查所有必填项和日期。');
      return false;
    }
  });
};

const resetForm = () => {
  if (leaveFormRef.value) {
    leaveFormRef.value.resetFields();
  }
  leaveForm.reason = ''; // resetFields 可能不会清空非 prop 的 v-model
  serverError.value = null;
  submitSuccess.value = false;
};

const disabledStartDate = (time) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return time.getTime() < today.getTime();
};
const disabledEndDate = (time) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  if (leaveForm.startDate) {
    const startDateTime = new Date(leaveForm.startDate).getTime();
    return time.getTime() < startDateTime || time.getTime() < today.getTime();
  }
  return time.getTime() < today.getTime();
};

</script>

<template>
  <div class="submit-leave-page-el">
    <el-card class="submit-form-card-el" shadow="always">
      <template #header>
        <div class="card-header-el">
          <el-icon :size="28" color="var(--el-color-primary)"><Document /></el-icon>
          <span>发起新的请假申请</span>
        </div>
      </template>

      <el-form
        v-if="!submitSuccess"
        ref="leaveFormRef"
        :model="leaveForm"
        :rules="leaveFormRules"
        label-position="top"
        class="submit-leave-form-el"
        @submit.prevent="handleSubmit"
        hide-required-asterisk
        :disabled="isLoading"
      >
        <el-form-item label="请假类型" prop="leaveType">
          <el-select
            v-model="leaveForm.leaveType"
            placeholder="请选择请假类型"
            clearable size="large" style="width: 100%;">
            <el-option v-for="type in leaveTypes" :key="type.value" :label="type.text" :value="type.value"/>
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="leaveForm.startDate" type="date" placeholder="选择开始日期"
                format="YYYY-MM-DD" value-format="YYYY-MM-DD"
                :disabled-date="disabledStartDate" clearable size="large" style="width: 100%;" :prefix-icon="Calendar"/>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker
                v-model="leaveForm.endDate" type="date" placeholder="选择结束日期"
                format="YYYY-MM-DD" value-format="YYYY-MM-DD"
                :disabled-date="disabledEndDate" clearable size="large" style="width: 100%;" :prefix-icon="Calendar"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="请假理由" prop="reason">
          <el-input
            type="textarea" v-model="leaveForm.reason" :rows="4"
            placeholder="请详细描述您的请假事由..." clearable show-word-limit maxlength="500"/>
        </el-form-item>

        <el-alert v-if="serverError" :title="serverError" type="error" show-icon :closable="false" style="margin-bottom: 20px;" />

        <el-form-item class="submit-button-container">
          <el-button
            type="primary" native-type="submit" :loading="isLoading"
            class="submit-leave-btn-el" size="large" round :icon="SubmitIcon">
            {{ isLoading ? '正在提交...' : '确认提交申请' }}
          </el-button>
          <el-button @click="resetForm" :disabled="isLoading" size="large" round style="margin-left: 10px;">
            清空表单
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="submitSuccess" class="submission-success-section">
           <el-result
              icon="success"
              title="提交成功"
              sub-title="您的请假申请已成功提交，请耐心等待审批。您可以在“我的申请”页面查看进度。"
            >
              <template #extra>
                <el-button type="primary" @click="resetForm" :icon="Refresh">提交新的申请</el-button>
                <el-button @click="router.push('/my-requests')">查看我的申请</el-button>
              </template>
            </el-result>
        </div>

    </el-card>
    <footer class="page-footer-credit-el">
      <p>© {{ new Date().getFullYear() }} 请假审批系统. All rights reserved.</p>
    </footer>
  </div>
</template>

<style scoped>
.submit-leave-page-el {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  background-color: var(--el-bg-color-page);
  padding: 20px 15px;
  box-sizing: border-box;
}

.submit-form-card-el {
  width: 100%;
  max-width: 700px;
  border-radius: 12px;
  border: none;
  animation: fadeInCard 0.6s ease-out forwards;
  opacity: 0;
  margin-top: 2rem;
  margin-bottom: 2rem;
}
@keyframes fadeInCard {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-header-el {
  display: flex;
  align-items: center;
  padding: 10px 0;
  text-align: center;
  justify-content: center;
}
.card-header-el .el-icon {
  color: var(--el-color-primary);
  margin-right: 10px;
}
.card-header-el span {
  font-size: 1.5rem;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.submit-leave-form-el {
  margin-top: 15px;
}
.el-form-item {
  margin-bottom: 22px;
}
.el-form-item__label {
  padding-bottom: 5px !important;
  line-height: 1.4 !important;
  color: var(--el-text-color-regular) !important;
}
.el-select, .el-date-picker, .el-input, .el-textarea {
  width: 100% !important;
}
.el-input--large .el-input__wrapper,
.el-select--large .el-select__wrapper,
.el-date-editor--large.el-input__wrapper {
  border-radius: 8px !important;
  padding-left: 12px;
}
.el-textarea__inner {
  border-radius: 8px !important;
  padding: 10px 12px;
}
.el-input__prefix .el-icon {
  color: var(--el-text-color-placeholder);
}

.el-alert {
  margin-bottom: 22px;
}
.el-result {
  padding: 30px 0;
}
.el-result__extra .el-button {
  margin: 0 8px;
}


.submit-button-container {
  margin-top: 25px;
  text-align: right;
}
.submit-leave-btn-el {
  font-weight: 500;
  letter-spacing: 0.5px;
}
.submit-leave-btn-el:hover:not(.is-disabled) {
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.2);
}


.page-footer-credit-el {
  width: 100%;
  text-align: center;
  padding: 1rem 0;
  font-size: 0.8rem;
  color: var(--el-text-color-placeholder);
  margin-top: auto;
}
</style>