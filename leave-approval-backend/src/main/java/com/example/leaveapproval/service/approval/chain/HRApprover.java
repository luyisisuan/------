package com.example.leaveapproval.service.approval.chain;

import com.example.leaveapproval.model.LeaveRequest;
import com.example.leaveapproval.model.Role;
import com.example.leaveapproval.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component; // 确保导入 @Component

import java.time.temporal.ChronoUnit;

/**
 * HR审批者实现。
 * 通常作为审批链的较高级别或最终审批环节，可以处理天数较长的请假，
 * 或者对特定类型的请假进行审核。
 */
@Component("hrApprover") // Spring Bean 名称
public class HRApprover extends AbstractApprover {

    private static final Logger logger = LoggerFactory.getLogger(HRApprover.class);
    // HR可以批准更长的假期，或者作为所有未被前面环节批准的申请的最终审批者。
    // MAX_DAYS 可以设得很大，或者在 canThisRoleApprove 中直接返回 true。
    private static final int MAX_DAYS_CAN_APPROVE_BY_ROLE = 30; // HR角色最多批准30天 (示例)

    @Override
    protected boolean canThisRoleApprove(LeaveRequest leaveRequest, User actionTakingApprover) {
        // 检查当前操作用户是否确实是HR角色
        if (!actionTakingApprover.getRoles().contains(Role.ROLE_HR)) {
            logger.warn("用户 {} (ID: {}) 尝试以HR身份操作，但其不具备 ROLE_HR 角色。",
                    actionTakingApprover.getUsername(), actionTakingApprover.getId());
            return false;
        }
        long leaveDays = calculateLeaveDays(leaveRequest);
        // 对于HR，可以设置一个较大的天数上限，或者根据公司政策，某些类型的申请总是需要HR批准。
        // 另一种策略是，如果HR是审批链的最后一环，则他们总是有权处理（批准/驳回）到达他们这里的申请。
        // 这里我们还是使用天数作为判断依据。
        boolean canApprove = leaveDays <= MAX_DAYS_CAN_APPROVE_BY_ROLE;
        if (!canApprove) {
            logger.info("HR {} 无法批准请假ID {}，申请天数 {} 超过其权限 {} 天。",
                    actionTakingApprover.getUsername(), leaveRequest.getId(), leaveDays, MAX_DAYS_CAN_APPROVE_BY_ROLE);
            // 如果HR都无法批准，通常意味着申请天数过长，可能需要更高级别或特殊流程
        }
        return canApprove;
        // 或者，如果HR是最终审批者，无论天数如何，他们都有权做出决定：
        // return true;
    }

    @Override
    protected int getMaxLeaveDaysThisRoleCanApprove() {
        return MAX_DAYS_CAN_APPROVE_BY_ROLE;
    }

    @Override
    public boolean isResponsibleRoleFor(LeaveRequest leaveRequest) {
        // 判断当前指定的审批人是否应该由HR这个角色来处理
        return leaveRequest.getCurrentApprover() != null &&
                leaveRequest.getCurrentApprover().getRoles().contains(Role.ROLE_HR);
    }

    @Override
    public User determineNextApproverUser(LeaveRequest leaveRequest, User applicant) {
        // HR 通常是审批链的最后一级（或处理所有超出常规流程的申请）。
        // 因此，在HR批准后，通常没有下一级审批人了。
        logger.debug("请假申请 ID: {} 已到达HR审批环节，HR批准后流程结束。", leaveRequest.getId());
        return null; // 表示HR是最终审批人，没有下一级
    }
}