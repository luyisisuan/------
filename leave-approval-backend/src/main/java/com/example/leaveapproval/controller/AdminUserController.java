package com.example.leaveapproval.controller;

import com.example.leaveapproval.dto.AdminUserCreateRequest;
import com.example.leaveapproval.dto.UserDto;
import com.example.leaveapproval.dto.UserUpdateRequest;
import com.example.leaveapproval.dto.MessageResponse;
import com.example.leaveapproval.service.user.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional; // 确保导入 Optional

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Autowired
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    // ... getAllUsers, getUserById, createUser 方法不变 ...

    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        Page<UserDto> users = adminUserService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return adminUserService.getUserById(id)
                .map(userDto -> ResponseEntity.ok(userDto)) // 更明确的写法
                // 或者 .map(ResponseEntity::ok) 之前的写法也应该可以，但下面的updateUser需要调整
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody AdminUserCreateRequest createRequest) {
        UserDto createdUser = adminUserService.createUser(createRequest);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }


    /**
     * 管理员更新指定ID的用户信息。
     * @param id 要更新的用户ID。
     * @param updateRequest 包含更新信息的 DTO。
     * @return 更新成功后的用户 DTO，如果用户未找到则返回 404。
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest updateRequest) {
        Optional<UserDto> updatedUserOptional = adminUserService.updateUser(id, updateRequest);

        return updatedUserOptional
                .map(userDto -> ResponseEntity.ok(userDto)) // 如果 Optional 有值 (UserDto)，则包装成 ResponseEntity.ok(userDto)
                .orElseGet(() -> ResponseEntity.notFound().build()); // 如果 Optional 为空，则返回 404
        // 或者更简洁的写法 (如果 IDE 和编译器能正确推断):
        // return adminUserService.updateUser(id, updateRequest)
        // .map(ResponseEntity::ok)
        // .orElse(ResponseEntity.notFound().build());
        // 但鉴于你遇到的错误，第一种更明确的写法可能更好，以避免类型推断问题。
    }

    /**
     * 管理员根据ID删除用户。
     * @param id 要删除的用户ID。
     * @return 成功删除的消息和 HTTP 状态 200 (OK)。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully!"));
    }
}