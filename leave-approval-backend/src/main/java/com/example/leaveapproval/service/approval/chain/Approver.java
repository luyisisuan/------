package com.example.leaveapproval.service.approval.chain;

import com.example.leaveapproval.model.ApprovalHistory; // 确保导入 Decision 枚举
import com.example.leaveapproval.model.LeaveRequest;
import com.example.leaveapproval.model.User;

/**
 * 审批者接口 (职责链中的处理者)。
 * 定义了审批操作、设置下一个审批者以及判断职责的能力。
 */
public interface Approver {

    /**
     * 设置职责链中的下一个审批者。
     *
     * @param nextApprover 下一个审批者节点。
     */
    void setNext(Approver nextApprover);

    /**
     * 处理审批动作（批准/驳回）。
     * 此方法由外部服务（如 LeaveRequestManagementService）在确定当前审批用户和其对应的 Approver 实例后调用。
     *
     * @param leaveRequest 需要审批的请假申请。
     * @param approverUser 当前执行审批操作的用户实体 (用于记录审批历史，并进行权限校验)。
     * @param decision     审批决定 (来自 ApprovalHistory.Decision)。
     * @param comments     审批意见。
     * @throws IllegalStateException    如果当前用户无权审批此申请。
     * @throws IllegalArgumentException 如果审批权限不足以处理该请求（例如天数超限）。
     */
    void handleApprovalAction(LeaveRequest leaveRequest, User approverUser, ApprovalHistory.Decision decision, String comments);

    /**
     * 判断此 Approver 实例是否是给定请假申请当前指定的审批人（或角色）。
     * 主要用于外部服务在调用 handleApprovalAction 前，或在构建审批链时，确认正确的 Approver 实例。
     *
     * @param leaveRequest 待检查的请假申请。
     * @return 如果此审批者类型与 leaveRequest.currentApprover 的角色匹配，则返回 true。
     */
    boolean isResponsibleRoleFor(LeaveRequest leaveRequest);


    /**
     * 当此审批者批准了申请后，确定下一个可能的审批用户。
     * 如果这是最后一级审批，或者根据规则不再需要下一级，则返回 null。
     *
     * @param leaveRequest 当前已被此审批者批准的请假申请。
     * @param applicant    请假申请的提交人。
     * @return 下一个审批用户实体 (User)，或者 null 如果没有下一级或流程结束。
     */
    User determineNextApproverUser(LeaveRequest leaveRequest, User applicant);

}