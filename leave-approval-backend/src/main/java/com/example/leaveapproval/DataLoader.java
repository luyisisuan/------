package com.example.leaveapproval; // 或者 com.example.leaveapproval.config

import com.example.leaveapproval.model.Role;
import com.example.leaveapproval.model.User;
import com.example.leaveapproval.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createAdminUserIfNotExists();
        // 你也可以在这里创建一些测试用的普通用户和审批人
        // createTestUsers();
    }

    private void createAdminUserIfNotExists() {
        String adminUsername = "admin";
        if (!userRepository.existsByUsername(adminUsername)) {
            User adminUser = new User();
            adminUser.setUsername(adminUsername);
            adminUser.setPassword(passwordEncoder.encode("admin123")); // 设置一个初始密码
            adminUser.setFullName("System Administrator");
            adminUser.setEmail("admin@example.com");
            adminUser.setRoles(Set.of(Role.ROLE_ADMIN, Role.ROLE_EMPLOYEE)); // 管理员也是一个员工
            adminUser.setEnabled(true); // 确保用户是启用的
            userRepository.save(adminUser);
            System.out.println("Created ADMIN user: " + adminUsername);
        }
    }

    // 可选：创建一些测试用户
    private void createTestUsers() {
        if (!userRepository.existsByUsername("employee1")) {
            User employee1 = new User("employee1", passwordEncoder.encode("password"), "Employee One", "emp1@example.com", Set.of(Role.ROLE_EMPLOYEE));
            userRepository.save(employee1);
            System.out.println("Created EMPLOYEE user: employee1");
        }
        if (!userRepository.existsByUsername("teamlead1")) {
            User teamlead1 = new User("teamlead1", passwordEncoder.encode("password"), "Team Lead One", "lead1@example.com", Set.of(Role.ROLE_TEAM_LEAD, Role.ROLE_EMPLOYEE));
            userRepository.save(teamlead1);
            System.out.println("Created TEAM_LEAD user: teamlead1");
        }
    }
}