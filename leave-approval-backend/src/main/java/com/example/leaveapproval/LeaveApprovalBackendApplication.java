package com.example.leaveapproval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport; // <<--- 导入

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO) // <<--- 添加这行注解
public class LeaveApprovalBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeaveApprovalBackendApplication.class, args);
	}

}