package com.example.leaveapproval.service.approval.chain;

import com.example.leaveapproval.model.*;
import com.example.leaveapproval.repository.ApprovalHistoryRepository;
import com.example.leaveapproval.repository.LeaveRequestRepository;
import com.example.leaveapproval.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.temporal.ChronoUnit;
import java.util.Collections; // 导入 Collections
import java.util.List;      // 导入 List

public abstract class AbstractApprover implements Approver {
    private static final Logger logger = LoggerFactory.getLogger(AbstractApprover.class);

    protected Approver nextApproverInChain;

    @Autowired
    protected LeaveRequestRepository leaveRequestRepository;

    @Autowired
    protected ApprovalHistoryRepository approvalHistoryRepository;

    @Autowired
    protected UserRepository userRepository;

    @Override
    public void setNext(Approver nextApprover) {
        this.nextApproverInChain = nextApprover;
    }

    @Override
    public void handleApprovalAction(LeaveRequest leaveRequest, User actionTakingUser, ApprovalHistory.Decision decision, String comments) {
        logger.info("Approver Node [{}]: 用户 {} (ID: {}) 尝试对请假ID {} 执行 {} 操作。",
                this.getClass().getSimpleName(), actionTakingUser.getUsername(), actionTakingUser.getId(),
                leaveRequest.getId(), decision);

        boolean isAdminAction = actionTakingUser.getRoles().contains(Role.ROLE_ADMIN);

        if (isAdminAction) {
            logger.info("Admin用户 {} (ID: {}) 正在执行审批操作。将直接处理请假申请ID {}。",
                    actionTakingUser.getUsername(), actionTakingUser.getId(), leaveRequest.getId());
            recordApprovalHistory(leaveRequest, actionTakingUser, decision, "Admin操作：" + comments);
            if (decision == ApprovalHistory.Decision.APPROVED) {
                leaveRequest.setCurrentApprover(null);
                leaveRequest.setStatusEnumAndUpdateState(LeaveStatus.APPROVED);
                logger.info("Admin {} 已最终批准请假申请ID {}。", actionTakingUser.getUsername(), leaveRequest.getId());
            } else if (decision == ApprovalHistory.Decision.REJECTED) {
                leaveRequest.setCurrentApprover(null);
                leaveRequest.setStatusEnumAndUpdateState(LeaveStatus.REJECTED);
                logger.info("Admin {} 已驳回请假申请ID {}。", actionTakingUser.getUsername(), leaveRequest.getId());
            } else {
                logger.error("Admin {} 对请假ID {} 执行了未知的审批决定类型: {}",
                        actionTakingUser.getUsername(), leaveRequest.getId(), decision);
                throw new IllegalArgumentException("Admin执行了无效的审批决定：" + decision);
            }
            leaveRequestRepository.save(leaveRequest);
        } else {
            logger.debug("非Admin用户 {} (ID: {}) 执行常规审批流程...",
                    actionTakingUser.getUsername(), actionTakingUser.getId());
            if (leaveRequest.getCurrentApprover() == null || !leaveRequest.getCurrentApprover().getId().equals(actionTakingUser.getId())) {
                String currentApproverUsername = leaveRequest.getCurrentApprover() != null ? leaveRequest.getCurrentApprover().getUsername() : "未指定或已处理";
                String errorMsg = String.format("权限错误：用户 %s (ID: %d) 尝试操作请假申请 %d，但该申请的当前审批人为 %s，或申请已非待审批状态。",
                        actionTakingUser.getUsername(), actionTakingUser.getId(), leaveRequest.getId(), currentApproverUsername);
                logger.warn(errorMsg);
                throw new IllegalStateException("您不是此请假申请的当前指定审批人或申请状态已改变。");
            }
            if (!canThisRoleApprove(leaveRequest, actionTakingUser)) {
                if (decision == ApprovalHistory.Decision.APPROVED) {
                    logger.info("审批角色 {} (用户: {}) 对请假ID {} 的批准超出其直接权限，将流转至下一级。",
                            this.getClass().getSimpleName().replace("Approver", ""),
                            actionTakingUser.getUsername(), leaveRequest.getId());
                    recordApprovalHistory(leaveRequest, actionTakingUser, decision, "通过，转上级审批：" + comments);
                    performApprove(leaveRequest, actionTakingUser);
                } else if (decision == ApprovalHistory.Decision.REJECTED) {
                    logger.info("审批角色 {} (用户: {}) 对请假ID {} 执行驳回操作（即使天数可能超出其批准上限）。",
                            this.getClass().getSimpleName().replace("Approver", ""),
                            actionTakingUser.getUsername(), leaveRequest.getId());
                    recordApprovalHistory(leaveRequest, actionTakingUser, decision, comments);
                    performReject(leaveRequest, actionTakingUser);
                } else {
                    logger.error("在canThisRoleApprove为false时，收到了未知的审批决定: {}，请假ID: {}", decision, leaveRequest.getId());
                    throw new IllegalArgumentException("无效的审批决定：" + decision + "，当角色无权批准时。");
                }
            } else {
                recordApprovalHistory(leaveRequest, actionTakingUser, decision, comments);
                if (decision == ApprovalHistory.Decision.APPROVED) {
                    performApprove(leaveRequest, actionTakingUser);
                } else if (decision == ApprovalHistory.Decision.REJECTED) {
                    performReject(leaveRequest, actionTakingUser);
                } else {
                    logger.error("收到了未知的审批决定类型: {}，请假ID: {}", decision, leaveRequest.getId());
                    throw new IllegalArgumentException("无效的审批决定：" + decision);
                }
            }
            leaveRequestRepository.save(leaveRequest);
        }
        logger.info("请假申请 ID: {} 处理完毕。新状态: {}, 新当前审批人: {}",
                leaveRequest.getId(),
                leaveRequest.getStatusEnum(),
                leaveRequest.getCurrentApprover() != null ? leaveRequest.getCurrentApprover().getUsername() : "无 (流程结束)");
    }

