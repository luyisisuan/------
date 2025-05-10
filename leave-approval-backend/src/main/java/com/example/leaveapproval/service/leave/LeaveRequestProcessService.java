package com.example.leaveapproval.service.leave;

import com.example.leaveapproval.dto.LeaveRequestCreateDto;
import com.example.leaveapproval.dto.LeaveRequestViewDto;
// ResourceNotFoundException 可能在子类实现 getCurrentApplicant 时用到，所以保留导入
import com.example.leaveapproval.exception.ResourceNotFoundException;
import com.example.leaveapproval.model.LeaveRequest;
// LeaveStatus 和 User 可能在子类实现的方法签名中用到，或者在父类模板方法中用到
import com.example.leaveapproval.model.LeaveStatus;
import com.example.leaveapproval.model.User;
// Repository 导入不再需要，因为父类不再直接持有它们
// import com.example.leaveapproval.repository.LeaveRequestRepository;
// import com.example.leaveapproval.repository.UserRepository;
import jakarta.annotation.PostConstruct; // 暂时保留，看是否还需要
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired; // 父类构造函数不再需要
import org.springframework.transaction.annotation.Transactional;
// import org.springframework.util.Assert; // 父类构造函数不再需要

/**
 * 抽象的请假申请处理流程服务 (模板方法模式)。
 * 父类不再直接持有 Repository 依赖，这些依赖由具体子类管理和使用。
 */
public abstract class LeaveRequestProcessService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveRequestProcessService.class);

    // 父类不再有自己的构造函数来接收 Repositories
    // @PostConstruct 也可以移除，因为没有字段需要在这里检查了
    // public LeaveRequestProcessService() {
    //     logger.info("LeaveRequestProcessService default constructor (if Spring needs it for proxying).");
    // }

    @PostConstruct
    public void postConstructLog() {
        logger.info("LeaveRequestProcessService @PostConstruct: instance created (class: {}).", this.getClass().getName());
    }


    @Transactional // 保持事务注解在模板方法上
    public final LeaveRequestViewDto submitLeaveRequest(LeaveRequestCreateDto createDto) {
        logger.info("Processing new leave request of type: {}", createDto.getLeaveType());

        User applicant = getCurrentApplicant(); // 调用子类实现的获取申请人方法
        logger.debug("Applicant identified: {}", applicant.getUsername());

        validateSpecificRules(createDto, applicant); // 调用子类实现的校验方法
        logger.debug("Specific rules validation passed.");

        LeaveRequest leaveRequest = createAndSaveInitialLeaveRequestEntity(createDto, applicant); // 调用子类实现的创建和保存方法
        logger.info("Leave request entity created and saved with ID: {}, initial status: {}", leaveRequest.getId(), leaveRequest.getStatusEnum());

        startApprovalWorkflow(leaveRequest, applicant); // 调用子类实现的启动工作流方法
        logger.debug("Approval workflow started. Current approver (if set): {}",
                leaveRequest.getCurrentApprover() != null ? leaveRequest.getCurrentApprover().getUsername() : "None (or process ended)");

        // 保存因 startApprovalWorkflow 可能导致的 currentApprover 或 status 变更
        LeaveRequest updatedRequestAfterWorkflow = saveLeaveRequest(leaveRequest); // 调用子类实现的保存方法
        logger.info("Leave request (ID: {}) saved after workflow start. Status: {}, Approver ID: {}",
                updatedRequestAfterWorkflow.getId(),
                updatedRequestAfterWorkflow.getStatusEnum(),
                updatedRequestAfterWorkflow.getCurrentApprover() != null ? updatedRequestAfterWorkflow.getCurrentApprover().getId() : "N/A");

        performPostSubmissionActions(updatedRequestAfterWorkflow); // 调用钩子方法
        logger.debug("Post-submission actions completed for leave request ID: {}.", updatedRequestAfterWorkflow.getId());

        LeaveRequestViewDto viewDto = LeaveRequestViewDto.fromEntity(updatedRequestAfterWorkflow);
        logger.info("Leave request processing complete. Returning DTO for ID: {}.", updatedRequestAfterWorkflow.getId());
        return viewDto;
    }

    // --- 抽象方法 (由子类实现) ---

    /**
     * 抽象方法：获取当前登录的申请人。
     * @return 当前认证的 User 对象。
     */
    protected abstract User getCurrentApplicant();

    /**
     * 抽象方法：创建并保存请假申请实体，并设置初始状态为 PENDING_APPROVAL。
     * @param createDto 包含请假信息的DTO。
     * @param applicant 申请人。
     * @return 已保存的 LeaveRequest 实体。
     */
    protected abstract LeaveRequest createAndSaveInitialLeaveRequestEntity(LeaveRequestCreateDto createDto, User applicant);

    /**
     * 抽象方法：校验特定于请假类型的规则。
     * @param createDto 包含请假信息的DTO。
     * @param applicant 申请人。
     */
    protected abstract void validateSpecificRules(LeaveRequestCreateDto createDto, User applicant);

    /**
     * 抽象方法：启动审批工作流。
     * 主要职责是找到并设置初始审批人 (`leaveRequest.setCurrentApprover()`)。
     * @param leaveRequest 已创建并保存了初始状态的请假申请实体。
     * @param applicant 申请人。
     */
    protected abstract void startApprovalWorkflow(LeaveRequest leaveRequest, User applicant);

    /**
     * 抽象方法：用于在工作流启动后保存 LeaveRequest 的最终状态。
     * @param leaveRequest 需要保存的 LeaveRequest 实体。
     * @return 已保存的 LeaveRequest 实体。
     */
    protected abstract LeaveRequest saveLeaveRequest(LeaveRequest leaveRequest);


    // --- 钩子方法 (子类可选择覆盖) ---

    /**
     * 钩子方法：执行请假申请提交后的操作（例如发送通知）。
     * @param leaveRequest 已完成提交流程并保存的请假申请实体。
     */
    protected void performPostSubmissionActions(LeaveRequest leaveRequest) {
        logger.debug("Executing default post-submission actions (none) for leave ID: {}", leaveRequest.getId());
    }
}