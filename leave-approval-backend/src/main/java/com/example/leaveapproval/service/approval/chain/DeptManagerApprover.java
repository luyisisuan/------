package com.example.leaveapproval.service.approval.chain;

import com.example.leaveapproval.model.LeaveRequest;
import com.example.leaveapproval.model.Role;
import com.example.leaveapproval.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.List; // 确保导入 List

/**
 * 部门经理审批者实现。
 * 通常负责审批比团队领导权限更长一些的请假申请。
 */
@Component("deptManagerApprover") // Spring Bean 名称
public class DeptManagerApprover extends AbstractApprover {

    private static final Logger logger = LoggerFactory.getLogger(DeptManagerApprover.class);
    private static final int MAX_DAYS_CAN_APPROVE_BY_ROLE = 7; // 部门经理角色最多批准7天

    @Override
    protected boolean canThisRoleApprove(LeaveRequest leaveRequest, User actionTakingApprover) {
        // 检查当前操作用户是否确实是部门经理角色
        if (!actionTakingApprover.getRoles().contains(Role.ROLE_DEPT_MANAGER)) {
            logger.warn("用户 {} (ID: {}) 尝试以部门经理身份操作，但其不具备 ROLE_DEPT_MANAGER 角色。",
                    actionTakingApprover.getUsername(), actionTakingApprover.getId());
            return false; // 或者抛出权限异常，但通常前置校验已做
        }
        long leaveDays = calculateLeaveDays(leaveRequest); // 使用父类提供的计算天数方法
        boolean canApprove = leaveDays <= MAX_DAYS_CAN_APPROVE_BY_ROLE;
        if (!canApprove) {
            logger.info("部门经理 {} 无法直接最终批准请假ID {}，申请天数 {} 超过其权限 {} 天。",
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
        // 判断当前指定的审批人是否应该由部门经理这个角色来处理
        User currentApprover = leaveRequest.getCurrentApprover();
        return currentApprover != null &&
                currentApprover.getRoles().contains(Role.ROLE_DEPT_MANAGER);
    }

    @Override
    public User determineNextApproverUser(LeaveRequest leaveRequest, User applicant) {
        long leaveDays = calculateLeaveDays(leaveRequest);

        // 如果部门经理已经有权批准（即天数在其权限内），则不需要下一级
        if (leaveDays <= MAX_DAYS_CAN_APPROVE_BY_ROLE) {
            logger.debug("请假申请 ID: {} 天数 {} 在部门经理权限内，流程在此节点结束。",
                    leaveRequest.getId(), leaveDays);
            return null; // 表示部门经理是此场景下的最终审批人 (如果他是审批链的倒数第二环)
        } else {
            // 天数超过部门经理权限，需要流转到下一级 (HR)
            logger.info("请假申请 ID: {} 天数 {} 超过部门经理权限，查找下一级审批人 (HR)。", leaveRequest.getId(), leaveDays);

            // 使用父类提供的 findUsersByRole 方法查找HR用户
            List<User> hrUsers = findUsersByRole(Role.ROLE_HR);
            if (!hrUsers.isEmpty()) {
                User hrUser = hrUsers.get(0); // 取第一个找到的HR用户
                logger.info("找到HR用户 {} 作为请假申请 ID: {} 的下一审批人。", hrUser.getUsername(), leaveRequest.getId());
                return hrUser;
            }

            logger.error("严重错误：请假申请 ID: {} 超过部门经理权限，但无法确定下一级审批人 (系统中未找到HR用户)。", leaveRequest.getId());
            // 此处应抛出异常或有明确的错误处理流程，表示审批链断裂或配置问题
            throw new IllegalStateException("审批流程配置错误：无法为请假申请 " + leaveRequest.getId() + " 确定下一级审批人 (HR)。");
        }
    }
    // 移除了 findFirstHRUser()，因为它现在应该由 AbstractApprover.findUsersByRole() 替代
}