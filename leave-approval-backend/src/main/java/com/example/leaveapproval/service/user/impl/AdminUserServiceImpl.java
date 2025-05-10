package com.example.leaveapproval.service.user.impl;

import com.example.leaveapproval.dto.AdminUserCreateRequest;
import com.example.leaveapproval.dto.UserDto;
import com.example.leaveapproval.dto.UserUpdateRequest;
import com.example.leaveapproval.exception.ResourceNotFoundException;
import com.example.leaveapproval.model.Role; // 新增：导入 Role 枚举
import com.example.leaveapproval.model.User;
import com.example.leaveapproval.repository.UserRepository;
import com.example.leaveapproval.service.user.AdminUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator; // 新增：导入 Comparator
import java.util.HashSet;   // 新增：导入 HashSet
import java.util.List;      // 新增：导入 List
import java.util.Optional;
import java.util.Set;       // 新增：导入 Set
import java.util.stream.Collectors; // 新增：导入 Collectors

/**
 * {@link AdminUserService} 接口的实现类。
 * 提供了管理员管理用户账户的具体业务逻辑。
 */
@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminUserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        logger.info("管理员操作：获取所有用户，分页参数：{}", pageable);
        return userRepository.findAll(pageable).map(UserDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserById(Long id) {
        logger.info("管理员操作：根据ID获取用户，用户ID：{}", id);
        return userRepository.findById(id).map(UserDto::fromEntity);
    }

    @Override
    public UserDto createUser(AdminUserCreateRequest createRequest) {
        logger.info("管理员操作：尝试创建新用户，用户名：{}", createRequest.getUsername());

        if (userRepository.existsByUsername(createRequest.getUsername())) {
            String errorMessage = "错误：用户名 '" + createRequest.getUsername() + "' 已被占用！";
            logger.warn(errorMessage);
            throw new DataIntegrityViolationException(errorMessage);
        }
        if (userRepository.existsByEmail(createRequest.getEmail())) {
            String errorMessage = "错误：邮箱 '" + createRequest.getEmail() + "' 已被使用！";
            logger.warn(errorMessage);
            throw new DataIntegrityViolationException(errorMessage);
        }
        if (createRequest.getRoles() == null || createRequest.getRoles().isEmpty()) {
            String errorMessage = "错误：管理员创建用户时必须分配角色。";
            logger.warn(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        User user = new User();
        user.setUsername(createRequest.getUsername());
        user.setPassword(passwordEncoder.encode(createRequest.getPassword()));
        user.setFullName(createRequest.getFullName());
        user.setEmail(createRequest.getEmail());

        if (StringUtils.hasText(createRequest.getDepartment())) {
            user.setDepartment(createRequest.getDepartment());
        }

        if (createRequest.getManagerId() != null) {
            User manager = userRepository.findById(createRequest.getManagerId())
                    .orElseThrow(() -> {
                        String errorMessage = "指定的上级用户 (ID: " + createRequest.getManagerId() + ") 不存在。";
                        logger.warn(errorMessage);
                        return new ResourceNotFoundException("Manager", "id", createRequest.getManagerId());
                    });
            user.setManager(manager);
        }

        user.setRoles(createRequest.getRoles());
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        logger.info("管理员操作：用户创建成功，用户ID：{}", savedUser.getId());
        return UserDto.fromEntity(savedUser);
    }

    @Override
    public Optional<UserDto> updateUser(Long id, UserUpdateRequest updateRequest) {
        logger.info("管理员操作：尝试更新用户，用户ID：{}", id);

        return userRepository.findById(id).flatMap(user -> {
            boolean isModified = false;

            if (StringUtils.hasText(updateRequest.getFullName()) && !updateRequest.getFullName().equals(user.getFullName())) {
                user.setFullName(updateRequest.getFullName());
                isModified = true;
            }

            if (StringUtils.hasText(updateRequest.getEmail()) && !updateRequest.getEmail().equals(user.getEmail())) {
                Optional<User> existingUserWithEmail = userRepository.findByEmail(updateRequest.getEmail());
                if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getId().equals(user.getId())) {
                    String errorMessage = "错误：邮箱 '" + updateRequest.getEmail() + "' 已被其他用户使用！";
                    logger.warn(errorMessage);
                    throw new DataIntegrityViolationException(errorMessage);
                }
                user.setEmail(updateRequest.getEmail());
                isModified = true;
            }

            if (updateRequest.getDepartment() != null && !updateRequest.getDepartment().equals(user.getDepartment())) {
                user.setDepartment(updateRequest.getDepartment());
                isModified = true;
            }

            if (updateRequest.getManagerId() != null) {
                if (user.getManager() == null || !updateRequest.getManagerId().equals(user.getManager().getId())) {
                    if (updateRequest.getManagerId().equals(user.getId())) {
                        String errorMsg = "错误：用户不能将自己设置为其直属上级。用户ID: " + id;
                        logger.warn(errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                    User manager = userRepository.findById(updateRequest.getManagerId())
                            .orElseThrow(() -> {
                                String errorMessage = "指定的上级用户 (ID: " + updateRequest.getManagerId() + ") 不存在。";
                                logger.warn(errorMessage);
                                return new ResourceNotFoundException("Manager", "id", updateRequest.getManagerId());
                            });
                    user.setManager(manager);
                    isModified = true;
                }
            } else if (updateRequest.getManagerId() == null && user.getManager() != null) { // 注意这里是 else if
                user.setManager(null);
                isModified = true;
            }


            if (updateRequest.getRoles() != null && !updateRequest.getRoles().isEmpty()) {
                if (!user.getRoles().equals(updateRequest.getRoles())) {
                    user.setRoles(updateRequest.getRoles());
                    isModified = true;
                }
            } else if (updateRequest.getRoles() != null && updateRequest.getRoles().isEmpty()){
                String errorMessage = "错误：更新用户时角色列表不能为空。如需禁用用户，请使用 'enabled' 标志。";
                logger.warn(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }

            if (updateRequest.getEnabled() != null && updateRequest.getEnabled() != user.isEnabled()) {
                user.setEnabled(updateRequest.getEnabled());
                isModified = true;
            }

            if (isModified) {
                User updatedUser = userRepository.save(user);
                logger.info("管理员操作：用户更新成功，用户ID：{}", updatedUser.getId());
                return Optional.of(UserDto.fromEntity(updatedUser));
            } else {
                logger.info("管理员操作：未检测到用户信息的实际更改，用户ID：{}。返回现有数据。", id);
                return Optional.of(UserDto.fromEntity(user));
            }
        });
    }
    @Override
    public void deleteUser(Long id) {
        logger.info("管理员操作：尝试删除用户，用户ID：{}", id);
        if (!userRepository.existsById(id)) {
            String errorMessage = "错误：尝试删除的用户 (ID: " + id + ") 不存在。";
            logger.warn(errorMessage);
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepository.deleteById(id);
        logger.info("管理员操作：用户删除成功，用户ID：{}", id);
    }

    // --- 新增方法实现 ---
    @Override
    @Transactional(readOnly = true) // 这是一个只读操作
    public List<UserDto> getPotentialManagers() {
        logger.info("获取所有潜在的经理用户列表。");

        // 定义哪些角色可以被视为经理角色
        Set<Role> managerRoles = Set.of(
                Role.ROLE_TEAM_LEAD,
                Role.ROLE_DEPT_MANAGER,
                Role.ROLE_HR,
                Role.ROLE_ADMIN
        );

        // 使用一个 Set 来存储用户，以自动处理因用户拥有多个经理角色而可能产生的重复
        Set<User> potentialManagersSet = new HashSet<>();

        // 遍历定义的经理角色，并从 userRepository 获取拥有这些角色的用户
        for (Role role : managerRoles) {
            // 假设 userRepository.findByRolesContaining(role) 返回 List<User>
            // 并且 User 类正确实现了 equals 和 hashCode 方法（Lombok @Data 通常会正确生成）
            // 以便 Set<User> 能够正确去重。
            potentialManagersSet.addAll(userRepository.findByRolesContaining(role));
        }

        // 将 User 实体集合转换为 UserDto 列表，并按姓名排序
        return potentialManagersSet.stream()
                .map(user -> {
                    // 使用 UserDto.fromEntity 转换，确保 UserDto 中包含所需信息
                    // 如果 UserDto.fromEntity 不符合这里的需求（例如，不需要 managerId, managerUsername），
                    // 你可以在这里手动创建和填充 UserDto。
                    // 为了保持一致性，并假设 UserDto.fromEntity 已包含足够的信息（如id, fullName, username, roles），我们直接使用它。
                    UserDto dto = UserDto.fromEntity(user);
                    // 如果希望在DTO中明确角色信息，确保UserDto.fromEntity会填充roles字段
                    return dto;
                })
                .sorted(Comparator.comparing(UserDto::getFullName, String.CASE_INSENSITIVE_ORDER)) // 按全名不区分大小写排序
                .collect(Collectors.toList());
    }
    // --- 新增方法实现结束 ---
}