package com.example.leaveapproval.dto;

import com.example.leaveapproval.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UserUpdateRequest {
    // 用户名通常不允许修改，如果允许，也需要特别处理唯一性
    // private String username;

    @Size(min = 2, max = 50)
    private String fullName;

    @Email
    @Size(max = 50)
    private String email; // 如果允许修改邮箱，需要检查唯一性

    private String department;

    private Long managerId; // 更新直属上级

    @NotEmpty(message = "Roles cannot be empty")
    private Set<Role> roles; // 管理员可以修改用户角色

    private Boolean enabled; // 管理员可以启用/禁用用户

    // 密码修改通常应该有单独的接口或流程
    // private String password;
}