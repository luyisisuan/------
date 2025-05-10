package com.example.leaveapproval.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
public class User implements UserDetails { // 实现 UserDetails 接口以集成 Spring Security

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username; // 用于登录

    @Column(nullable = false)
    private String password; // 存储加密后的密码

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 100)
    private String department;

    // 使用 ElementCollection 存储角色集合，因为一个用户可能有多个角色 (虽然本例中可能一个用户一个核心业务角色 + 一个ADMIN角色)
    // 或者如果角色是固定的几个，并且希望强类型，也可以只用一个 @Enumerated(EnumType.STRING) Role role;
    // 这里使用 Set<Role> 更灵活，比如一个 ADMIN 也可以是 DEPT_MANAGER
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER) // EAGER 加载角色信息
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<Role> roles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY) // 懒加载，需要时再查询
    @JoinColumn(name = "manager_id")
    private User manager; // 直属上级

    // UserDetails 接口要求的字段，通常可以默认为 true
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true; // 用户是否启用

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Spring Security UserDetails 接口实现
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());
    }

    // username 和 password 由 Lombok 的 @Data 生成 getter
    // isAccountNonExpired, isAccountNonLocked, isCredentialsNonExpired, isEnabled 由 Lombok 生成 getter

    // 构造函数等可根据需要添加
    public User(String username, String password, String fullName, String email, Set<Role> roles) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.roles = roles;
    }
}