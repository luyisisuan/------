package com.example.leaveapproval.model;

/**
 * 定义了请假申请的各种状态。
 */
public enum LeaveStatus {
    PENDING_APPROVAL, // 待审批 (可以细化为 PENDING_TEAM_LEAD, PENDING_DEPT_MANAGER, PENDING_HR 等，但初期用一个通用状态，通过 current_approver_id 区分)
    APPROVED,         // 已批准
    REJECTED,         // 已驳回
    CANCELLED,        // 已取消 (由申请人操作)
    PROCESSING        // 处理中 (例如，HR已收到批准单，正在处理后续事宜)
}