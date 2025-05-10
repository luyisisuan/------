package com.example.leaveapproval.model;

import java.util.Set;

public enum Role {
    ROLE_EMPLOYEE(1),     // 普通员工
    ROLE_TEAM_LEAD(2),    // 团队领导 (审批人)
    ROLE_DEPT_MANAGER(3), // 部门经理 (审批人)
    ROLE_HR(4),           // HR (审批人)
    ROLE_ADMIN(5);        // 系统管理员 (管理用户和系统配置)

    private final int hierarchyLevel;

    Role(int level) {
        this.hierarchyLevel = level;
    }

    public int getHierarchyLevel() {
        return hierarchyLevel;
    }

    /**
     * 检查当前角色是否比另一个角色级别高。
     * @param other 要比较的角色
     * @return 如果当前角色级别更高，则为 true
     */
    public boolean isSeniorTo(Role other) {
        if (other == null) {
            return true; // 任何角色都比 null 级别高 (根据业务场景可调整)
        }
        return this.hierarchyLevel > other.hierarchyLevel;
    }

    /**
     * 检查当前角色是否与另一个角色级别相同或更高。
     * @param other 要比较的角色
     * @return 如果当前角色级别相同或更高，则为 true
     */
    public boolean isSameOrSeniorTo(Role other) {
        if (other == null) {
            return true; // 任何角色都比 null 级别高 (根据业务场景可调整)
        }
        return this.hierarchyLevel >= other.hierarchyLevel;
    }

    /**
     * 从一组角色中获取层级最高的角色。
     * @param roles 一组角色
     * @return 层级最高的角色，如果集合为空或null，则返回null
     */
    public static Role getHighestRole(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.stream()
                .max((r1, r2) -> Integer.compare(r1.hierarchyLevel, r2.hierarchyLevel))
                .orElse(null);
    }
}