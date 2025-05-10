package com.example.leaveapproval.model.state;

import com.example.leaveapproval.model.ApprovalHistory;
import com.example.leaveapproval.model.LeaveRequest;
import com.example.leaveapproval.model.LeaveStatus;
import com.example.leaveapproval.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CancelledState implements LeaveState {
    private static final Logger logger = LoggerFactory.getLogger(CancelledState.class);
    private static final CancelledState INSTANCE = new CancelledState();

    private CancelledState() {}

    public static CancelledState getInstance() {
        return INSTANCE;
    }

    @Override
    public void approve(LeaveRequest context, User actionTakingUser, ApprovalHistory.Decision decision, String comments) {
        String errorMsg = "操作不允许：请假申请 ID: " + context.getId() + " 已处于 CANCELLED 状态，不能批准。";
        logger.warn(errorMsg);
        throw new IllegalStateException(errorMsg);
    }

    @Override
    public void reject(LeaveRequest context, User actionTakingUser, ApprovalHistory.Decision decision, String comments) {
        String errorMsg = "操作不允许：请假申请 ID: " + context.getId() + " 已处于 CANCELLED 状态，不能驳回。";
        logger.warn(errorMsg);
        throw new IllegalStateException(errorMsg);
    }

    @Override
    public void cancel(LeaveRequest context, User actionTakingUser) {
        String errorMsg = "操作不允许：请假申请 ID: " + context.getId() + " 已处于 CANCELLED 状态，不能再次取消。";
        logger.warn(errorMsg);
        throw new IllegalStateException(errorMsg);
    }

    @Override
    public LeaveStatus getStatusEnum() {
        return LeaveStatus.CANCELLED;
    }
}