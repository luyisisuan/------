package com.example.leaveapproval.repository;

import com.example.leaveapproval.model.User;
import com.example.leaveapproval.model.Role; // <<--- 导入 Role 枚举
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // <<--- 导入 List
import java.util.Optional; // 导入 Optional

@Repository // 标记这是一个Spring Data JPA Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 根据用户名查找用户 (用于登录和唯一性检查)
    Optional<User> findByUsername(String username);

    // 根据邮箱查找用户 (用于唯一性检查)
    Optional<User> findByEmail(String email);

    // 检查用户名是否存在
    Boolean existsByUsername(String username);

    // 检查邮箱是否存在
    Boolean existsByEmail(String email);

    /**
     * 查找系统中拥有特定角色的所有用户列表。
     * Spring Data JPA 会根据方法名和实体关系自动生成查询。
     *
     * @param role 角色枚举值 (例如 Role.ROLE_TEAM_LEAD)。
     * @return 拥有该角色的用户列表；如果不存在，返回空列表。
     */
    List<User> findByRolesContaining(Role role); // <<--- 添加这个关键方法

    // 你可以根据需要添加其他查询方法，例如：
    // 查找拥有特定角色的第一个用户（按ID升序）
    // Optional<User> findTopByRolesContainingOrderByIdAsc(Role role);
}