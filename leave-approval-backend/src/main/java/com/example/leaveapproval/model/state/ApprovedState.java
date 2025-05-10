package com.example.leaveapproval.model.state;

import com.example.leaveapproval.model.ApprovalHistory;
import com.example.leaveapproval.model.LeaveRequest;
import com.example.leaveapproval.model.LeaveStatus;
import com.example.leaveapproval.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApprovedState implements LeaveState {
    private static final Logger logger = LoggerFactory.getLogger(ApprovedState.class);
    private static final ApprovedState INSTANCE = new ApprovedState();

    private ApprovedState() {}

    public static ApprovedState getInstance() {
        return INSTANCE;
    }

    @Override
    public void approve(LeaveRequest context, User actionTakingUser, ApprovalHistory.Decision decision, String comments) {
        String errorMsg = "操作不允许：请假申请 ID: " + context.getId() + " 已处于 APPROVED 状态，不能再次批准。";
        logger.warn(errorMsg);
        throw new IllegalStateException(errorMsg);
    }

    @Override
    public void reject(LeaveRequest context, User actionTakingUser, ApprovalHistory.Decision decision, String comments) {
        String errorMsg = "操作不允许：请假申请 ID: " + context.getId() + " 已处于 APPROVED 状态，不能驳回。";
        logger.warn(errorMsg);
        throw new IllegalStateException(errorMsg);
    }

    @Override
    public void cancel(LeaveRequest context, User actionTakingUser) {
        // 业务规则：已批准的申请是否允许取消？如果允许，可能需要特殊流程或权限。
        // 简化：假设已批准的不能直接取消，或需要管理员操作。
        String errorMsg = "操作不允许：请假申请 ID: " + context.getId() + " 已处于 APPROVED 状态，取消操作可能需要特殊权限或流程。";
        logger.warn(errorMsg);
        throw new IllegalStateException(errorMsg);
        // 如果允许取消，则：
        // context.setStatusEnumAndUpdateState(LeaveStatus.CANCELLED);
        // context.setCurrentApprover(null);
        // logger.info("已批准的请假申请 ID: {} 已被取消。", context.getId());
    }

    @Override
    public LeaveStatus getStatusEnum() {
        return LeaveStatus.APPROVED;
    }
}