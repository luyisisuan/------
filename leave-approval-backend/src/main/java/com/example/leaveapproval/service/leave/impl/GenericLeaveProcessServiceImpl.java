package com.example.leaveapproval.service.leave.impl;

import com.example.leaveapproval.dto.LeaveRequestCreateDto;
import com.example.leaveapproval.exception.ResourceNotFoundException; // 确保导入
import com.example.leaveapproval.model.LeaveRequest;
import com.example.leaveapproval.model.LeaveStatus;
import com.example.leaveapproval.model.User;
import com.example.leaveapproval.repository.LeaveRequestRepository;
import com.example.leaveapproval.repository.UserRepository;
import com.example.leaveapproval.service.approval.chain.ApprovalChainBuilder;
import com.example.leaveapproval.service.leave.LeaveRequestProcessService; // 导入父类
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; // 导入 Authentication
import org.springframework.security.core.context.SecurityContextHolder; // 导入 SecurityContextHolder
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 子类现在负责事务的边界

import java.time.temporal.ChronoUnit;

/**
 * 通用请假类型的处理服务实现。
 * 继承自 {@link LeaveRequestProcessService} 抽象模板类，并实现其中的抽象方法。
 * 此类现在直接负责注入和使用 Repositories。
 */
@Service("genericLeaveProcessService")
// @Transactional // 事务注解现在放在父类的 final 模板方法上，子类的方法会自动参与
public class GenericLeaveProcessServiceImpl extends LeaveRequestProcessService {

    private static final Logger logger = LoggerFactory.getLogger(GenericLeaveProcessServiceImpl.class);

    // 子类直接注入并持有 Repositories
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final ApprovalChainBuilder approvalChainBuilder;

    /**
     * 通过构造函数注入所有依赖。
     * 父类不再接收 Repositories，子类自己管理。
     * @param leaveRequestRepository 请假申请数据仓库。
     * @param userRepository 用户数据仓库。
     * @param approvalChainBuilder 审批链构建器。
     */
    @Autowired
    public GenericLeaveProcessServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            UserRepository userRepository,
            ApprovalChainBuilder approvalChainBuilder) {
        // super(); // 父类现在没有需要调用的带参构造函数了，可以省略或调用隐式的super()
        this.leaveRequestRepository = leaveRequestRepository;
        this.userRepository = userRepository;
        this.approvalChainBuilder = approvalChainBuilder;
        logger.debug("GenericLeaveProcessServiceImpl CONSTRUCTOR: All dependencies injected.");
    }

    @PostConstruct
    public void checkAllDependenciesAfterConstruction() {
        logger.info("GenericLeaveProcessServiceImpl @PostConstruct: Checking dependencies...");
        if (this.leaveRequestRepository == null) {
            logger.error("CRITICAL! GenericLeaveProcessServiceImpl @PostConstruct: leaveRequestRepository is NULL!");
        } else {
            logger.info("GenericLeaveProcessServiceImpl @PostConstruct: leaveRequestRepository is NOT NULL.");
        }
        if (this.userRepository == null) {
            logger.error("CRITICAL! GenericLeaveProcessServiceImpl @PostConstruct: userRepository is NULL!");
        } else {
            logger.info("GenericLeaveProcessServiceImpl @PostConstruct: userRepository is NOT NULL.");
        }
        if (this.approvalChainBuilder == null) {
            logger.error("CRITICAL! GenericLeaveProcessServiceImpl @PostConstruct: approvalChainBuilder is NULL!");
        } else {
            logger.info("GenericLeaveProcessServiceImpl @PostConstruct: approvalChainBuilder is NOT NULL.");
        }
    }

    // --- 实现父类的抽象方法 ---

    @Override
    protected User getCurrentApplicant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            throw new IllegalStateException("User not authenticated or authentication principal is not a User instance for getCurrentApplicant.");
        }
        String username = ((User) authentication.getPrincipal()).getUsername();
        // 使用本类注入的 userRepository
        if (this.userRepository == null) {
            throw new IllegalStateException("UserRepository is null in getCurrentApplicant (GenericLeaveProcessServiceImpl).");
        }
        return this.userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    @Override
    protected LeaveRequest createAndSaveInitialLeaveRequestEntity(LeaveRequestCreateDto createDto, User applicant) {
        if (createDto.getEndDate().isBefore(createDto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        LeaveRequest lr = new LeaveRequest();
        lr.setApplicant(applicant);
        lr.setLeaveType(createDto.getLeaveType());
        lr.setStartDate(createDto.getStartDate());
        lr.setEndDate(createDto.getEndDate());
        lr.setReason(createDto.getReason());
        lr.setStatusEnumAndUpdateState(LeaveStatus.PENDING_APPROVAL);

        // 使用本类注入的 leaveRequestRepository
        if (this.leaveRequestRepository == null) {
            throw new IllegalStateException("LeaveRequestRepository is null in createAndSaveInitialLeaveRequestEntity (GenericLeaveProcessServiceImpl).");
        }
        return this.leaveRequestRepository.save(lr);
    }

    @Override
    protected void validateSpecificRules(LeaveRequestCreateDto createDto, User applicant) {
        logger.info("Executing specific rule validation for generic leave. Applicant: {}, Type: {}",
                applicant.getUsername(), createDto.getLeaveType());
        long leaveDays = ChronoUnit.DAYS.between(createDto.getStartDate(), createDto.getEndDate()) + 1;
        if (leaveDays <= 0) {
            throw new IllegalArgumentException("Leave duration must be greater than 0 days.");
        }
        logger.debug("Calculated leave duration: {} days.", leaveDays);
        logger.info("Specific rules validation passed for generic leave. Applicant: {}", applicant.getUsername());
    }

    @Override
    protected void startApprovalWorkflow(LeaveRequest leaveRequest, User applicant) {
        logger.info("Starting approval workflow for generic leave. Leave ID: {}, Applicant: {}",
                leaveRequest.getId(), applicant.getUsername());
        User initialApproverUser = approvalChainBuilder.getInitialApproverUser(leaveRequest, applicant);
        if (initialApproverUser != null) {
            leaveRequest.setCurrentApprover(initialApproverUser);
            // 状态 PENDING_APPROVAL 已在 createAndSaveInitialLeaveRequestEntity 中设置
            logger.info("Leave request (ID: {}, Type: {}) assigned to initial approver: {} (ID: {}). Current status: {}",
                    leaveRequest.getId(), leaveRequest.getLeaveType(),
                    initialApproverUser.getUsername(), initialApproverUser.getId(),
                    leaveRequest.getStatusEnum());
        } else {
            String errorMessage = String.format(
                    "Could not determine initial approver for leave request (ID: %d, Type: %s, Applicant: %s). ",
                    leaveRequest.getId(), leaveRequest.getLeaveType(), applicant.getUsername()
            );
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    @Override
    protected LeaveRequest saveLeaveRequest(LeaveRequest leaveRequest) {
        logger.debug("Saving leave request (ID: {}) from GenericLeaveProcessServiceImpl after workflow start.", leaveRequest.getId());
        // 使用本类注入的 leaveRequestRepository
        if (this.leaveRequestRepository == null) {
            throw new IllegalStateException("LeaveRequestRepository is null in saveLeaveRequest (GenericLeaveProcessServiceImpl).");
        }
        return this.leaveRequestRepository.save(leaveRequest);
    }

    @Override
    protected void performPostSubmissionActions(LeaveRequest leaveRequest) {
        logger.info("Executing post-submission actions for generic leave. Leave ID: {}", leaveRequest.getId());
        // 通知逻辑等
    }
}