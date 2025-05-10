package com.example.leaveapproval.service.user;

import com.example.leaveapproval.dto.AdminUserCreateRequest;
import com.example.leaveapproval.dto.UserDto;
import com.example.leaveapproval.dto.UserUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List; // 新增：导入 List
import java.util.Optional;

/**
 * 用户管理服务接口 (管理员权限)。
 * 定义了管理员对用户实体进行操作的业务逻辑契约。
 */
public interface AdminUserService {

    /**
     * 分页获取所有用户信息。
     *
     * @param pageable 分页参数对象。
     * @return 包含用户DTO的分页结果对象。
     */
    Page<UserDto> getAllUsers(Pageable pageable);

    /**
     * 根据用户ID获取用户信息。
     *
     * @param id 用户的唯一标识ID。
     * @return 包含用户DTO的Optional对象，如果用户不存在则为空。
     */
    Optional<UserDto> getUserById(Long id);

    /**
     * 管理员创建新用户。
     *
     * @param createRequest 包含新用户创建信息的DTO对象。
     * @return 创建成功后的用户DTO对象。
     * @throws org.springframework.dao.DataIntegrityViolationException 如果用户名或邮箱已存在。
     * @throws IllegalArgumentException 如果DTO中未指定用户角色。
     */
    UserDto createUser(AdminUserCreateRequest createRequest);

    /**
     * 管理员更新指定ID的用户信息。
     *
     * @param id 要更新的用户的ID。
     * @param updateRequest 包含用户更新信息的DTO对象。
     * @return 更新成功后的用户DTO对象的Optional，如果用户不存在则为空。
     * @throws com.example.leaveapproval.exception.ResourceNotFoundException 如果用户或其指定的上级用户不存在。
     * @throws org.springframework.dao.DataIntegrityViolationException 如果更新后的邮箱与现有其他用户冲突。
     * @throws IllegalArgumentException 如果尝试将用户角色列表更新为空。
     */
    Optional<UserDto> updateUser(Long id, UserUpdateRequest updateRequest);

    /**
     * 管理员根据ID删除用户。
     *
     * @param id 要删除的用户的ID。
     * @throws com.example.leaveapproval.exception.ResourceNotFoundException 如果指定ID的用户不存在。
     */
    void deleteUser(Long id);

    // --- 新增方法签名 ---
    /**
     * 获取可作为经理的用户列表。
     * 这些用户通常拥有特定的管理角色 (例如 TeamLead, DeptManager, HR, Admin)。
     * 用于前端下拉列表选择经理。
     *
     * @return 可选经理的用户DTO列表。
     */
    List<UserDto> getPotentialManagers();
    // --- 新增方法签名结束 ---
}