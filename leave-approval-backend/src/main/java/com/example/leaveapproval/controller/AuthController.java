package com.example.leaveapproval.controller;

import com.example.leaveapproval.dto.JwtResponse;
import com.example.leaveapproval.dto.LoginRequest;
import com.example.leaveapproval.dto.MessageResponse;
import com.example.leaveapproval.dto.RegisterRequest;
import com.example.leaveapproval.dto.UserDto; // 新增导入 UserDto，用于经理列表返回
import com.example.leaveapproval.model.Role;
import com.example.leaveapproval.model.User;
import com.example.leaveapproval.repository.UserRepository;
import com.example.leaveapproval.service.user.AdminUserService; // 新增导入 AdminUserService
import com.example.leaveapproval.util.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired // 新增注入 AdminUserService
    private AdminUserService adminUserService; // 用于获取潜在经理列表

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User userDetails = (User) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) { // registerRequest 假设已包含 managerId
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username '" + registerRequest.getUsername() + "' is already taken!"));
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email '" + registerRequest.getEmail() + "' is already in use!"));
        }

        // 创建新用户账户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(encoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getFullName());

        // --- 修改开始：处理 managerId ---
        if (registerRequest.getManagerId() != null) {
            User manager = userRepository.findById(registerRequest.getManagerId())
                    .orElse(null); // 查找经理

            if (manager == null) {
                // 如果选择的经理ID无效，返回错误
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Selected manager with ID " + registerRequest.getManagerId() + " not found."));
            }
            user.setManager(manager); // 设置经理
        }
        // --- 修改结束 ---

        // 默认新注册用户为普通员工
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_EMPLOYEE); // 确保 Role 枚举被正确导入
        user.setRoles(roles);
        user.setEnabled(true);

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    // --- 新增API端点：获取可选的经理列表 ---
    @GetMapping("/potential-managers")
    // 可选：添加权限控制，例如 @PreAuthorize("isAuthenticated()") 或针对特定角色
    public ResponseEntity<List<UserDto>> getPotentialManagersList() {
        // 调用服务层方法获取经理列表
        // 我们将在下一步确保 adminUserService 中有 getPotentialManagers() 方法
        List<UserDto> managers = adminUserService.getPotentialManagers();
        return ResponseEntity.ok(managers);
    }
    // --- 新增API端点结束 ---
}