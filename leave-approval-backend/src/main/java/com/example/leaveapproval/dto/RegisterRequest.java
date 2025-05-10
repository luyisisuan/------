package com.example.leaveapproval.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
// import java.util.Set; // 如果注册时允许指定角色，普通用户注册通常不需要

@Data
public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank
    @Email
    @Size(max = 50)
    private String email;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

    @NotBlank
    @Size(min = 2, max = 50)
    private String fullName;

    // 新增：直属经理ID (可选)
    // 如果是必填项，可以在这里添加 @NotNull 注解，但这取决于您的业务逻辑
    private Long managerId;

    // 普通用户注册时，角色通常是固定的 (例如 ROLE_EMPLOYEE)，由后端服务分配。
    // 如果允许用户在注册时选择角色，则需要取消下面行的注释，并确保后端逻辑支持。
    // private Set<String> roles;
}