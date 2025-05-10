package com.example.leaveapproval.service.approval.chain;

import com.example.leaveapproval.model.LeaveRequest;
import com.example.leaveapproval.model.User;
import com.example.leaveapproval.model.Role; // 确保 Role 枚举已更新并包含 getHighestRole 和层级
import com.example.leaveapproval.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set; // 导入 Set

/**
 * 构建审批链并确定初始审批人的组件。
 */
@Component
public class ApprovalChainBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalChainBuilder.class);

    private final Approver teamLeadApprover;
    private final Approver deptManagerApprover;
    private final Approver hrApprover;
    private final UserRepository userRepository;

    @Autowired
    public ApprovalChainBuilder(
            @Qualifier("teamLeadApprover") Approver teamLeadApprover,
            @Qualifier("deptManagerApprover") Approver deptManagerApprover,
            @Qualifier("hrApprover") Approver hrApprover,
            UserRepository userRepository) {
        this.teamLeadApprover = teamLeadApprover;
        this.deptManagerApprover = deptManagerApprover;
        this.hrApprover = hrApprover;
        this.userRepository = userRepository;
    }

    /**
     * 构建标准的审批链结构: TeamLead -> DeptManager -> HR。
     * @return 构建好的审批链的头部。
     */
    public Approver buildStandardChain() {
        logger.debug("构建标准审批链结构: TeamLead -> DeptManager -> HR");
        teamLeadApprover.setNext(deptManagerApprover);
        deptManagerApprover.setNext(hrApprover);
        hrApprover.setNext(null);
        return teamLeadApprover;
    }

    /**
     * 根据请假申请人和申请信息确定初始审批用户。
     * 审批逻辑：
     * 1. Admin申请 -> 另一个Admin (如果存在)
     * 2. HR申请 -> Admin
     * 3. DeptManager申请 -> HR (备选Admin)
     * 4. TeamLead申请 -> 直属DeptManager (如果存在且是) -> 系统DeptManager -> 系统HR -> 系统Admin
     * 5. Employee申请 -> 直属TeamLead/DeptManager/HR (按顺序) -> 系统TeamLead -> 系统HR -> 系统Admin
     *
     * @param leaveRequest 请假申请 (可为null，主要用于日志)
     * @param applicant    请假申请的提交人
     * @return 第一个负责审批的 {@link User} 实体；如果无法确定，则抛出异常。
     */
    public User getInitialApproverUser(LeaveRequest leaveRequest, User applicant) {
        String leaveRequestIdForLog = (leaveRequest != null && leaveRequest.getId() != null) ? leaveRequest.getId().toString() : "未知ID";
        String leaveRequestTypeForLog = (leaveRequest != null && leaveRequest.getLeaveType() != null) ? leaveRequest.getLeaveType().toString() : "未知类型";

        logger.info("为申请人 {} (ID: {}) 的请假申请 (ID: {}, 类型: {}) 确定初始审批人...",
                applicant.getUsername(), applicant.getId(),
                leaveRequestIdForLog, leaveRequestTypeForLog);

        User initialApprover = null;
        Set<Role> applicantRoles = applicant.getRoles(); // User.getRoles() 返回 Set<Role>
        Role highestApplicantRole = Role.getHighestRole(applicantRoles); // 使用您更新后的Role枚举中的方法

        if (highestApplicantRole == null) {
            logger.error("申请人 {} (ID: {}) 没有任何角色，无法确定审批流程。", applicant.getUsername(), applicant.getId());
            throw new IllegalStateException("申请人角色未定义，无法处理请假申请。");
        }

        // 1. Admin 提交的申请
        if (highestApplicantRole == Role.ROLE_ADMIN) {
            List<User> admins = userRepository.findByRolesContaining(Role.ROLE_ADMIN);
            if (admins.size() > 1) {
                initialApprover = admins.stream()
                        .filter(admin -> !admin.getId().equals(applicant.getId()))
                        .findFirst()
                        // 如果过滤后为空（比如所有其他admin都恰好是自己，理论上不会在size>1时发生），则取第一个
                        .orElse(admins.stream().filter(admin -> !admin.getId().equals(applicant.getId())).findFirst().orElse(admins.get(0)));
            } else if (admins.size() == 1) {
                // 如果只有一个Admin，他自己是申请人，这里根据策略决定是否允许自审批。
                // 为简单起见，这里假设如果他是唯一的Admin，他可以作为审批人（后续审批逻辑可控制是否能“批准”自己的申请）
                initialApprover = admins.get(0);
                logger.warn("Admin {} 提交申请，系统中只有一个Admin。他将作为初始审批人。", applicant.getUsername());
            }

            if (initialApprover != null) {
                logger.info("申请人 {} (ADMIN) 的初始审批人确定为 Admin: {} (ID: {})", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                return initialApprover;
            } else {
                logger.error("Admin {} 提交申请，但无法确定审批人（系统中无Admin用户或配置问题）。", applicant.getUsername());
                throw new IllegalStateException("Admin申请无法确定初始审批人。");
            }
        }

        // 2. HR 提交的申请 -> 由 Admin 审批
        if (highestApplicantRole == Role.ROLE_HR) {
            initialApprover = findFirstUserWithRoleExcludingApplicant(Role.ROLE_ADMIN, applicant.getId());
            if (initialApprover != null) {
                logger.info("申请人 {} (HR) 的初始审批人确定为 Admin: {} (ID: {})", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                return initialApprover;
            } else {
                logger.error("HR {} 提交申请，但系统中无Admin用户处理。", applicant.getUsername());
                throw new IllegalStateException("HR申请无Admin审批，流程配置错误。");
            }
        }

        // 3. Department Manager 提交的申请 -> 由 HR 审批
        if (highestApplicantRole == Role.ROLE_DEPT_MANAGER) {
            initialApprover = findFirstUserWithRoleExcludingApplicant(Role.ROLE_HR, applicant.getId());
            if (initialApprover != null) {
                logger.info("申请人 {} (DEPT_MANAGER) 的初始审批人确定为 HR: {} (ID: {})", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                return initialApprover;
            } else {
                logger.warn("申请人 {} (DEPT_MANAGER) 未找到HR处理，尝试查找Admin...", applicant.getUsername());
                initialApprover = findFirstUserWithRoleExcludingApplicant(Role.ROLE_ADMIN, applicant.getId());
                if (initialApprover != null) {
                    logger.info("申请人 {} (DEPT_MANAGER) 转由 Admin: {} (ID: {}) 审批", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                    return initialApprover;
                }
                logger.error("DeptManager {} 提交申请，但系统中无HR或Admin用户处理。", applicant.getUsername());
                throw new IllegalStateException("DeptManager申请无HR/Admin审批，流程配置错误。");
            }
        }

        // 4. Team Lead 提交的申请
        if (highestApplicantRole == Role.ROLE_TEAM_LEAD) {
            User manager = applicant.getManager(); // 获取直属经理
            if (manager != null && manager.getRoles() != null && manager.getRoles().contains(Role.ROLE_DEPT_MANAGER) && !manager.getId().equals(applicant.getId())) {
                initialApprover = manager;
                logger.info("申请人 {} (TEAM_LEAD) 的初始审批人确定为其直属经理 (DeptManager): {} (ID: {})", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                return initialApprover;
            } else {
                logger.warn("申请人 {} (TEAM_LEAD) 无直属DeptManager或直属经理非DeptManager/是本人，尝试查找系统DeptManager...", applicant.getUsername());
                initialApprover = findFirstUserWithRoleExcludingApplicant(Role.ROLE_DEPT_MANAGER, applicant.getId());
                if (initialApprover != null) {
                    logger.info("申请人 {} (TEAM_LEAD) 转由系统DeptManager: {} (ID: {}) 审批", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                    return initialApprover;
                } else {
                    logger.warn("申请人 {} (TEAM_LEAD) 未找到DeptManager，尝试查找系统HR...", applicant.getUsername());
                    initialApprover = findFirstUserWithRoleExcludingApplicant(Role.ROLE_HR, applicant.getId());
                    if (initialApprover != null) {
                        logger.info("申请人 {} (TEAM_LEAD) 转由系统HR: {} (ID: {}) 审批", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                        return initialApprover;
                    } else {
                        logger.warn("申请人 {} (TEAM_LEAD) 未找到HR，尝试查找系统Admin...", applicant.getUsername());
                        initialApprover = findFirstUserWithRoleExcludingApplicant(Role.ROLE_ADMIN, applicant.getId());
                        if (initialApprover != null) {
                            logger.info("申请人 {} (TEAM_LEAD) 转由系统Admin: {} (ID: {}) 审批", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                            return initialApprover;
                        }
                        logger.error("TeamLead {} 提交申请，但系统中无DeptManager、HR或Admin用户处理。", applicant.getUsername());
                        throw new IllegalStateException("TeamLead申请无合适审批人，流程配置错误。");
                    }
                }
            }
        }

        // 5. 普通员工 (ROLE_EMPLOYEE) 提交的申请
        if (highestApplicantRole == Role.ROLE_EMPLOYEE) {
            User manager = applicant.getManager();
            if (manager != null && manager.getRoles() != null && !manager.getId().equals(applicant.getId())) {
                if (manager.getRoles().contains(Role.ROLE_TEAM_LEAD)) {
                    initialApprover = manager;
                    logger.info("申请人 {} (EMPLOYEE) 的初始审批人确定为其直属经理 (TeamLead): {} (ID: {})", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                    return initialApprover;
                } else if (manager.getRoles().contains(Role.ROLE_DEPT_MANAGER)) {
                    initialApprover = manager;
                    logger.info("申请人 {} (EMPLOYEE) 的初始审批人确定为其直属经理 (DeptManager): {} (ID: {})", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                    return initialApprover;
                } else if (manager.getRoles().contains(Role.ROLE_HR)) {
                    initialApprover = manager;
                    logger.info("申请人 {} (EMPLOYEE) 的初始审批人确定为其直属经理 (HR): {} (ID: {})", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                    return initialApprover;
                }
            }

            logger.warn("申请人 {} (EMPLOYEE) 无合适（非本人）直属经理，尝试按角色全局查找初始审批人...", applicant.getUsername());
            initialApprover = findFirstUserWithRoleExcludingApplicant(Role.ROLE_TEAM_LEAD, applicant.getId());
            if (initialApprover != null) {
                logger.info("申请人 {} (EMPLOYEE) 找到系统TeamLead: {} (ID: {}) 作为初始审批人。", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                return initialApprover;
            }

            initialApprover = findFirstUserWithRoleExcludingApplicant(Role.ROLE_HR, applicant.getId());
            if (initialApprover != null) {
                logger.warn("申请人 {} (EMPLOYEE) 未找到TeamLead，转由系统HR: {} (ID: {}) 作为初始审批人。", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                return initialApprover;
            }

            initialApprover = findFirstUserWithRoleExcludingApplicant(Role.ROLE_ADMIN, applicant.getId());
            if (initialApprover != null) {
                logger.warn("申请人 {} (EMPLOYEE) 未找到TeamLead/HR，转由系统Admin: {} (ID: {}) 作为初始审批人。", applicant.getUsername(), initialApprover.getUsername(), initialApprover.getId());
                return initialApprover;
            }

            logger.error("为申请人 {} (EMPLOYEE) 无法确定任何初始审批人。", applicant.getUsername());
            throw new IllegalStateException("Employee申请无合适审批人，请检查审批流程配置和用户汇报关系。");
        }

        logger.error("无法为申请人 {} (ID: {}, 最高角色: {}) 确定初始审批人。此情况不应发生，请检查审批逻辑覆盖。",
                applicant.getUsername(), applicant.getId(), highestApplicantRole);
        throw new IllegalStateException("未知的申请人角色或审批逻辑配置不完整。");
    }

    /**
     * 辅助方法：查找拥有指定角色的第一个用户，排除申请人自己。
     * @param role 要查找的角色
     * @param applicantId 申请人的ID，用于排除
     * @return 用户实体或null
     */
    private User findFirstUserWithRoleExcludingApplicant(Role role, Long applicantId) {
        List<User> users = userRepository.findByRolesContaining(role);
        if (!users.isEmpty()) {
            // 优先选择非申请人本人
            return users.stream()
                    .filter(user -> !user.getId().equals(applicantId))
                    .findFirst()
                    .orElseGet(() -> {
                        // 如果所有该角色的用户都是申请人自己（理论上不太可能，除非角色很少且特殊配置），
                        // 或者没有非申请人的用户，但仍有该角色的用户（例如，Admin审批Admin的申请，且只有一个Admin）
                        // 此时，如果业务允许自审批或必须指定一个，则返回第一个。
                        // 但如果严格不能自审批，这里应该返回null或抛异常。
                        // 为了简单，如果找不到非本人的，就看users列表里是不是只有本人
                        if (users.size() == 1 && users.get(0).getId().equals(applicantId)) {
                            //如果只有一个用户且是申请人，根据具体需求，可能允许，也可能不允许
                            //logger.warn("查找角色 {} 时，仅找到申请人 {} 自己。", role, applicantId);
                            //return users.get(0); // 示例：允许返回自己，让后续逻辑处理
                            return null; // 更安全的做法：如果找不到非本人的，则返回null，强制上级或不同角色处理
                        }
                        // 如果有多个用户，但凑巧第一个是申请人，取下一个（如果存在）
                        if(users.size() > 0 && !users.get(0).getId().equals(applicantId)) return users.get(0);
                        if(users.size() > 1 && users.get(0).getId().equals(applicantId)) return users.get(1);
                        // 最终回退到列表的第一个（如果存在）
                        return users.isEmpty() ? null : users.get(0);
                    });
        }
        return null;
    }

    // 旧的 findFirstUserWithRole 方法（可选保留或移除，新的 exludingApplicant 更常用）
    // private User findFirstUserWithRole(Role role) {
    //     List<User> users = userRepository.findByRolesContaining(role);
    //     if (!users.isEmpty()) {
    //         return users.get(0);
    //     }
    //     return null;
    // }
}