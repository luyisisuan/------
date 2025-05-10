package com.example.leaveapproval.dto;

import com.example.leaveapproval.model.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String department;
    private Long managerId; // 直属上级ID
    private String managerUsername; // 直属上级用户名 (可选，方便前端展示)
    private Set<Role> roles;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 可以添加一个构造函数或静态工厂方法从 User 实体转换
    public static UserDto fromEntity(com.example.leaveapproval.model.User user) {
        if (user == null) return null;
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setDepartment(user.getDepartment());
        if (user.getManager() != null) {
            dto.setManagerId(user.getManager().getId());
            dto.setManagerUsername(user.getManager().getUsername());
        }
        dto.setRoles(user.getRoles());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}