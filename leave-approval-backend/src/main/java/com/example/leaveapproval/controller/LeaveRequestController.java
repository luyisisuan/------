package com.example.leaveapproval.controller;

import com.example.leaveapproval.dto.ApprovalActionDto;
import com.example.leaveapproval.dto.LeaveRequestCreateDto;
import com.example.leaveapproval.dto.LeaveRequestViewDto;
import com.example.leaveapproval.model.LeaveStatus;
import com.example.leaveapproval.model.Role; // <<--- 导入 Role 枚举
import com.example.leaveapproval.model.User;
import com.example.leaveapproval.service.leave.LeaveRequestManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/leave-requests") // 所有请假相关API的基础路径
public class LeaveRequestController {

    private static final Logger logger = LoggerFactory.getLogger(LeaveRequestController.class);

    private final LeaveRequestManagementService leaveRequestManagementService;

    @Autowired
    public LeaveRequestController(LeaveRequestManagementService leaveRequestManagementService) {
        this.leaveRequestManagementService = leaveRequestManagementService;
    }

    /**
     * 内部辅助方法：获取当前认证的用户实体。
     */
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            String principalName = authentication != null ? authentication.getPrincipal().toString() : "null";
            logger.warn("无法获取当前认证用户，认证信息主体为: {}", principalName);
            throw new IllegalStateException("用户未登录或认证信息无效。请重新登录。");
        }
        return (User) authentication.getPrincipal();
    }

    /**
     * 员工提交新的请假申请。
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()") // 任何已认证用户都可以提交
    public ResponseEntity<LeaveRequestViewDto> submitLeaveRequest(@Valid @RequestBody LeaveRequestCreateDto createDto) {
        logger.info("用户 {} 正在提交新的请假申请...", getCurrentAuthenticatedUser().getUsername());
        LeaveRequestViewDto createdLeaveRequest = leaveRequestManagementService.submitLeaveRequest(createDto);
        logger.info("请假申请 (ID: {}) 已成功提交。", createdLeaveRequest.getId());
        return new ResponseEntity<>(createdLeaveRequest, HttpStatus.CREATED);
    }

    /**
     * 审批人处理请假申请（批准/驳回）。
     */
    @PostMapping("/{leaveRequestId}/action")
    @PreAuthorize("isAuthenticated()") // 确保用户已登录，具体权限在Service层校验
    public ResponseEntity<LeaveRequestViewDto> processApprovalAction(
            @PathVariable Long leaveRequestId,
            @Valid @RequestBody ApprovalActionDto actionDto) {
        User currentUser = getCurrentAuthenticatedUser();
        logger.info("审批人 {} (ID: {}) 正在对请假申请ID {} 执行审批操作：{}",
                currentUser.getUsername(), currentUser.getId(), leaveRequestId, actionDto.getDecision());
        LeaveRequestViewDto updatedLeaveRequest = leaveRequestManagementService.processApprovalAction(leaveRequestId, actionDto, currentUser.getId());
        logger.info("请假申请 (ID: {}) 的审批操作已处理完成，新状态：{}", updatedLeaveRequest.getId(), updatedLeaveRequest.getStatus());
        return ResponseEntity.ok(updatedLeaveRequest);
    }

    /**
     * 员工取消自己提交的请假申请。
     */
    @PostMapping("/{leaveRequestId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestViewDto> cancelLeaveRequest(@PathVariable Long leaveRequestId) {
        User currentUser = getCurrentAuthenticatedUser();
        logger.info("用户 {} (ID: {}) 正在尝试取消请假申请ID {}", currentUser.getUsername(), currentUser.getId(), leaveRequestId);
        LeaveRequestViewDto cancelledLeaveRequest = leaveRequestManagementService.cancelLeaveRequest(leaveRequestId, currentUser.getId());
        logger.info("请假申请 (ID: {}) 已成功取消。", cancelledLeaveRequest.getId());
        return ResponseEntity.ok(cancelledLeaveRequest);
    }

    /**
     * 根据ID获取请假申请的详细信息。
     */
    @GetMapping("/{leaveRequestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestViewDto> getLeaveRequestDetails(@PathVariable Long leaveRequestId) {
        User currentUser = getCurrentAuthenticatedUser();
        logger.debug("用户 {} 查询请假申请详情，ID: {}", currentUser.getUsername(), leaveRequestId);
        // Service层可能需要根据currentUser和leaveRequestId做进一步权限判断
        Optional<LeaveRequestViewDto> leaveRequestOpt = leaveRequestManagementService.getLeaveRequestDetailsById(leaveRequestId);
        return leaveRequestOpt
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取当前登录用户提交的所有请假申请（分页）。
     */
    @GetMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LeaveRequestViewDto>> getMyLeaveRequests(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User currentUser = getCurrentAuthenticatedUser();
        logger.debug("用户 {} (ID: {}) 查询我的请假申请列表，分页：{}", currentUser.getUsername(), currentUser.getId(), pageable);
        Page<LeaveRequestViewDto> myRequests = leaveRequestManagementService.getMyLeaveRequests(currentUser.getId(), pageable);
        return ResponseEntity.ok(myRequests);
    }

    /**
     * 获取待当前用户审批的请假申请列表（分页）。
     * 如果是 Admin 用户，则获取所有待审批的列表。
     * 其他审批角色，则获取指派给自己的待审批列表。
     */
    @GetMapping("/pending-approvals") // <<--- 修改了路径名，使其更通用
    @PreAuthorize("hasAnyRole('TEAM_LEAD', 'DEPT_MANAGER', 'HR', 'ADMIN')") // 确保只有这些角色能调用
    public ResponseEntity<Page<LeaveRequestViewDto>> getVisiblePendingRequests(
            @RequestParam(name = "status", required = false) LeaveStatus status, // 可选的状态参数
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        User currentUser = getCurrentAuthenticatedUser();
        Page<LeaveRequestViewDto> pendingRequests;

        // 检查当前用户是否是 Admin
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ROLE_ADMIN));

        if (isAdmin) {
            // Admin 用户获取所有指定状态（或默认 PENDING_APPROVAL）的请假申请
            logger.info("Admin用户 {} (ID: {}) 查询所有状态为 '{}' 的待审批列表，分页：{}",
                    currentUser.getUsername(), currentUser.getId(), (status != null ? status : "默认(PENDING_APPROVAL)"), pageable);
            pendingRequests = leaveRequestManagementService.adminGetAllPendingRequests(status, pageable);
        } else {
            // 其他审批角色（TeamLead, DeptManager, HR）获取指派给自己的待审批列表
            logger.info("审批人 {} (ID: {}) 查询指派给自己的状态为 '{}' 的待审批列表，分页：{}",
                    currentUser.getUsername(), currentUser.getId(), (status != null ? status : "默认(PENDING_APPROVAL)"), pageable);
            pendingRequests = leaveRequestManagementService.getPendingApprovalRequestsForUser(currentUser.getId(), status, pageable);
        }
        return ResponseEntity.ok(pendingRequests);
    }

    // 如果之前有 /pending-my-approval 路径并且前端还在使用，可以保留它并重定向或让它也执行新逻辑，
    // 但建议统一到一个路径。
}