    protected void recordApprovalHistory(LeaveRequest leaveRequest, User approverUser, ApprovalHistory.Decision decision, String comments) {
        ApprovalHistory history = new ApprovalHistory();
        history.setLeaveRequest(leaveRequest);
        history.setApprover(approverUser);
        history.setDecision(decision);
        history.setComments(comments);
        approvalHistoryRepository.save(history);
        logger.info("审批历史已记录：请假ID {}，审批人 {} (ID: {}), 决定 {}，审批节点角色 {}",
                leaveRequest.getId(), approverUser.getUsername(), approverUser.getId(), decision, this.getClass().getSimpleName());
    }

    protected void performApprove(LeaveRequest leaveRequest, User currentActionTakingApprover) {
        User nextActualApproverUser = determineNextApproverUser(leaveRequest, leaveRequest.getApplicant());
        if (nextActualApproverUser != null) {
            leaveRequest.setCurrentApprover(nextActualApproverUser);
            leaveRequest.setStatusEnumAndUpdateState(LeaveStatus.PENDING_APPROVAL);
            logger.info("请假申请 {} (ID: {}) 已被 {} 代表节点 {} 批准，并成功流转至下一审批人：{}",
                    leaveRequest.getLeaveType(), leaveRequest.getId(), currentActionTakingApprover.getUsername(),
                    this.getClass().getSimpleName(), nextActualApproverUser.getUsername());
        } else {
            leaveRequest.setCurrentApprover(null);
            leaveRequest.setStatusEnumAndUpdateState(LeaveStatus.APPROVED);
            logger.info("请假申请 {} (ID: {}) 已被 {} 代表节点 {} 最终批准。",
                    leaveRequest.getLeaveType(), leaveRequest.getId(), currentActionTakingApprover.getUsername(),
                    this.getClass().getSimpleName());
        }
    }

    protected void performReject(LeaveRequest leaveRequest, User currentActionTakingApprover) {
        leaveRequest.setCurrentApprover(null);
        leaveRequest.setStatusEnumAndUpdateState(LeaveStatus.REJECTED);
        logger.info("请假申请 {} (ID: {}) 已被 {} 代表节点 {} 驳回。",
                leaveRequest.getLeaveType(), leaveRequest.getId(), currentActionTakingApprover.getUsername(),
                this.getClass().getSimpleName());
    }

    protected long calculateLeaveDays(LeaveRequest leaveRequest) {
        if (leaveRequest.getStartDate() == null || leaveRequest.getEndDate() == null || leaveRequest.getEndDate().isBefore(leaveRequest.getStartDate())) {
            logger.warn("计算请假天数时发现无效日期，请假ID: {}", leaveRequest.getId());
            return 0;
        }
        return ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;
    }

    /**
     * 辅助方法：根据角色查找用户列表。
     * 使用注入的 userRepository。
     * @param role 要查找的角色。
     * @return 拥有该角色的用户列表。
     */
    protected List<User> findUsersByRole(Role role) {
        if (this.userRepository == null) {
            logger.error("UserRepository 未在 AbstractApprover 中正确注入，无法按角色查找用户。");
            return Collections.emptyList(); // 返回空列表，避免 NullPointerException
        }
        return this.userRepository.findByRolesContaining(role);
    }

    protected abstract boolean canThisRoleApprove(LeaveRequest leaveRequest, User actionTakingApprover);
    protected abstract int getMaxLeaveDaysThisRoleCanApprove();
    // isResponsibleRoleFor 和 determineNextApproverUser 由 Approver 接口定义，子类必须实现
}