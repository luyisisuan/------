package com.example.leaveapproval.service.leave.impl;

import com.example.leaveapproval.dto.ApprovalActionDto;
import com.example.leaveapproval.dto.ApprovalHistoryViewDto;
import com.example.leaveapproval.dto.LeaveRequestCreateDto;
import com.example.leaveapproval.dto.LeaveRequestViewDto;
import com.example.leaveapproval.exception.ResourceNotFoundException;
import com.example.leaveapproval.model.*; // User, LeaveRequest, ApprovalHistory, Role, LeaveStatus, LeaveType
import com.example.leaveapproval.model.state.LeaveState; // 明确导入 LeaveState 接口
import com.example.leaveapproval.repository.ApprovalHistoryRepository;
import com.example.leaveapproval.repository.LeaveRequestRepository;
import com.example.leaveapproval.repository.UserRepository;
import com.example.leaveapproval.service.approval.chain.Approver;
import com.example.leaveapproval.service.leave.LeaveRequestManagementService;
import com.example.leaveapproval.service.leave.LeaveRequestProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveRequestManagementServiceImpl implements LeaveRequestManagementService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveRequestManagementServiceImpl.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final LeaveRequestProcessService leaveRequestProcessService;
    private final ApplicationContext applicationContext;

    @Autowired
    public LeaveRequestManagementServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            UserRepository userRepository,
            ApprovalHistoryRepository approvalHistoryRepository,
            @Qualifier("genericLeaveProcessService") LeaveRequestProcessService leaveRequestProcessService,
            ApplicationContext applicationContext) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.userRepository = userRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.leaveRequestProcessService = leaveRequestProcessService;
        this.applicationContext = applicationContext;
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            String principalName = authentication != null ? authentication.getPrincipal().toString() : "null";
            logger.warn("无法获取当前认证用户，认证信息主体为: {}", principalName);
            throw new IllegalStateException("用户未登录或认证信息无效。请重新登录。");
        }
        return (User) authentication.getPrincipal();
    }

    @Override
    public LeaveRequestViewDto submitLeaveRequest(LeaveRequestCreateDto createDto) {
        logger.info("接收到新的请假申请提交请求。");
        LeaveRequestViewDto createdLeaveRequest = leaveRequestProcessService.submitLeaveRequest(createDto);
        logger.info("请假申请 (ID: {}) 已成功提交并启动审批流程，当前状态: {}",
                createdLeaveRequest.getId(), createdLeaveRequest.getStatus());
        return createdLeaveRequest;
    }

    @Override
    public LeaveRequestViewDto processApprovalAction(Long leaveRequestId, ApprovalActionDto actionDto, Long approverUserId) {
        logger.info("用户ID {} 尝试处理请假申请ID {}，决定：{}，意见：'{}'",
                approverUserId, leaveRequestId, actionDto.getDecision(), actionDto.getComments());

        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> {
                    logger.warn("处理审批操作失败：未找到请假申请ID {}", leaveRequestId);
                    return new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId);
                });

        User actionTakingApprover = userRepository.findById(approverUserId)
                .orElseThrow(() -> {
                    logger.warn("处理审批操作失败：未找到执行操作的用户ID {}", approverUserId);
                    return new ResourceNotFoundException("User (Approver)", "id", approverUserId);
                });

        LeaveState currentLeaveState = leaveRequest.getCurrentState();

        try {
            if (actionDto.getDecision() == ApprovalHistory.Decision.APPROVED) {
                logger.debug("调用 LeaveRequest ID {} 的当前状态 {} 的 approve 方法进行前置处理。", leaveRequestId, currentLeaveState.getStatusEnum());
                currentLeaveState.approve(leaveRequest, actionTakingApprover, actionDto.getDecision(), actionDto.getComments());
            } else if (actionDto.getDecision() == ApprovalHistory.Decision.REJECTED) {
                logger.debug("调用 LeaveRequest ID {} 的当前状态 {} 的 reject 方法进行前置处理。", leaveRequestId, currentLeaveState.getStatusEnum());
                currentLeaveState.reject(leaveRequest, actionTakingApprover, actionDto.getDecision(), actionDto.getComments());
            } else {
                logger.warn("在 processApprovalAction 中收到非 APPROVED/REJECTED 的审批决定类型: {}，请假ID: {}",
                        actionDto.getDecision(), leaveRequestId);
                throw new IllegalArgumentException("无效的审批操作决定类型: " + actionDto.getDecision() + "。请使用专门的取消接口（如果适用）。");
            }
        } catch (IllegalStateException e) {
            logger.warn("请假申请 ID {} 的当前状态 {} 不允许执行 {} 操作：{}",
                    leaveRequestId, currentLeaveState.getStatusEnum(), actionDto.getDecision(), e.getMessage());
            throw e;
        }

        boolean isAdminAction = actionTakingApprover.getRoles().contains(Role.ROLE_ADMIN);
        boolean isCurrentUserAssignedApprover = (leaveRequest.getCurrentApprover() != null &&
                leaveRequest.getCurrentApprover().getId().equals(actionTakingApprover.getId()));

        if (!isAdminAction && !isCurrentUserAssignedApprover) {
            String currentApproverUsername = leaveRequest.getCurrentApprover() != null ? leaveRequest.getCurrentApprover().getUsername() : "未指定";
            String errorMsg = String.format("权限不足：用户 %s 不是请假申请 %d 的当前指定审批人 (%s) 且不具备越级审批权限。",
                    actionTakingApprover.getUsername(), leaveRequestId, currentApproverUsername);
            logger.warn(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        User designatedApproverForNode = leaveRequest.getCurrentApprover();
        if (designatedApproverForNode == null && leaveRequest.getStatusEnum() == LeaveStatus.PENDING_APPROVAL) {
            if (isAdminAction) {
                designatedApproverForNode = actionTakingApprover;
                logger.warn("请假申请 {} 状态为 PENDING_APPROVAL 但无当前审批人，Admin {} 将尝试代表节点处理。",
                        leaveRequestId, actionTakingApprover.getUsername());
            } else {
                logger.error("请假申请 {} 状态为 PENDING_APPROVAL 但没有指定的当前审批人，且操作者非Admin，无法处理。", leaveRequestId);
                throw new IllegalStateException("请假申请 " + leaveRequestId + " 处于待审批状态但没有指定的当前审批人，无法处理。");
            }
        } else if (designatedApproverForNode == null && leaveRequest.getStatusEnum() != LeaveStatus.PENDING_APPROVAL) {
            if (isAdminAction) {
                designatedApproverForNode = actionTakingApprover;
                logger.warn("请假申请 {} 状态为 {} 且无当前审批人，Admin {} 将尝试代表节点处理。",
                        leaveRequestId, leaveRequest.getStatusEnum(), actionTakingApprover.getUsername());
            } else {
                logger.error("请假申请 {} 状态为 {} 且无当前审批人，非Admin用户无法操作。", leaveRequestId, leaveRequest.getStatusEnum());
                throw new IllegalStateException("请假申请 " + leaveRequestId + " 当前状态 ("+ leaveRequest.getStatusEnum() +") 或配置不允许此操作。");
            }
        }

        Approver approverNode = getApproverNodeForUser(designatedApproverForNode);
        if (approverNode == null) {
            String designatedApproverInfo = (designatedApproverForNode != null) ?
                    String.format("%s (ID: %d, 角色: %s)", designatedApproverForNode.getUsername(), designatedApproverForNode.getId(), designatedApproverForNode.getRoles())
                    : "未指定审批人";
            String errorMsg = String.format(
                    "系统错误：无法为当前应审批用户 %s 找到对应的审批处理者配置。请假ID: %d",
                    designatedApproverInfo, leaveRequestId
            );
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {
            approverNode.handleApprovalAction(leaveRequest, actionTakingApprover, actionDto.getDecision(), actionDto.getComments());
            logger.info("请假申请 ID: {} 的审批操作已由 {} (ID: {}) 代表节点 {} (角色匹配自 {}) 处理完成。",
                    leaveRequestId, actionTakingApprover.getUsername(), actionTakingApprover.getId(),
                    approverNode.getClass().getSimpleName(),
                    designatedApproverForNode != null ? designatedApproverForNode.getUsername() : "系统/Admin");
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("请假申请 ID: {} 的审批操作在职责链环节失败：{}", leaveRequestId, e.getMessage(), e);
            throw e;
        }

        LeaveRequest updatedRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> {
                    logger.error("严重错误：处理审批后无法重新获取请假申请 {}。", leaveRequestId);
                    return new InternalError("严重错误：处理审批后无法重新获取请假申请 " + leaveRequestId + "。");
                });

        return populateLeaveRequestViewDto(updatedRequest);
    }

    private Approver getApproverNodeForUser(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            logger.warn("尝试为没有角色或为null的用户获取Approver节点：{}", user != null ? user.getUsername() : "null用户");
            return null;
        }
        if (user.getRoles().contains(Role.ROLE_HR)) {
            return applicationContext.getBean("hrApprover", Approver.class);
        } else if (user.getRoles().contains(Role.ROLE_DEPT_MANAGER)) {
            return applicationContext.getBean("deptManagerApprover", Approver.class);
        } else if (user.getRoles().contains(Role.ROLE_TEAM_LEAD)) {
            return applicationContext.getBean("teamLeadApprover", Approver.class);
        }
        if (user.getRoles().size() == 1 && user.getRoles().contains(Role.ROLE_ADMIN)) {
            logger.info("为纯Admin角色 {} 指定HR审批节点作为入口。", user.getUsername());
            return applicationContext.getBean("hrApprover", Approver.class);
        }
        logger.warn("用户 {} (ID: {}) 具有角色 {}，但没有匹配的特定审批处理者节点配置用于启动审批链。如果操作者是Admin，其权限仍将在审批链内部处理。",
                user.getUsername(), user.getId(), user.getRoles());
        return null;
    }

    @Override
    public LeaveRequestViewDto cancelLeaveRequest(Long leaveRequestId, Long applicantId) {
        logger.info("用户ID {} 尝试取消请假申请ID {}", applicantId, leaveRequestId);
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));
        User actionTaker = userRepository.findById(applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("User (Action Taker)", "id", applicantId));

        if (!leaveRequest.getApplicant().getId().equals(actionTaker.getId())) {
            String errorMsg = String.format("权限不足：用户 %s 不是请假申请 %d 的申请人。",
                    actionTaker.getUsername(), leaveRequestId);
            logger.warn(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {
            leaveRequest.cancel(actionTaker);
            LeaveRequest cancelledRequest = leaveRequestRepository.save(leaveRequest);
            logger.info("请假申请 ID: {} 已被申请人 {} 成功取消，新状态: {}",
                    cancelledRequest.getId(), actionTaker.getUsername(), cancelledRequest.getStatusEnum());
            return populateLeaveRequestViewDto(cancelledRequest);
        } catch (IllegalStateException e) {
            logger.warn("取消操作失败 (ID: {}，操作人: {}): {}", leaveRequestId, actionTaker.getUsername(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LeaveRequestViewDto> getLeaveRequestDetailsById(Long leaveRequestId) {
        logger.debug("查询请假申请详情，ID: {}", leaveRequestId);
        return leaveRequestRepository.findById(leaveRequestId)
                .map(this::populateLeaveRequestViewDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestViewDto> getMyLeaveRequests(Long applicantId, Pageable pageable) {
        logger.debug("用户ID {} 查询我的请假申请，分页：{}", applicantId, pageable);
        // userRepository.findById(applicantId) // 这行不是必须的，除非你要校验用户存在
        //         .orElseThrow(() -> new ResourceNotFoundException("User (Applicant)", "id", applicantId));
        return leaveRequestRepository.findByApplicantId(applicantId, pageable)
                .map(this::populateLeaveRequestViewDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestViewDto> getPendingApprovalRequestsForUser(Long approverId, LeaveStatus status, Pageable pageable) {
        LeaveStatus queryStatus = (status == null) ? LeaveStatus.PENDING_APPROVAL : status;
        logger.debug("审批人ID {} 查询状态为 {} 的请假申请列表，分页：{}", approverId, queryStatus, pageable);
        // userRepository.findById(approverId) // 这行不是必须的，除非你要校验用户存在
        //        .orElseThrow(() -> new ResourceNotFoundException("User (Approver)", "id", approverId));
        return leaveRequestRepository.findByCurrentApproverIdAndStatusEnum(approverId, queryStatus, pageable)
                .map(this::populateLeaveRequestViewDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestViewDto> adminGetAllPendingRequests(LeaveStatus status, Pageable pageable) {
        LeaveStatus queryStatus = (status == null) ? LeaveStatus.PENDING_APPROVAL : status;
        logger.info("Admin 操作：获取所有状态为 {} 的请假申请，分页：{}", queryStatus, pageable);
        // 更正点：使用 Repository 中定义的 findByStatusEnum
        return leaveRequestRepository.findByStatusEnum(queryStatus, pageable)
                .map(this::populateLeaveRequestViewDto);
    }

    private LeaveRequestViewDto populateLeaveRequestViewDto(LeaveRequest leaveRequest) {
        if (leaveRequest == null) return null;
        LeaveRequestViewDto dto = LeaveRequestViewDto.fromEntity(leaveRequest);
        if (dto != null) {
            // 更正点：使用 Repository 中定义的 findByLeaveRequestOrderByApprovedAtAsc
            List<ApprovalHistory> histories = approvalHistoryRepository.findByLeaveRequestOrderByApprovedAtAsc(leaveRequest);
            dto.setApprovalHistory(
                    histories.stream()
                            .map(ApprovalHistoryViewDto::fromEntity)
                            .collect(Collectors.toList())
            );
            logger.trace("为请假申请ID {} 填充了 {} 条审批历史记录。", leaveRequest.getId(), histories.size());
        }
        return dto;
    }
}