package com.example.leaveapproval.service.approval.chain;

import com.example.leaveapproval.model.LeaveRequest;
import com.example.leaveapproval.model.Role;
import com.example.leaveapproval.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.List; // 确保导入 List

@Component("teamLeadApprover")
public class TeamLeadApprover extends AbstractApprover {

    private static final Logger logger = LoggerFactory.getLogger(TeamLeadApprover.class);
    private static final int MAX_DAYS_CAN_APPROVE_BY_ROLE = 2;

    @Override
    protected boolean canThisRoleApprove(LeaveRequest leaveRequest, User actionTakingApprover) {
        if (!actionTakingApprover.getRoles().contains(Role.ROLE_TEAM_LEAD)) {
            logger.warn("用户 {} (ID: {}) 尝试以团队领导身份操作，但其不具备 ROLE_TEAM_LEAD 角色。",
                    actionTakingApprover.getUsername(), actionTakingApprover.getId());
            return false;
        }
        long leaveDays = calculateLeaveDays(leaveRequest);
        boolean canApprove = leaveDays <= MAX_DAYS_CAN_APPROVE_BY_ROLE;
        if (!canApprove) {
            logger.info("团队领导 {} 无法直接最终批准请假ID {}，申请天数 {} 超过其权限 {} 天。",
                    actionTakingApprover.getUsername(), leaveRequest.getId(), leaveDays, MAX_DAYS_CAN_APPROVE_BY_ROLE);
        }
        return canApprove;
    }

    @Override
    protected int getMaxLeaveDaysThisRoleCanApprove() {
        return MAX_DAYS_CAN_APPROVE_BY_ROLE;
    }

    @Override
    public boolean isResponsibleRoleFor(LeaveRequest leaveRequest) {
        User currentApprover = leaveRequest.getCurrentApprover();
        return currentApprover != null && currentApprover.getRoles().contains(Role.ROLE_TEAM_LEAD);
    }

    @Override
    public User determineNextApproverUser(LeaveRequest leaveRequest, User applicant) {
        long leaveDays = calculateLeaveDays(leaveRequest);

        if (leaveDays <= MAX_DAYS_CAN_APPROVE_BY_ROLE) {
            logger.debug("请假申请 ID: {} 天数 {} 在团队领导权限内，流程在此节点结束。",
                    leaveRequest.getId(), leaveDays);
            return null; // TeamLead 是此场景的最终审批人
        } else {
            logger.info("请假申请 ID: {} 天数 {} 超过团队领导权限，查找下一级审批人 (部门经理或HR)。", leaveRequest.getId(), leaveDays);

            // 优先查找部门经理
            List<User> deptManagers = findUsersByRole(Role.ROLE_DEPT_MANAGER); // 使用父类的方法
            if (!deptManagers.isEmpty()) {
                User deptManager = deptManagers.get(0);
                logger.info("找到部门经理 {} 作为请假申请 ID: {} 的下一审批人。", deptManager.getUsername(), leaveRequest.getId());
                return deptManager;
            } else {
                logger.warn("未找到部门经理，尝试查找HR作为下一级。");
                // 如果没有部门经理，则查找HR
                List<User> hrUsers = findUsersByRole(Role.ROLE_HR); // 使用父类的方法
                if (!hrUsers.isEmpty()) {
                    User hrUser = hrUsers.get(0);
                    logger.info("找到HR用户 {} 作为请假申请 ID: {} 的下一审批人。", hrUser.getUsername(), leaveRequest.getId());
                    return hrUser;
                }
            }

            logger.error("严重错误：请假申请 ID: {} 超过团队领导权限，且无法确定下一级审批人（部门经理或HR均未找到）。", leaveRequest.getId());
            throw new IllegalStateException("审批流程配置错误：无法为请假申请 " + leaveRequest.getId() + " 确定下一级审批人。");
        }
    }
    // 移除了本类中的 findFirstHRUser()
}