package com.example.leaveapproval.model.state;

import com.example.leaveapproval.model.ApprovalHistory;
import com.example.leaveapproval.model.LeaveRequest;
import com.example.leaveapproval.model.LeaveStatus;
import com.example.leaveapproval.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RejectedState implements LeaveState {
    private static final Logger logger = LoggerFactory.getLogger(RejectedState.class);
    private static final RejectedState INSTANCE = new RejectedState();

    private RejectedState() {}

    public static RejectedState getInstance() {
        return INSTANCE;
    }

    @Override
    public void approve(LeaveRequest context, User actionTakingUser, ApprovalHistory.Decision decision, String comments) {
        String errorMsg = "操作不允许：请假申请 ID: " + context.getId() + " 已处于 REJECTED 状态，不能批准。";
        logger.warn(errorMsg);
        throw new IllegalStateException(errorMsg);
    }

    @Override
    public void reject(LeaveRequest context, User actionTakingUser, ApprovalHistory.Decision decision, String comments) {
        String errorMsg = "操作不允许：请假申请 ID: " + context.getId() + " 已处于 REJECTED 状态，不能再次驳回。";
        logger.warn(errorMsg);
        throw new IllegalStateException(errorMsg);
    }

    @Override
    public void cancel(LeaveRequest context, User actionTakingUser) {
        // 业务规则：已驳回的申请通常不允许取消。
        String errorMsg = "操作不允许：请假申请 ID: " + context.getId() + " 已处于 REJECTED 状态，不能取消。";
        logger.warn(errorMsg);
        throw new IllegalStateException(errorMsg);
    }

    @Override
    public LeaveStatus getStatusEnum() {
        return LeaveStatus.REJECTED;
    }
}