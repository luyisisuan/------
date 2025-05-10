package com.example.leaveapproval.dto;

import com.example.leaveapproval.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;

@Data
public class AdminUserCreateRequest {

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

    private String department; // 可选

    private Long managerId;    // 可选

    @NotEmpty(message = "User must have at least one role assigned by admin.")
    private Set<Role> roles;
}