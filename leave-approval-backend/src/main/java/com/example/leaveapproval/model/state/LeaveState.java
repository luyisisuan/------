package com.example.leaveapproval.model.state;

import com.example.leaveapproval.model.ApprovalHistory; // 用于 Decision
import com.example.leaveapproval.model.LeaveRequest;    // 上下文对象
import com.example.leaveapproval.model.User;          // 操作用户

/**
 * 请假申请状态接口 (状态模式中的 State)。
 * 定义了在不同状态下，请假申请可以执行的操作。
 */
public interface LeaveState {

    /**
     * 尝试批准请假申请。
     *
     * @param context            当前的请假申请对象 (上下文)。
     * @param actionTakingUser   执行批准操作的用户。
     * @param comments           审批意见。
     * @param decision           审批决定 (应为 APPROVED)。
     * @throws IllegalStateException 如果当前状态不允许执行批准操作。
     */
    void approve(LeaveRequest context, User actionTakingUser, ApprovalHistory.Decision decision, String comments);

    /**
     * 尝试驳回请假申请。
     *
     * @param context            当前的请假申请对象。
     * @param actionTakingUser   执行驳回操作的用户。
     * @param comments           审批意见。
     * @param decision           审批决定 (应为 REJECTED)。
     * @throws IllegalStateException 如果当前状态不允许执行驳回操作。
     */
    void reject(LeaveRequest context, User actionTakingUser, ApprovalHistory.Decision decision, String comments);

    /**
     * 尝试取消请假申请 (通常由申请人发起)。
     *
     * @param context            当前的请假申请对象。
     * @param actionTakingUser   执行取消操作的用户 (必须是申请人)。
     * @throws IllegalStateException 如果当前状态不允许执行取消操作，或操作人不是申请人。
     */
    void cancel(LeaveRequest context, User actionTakingUser);

    /**
     * (可选) 当进入此状态时执行的动作。
     * 例如，发送通知。
     * @param context 当前的请假申请对象。
     */
    // void onEnterState(LeaveRequest context);

    /**
     * 获取当前状态对应的枚举值。
     * 便于持久化和查询。
     * @return LeaveStatus 枚举。
     */
    com.example.leaveapproval.model.LeaveStatus getStatusEnum();
}