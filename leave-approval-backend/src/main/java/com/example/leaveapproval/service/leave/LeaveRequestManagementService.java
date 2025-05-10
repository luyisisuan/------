package com.example.leaveapproval.service.leave;

import com.example.leaveapproval.dto.ApprovalActionDto;
import com.example.leaveapproval.dto.LeaveRequestCreateDto;
import com.example.leaveapproval.dto.LeaveRequestViewDto;
import com.example.leaveapproval.model.LeaveStatus; // 确保导入
import com.example.leaveapproval.model.User; // 确保导入 (如果方法参数需要)
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 请假管理核心服务接口。
 * 封装了员工提交请假、审批人处理请假以及查询请假信息的业务逻辑。
 */
public interface LeaveRequestManagementService {

    /**
     * 员工提交新的请假申请。
     * @param createDto 包含请假申请信息的DTO。
     * @return 创建并启动审批流程后的请假申请视图DTO。
     */
    LeaveRequestViewDto submitLeaveRequest(LeaveRequestCreateDto createDto);

    /**
     * 审批人处理请假申请（批准或驳回）。
     * @param leaveRequestId 要处理的请假申请ID。
     * @param actionDto      包含审批决定和意见的DTO。
     * @param approverUserId 执行审批操作的审批人的ID。
     * @return 处理完成后的请假申请视图DTO。
     */
    LeaveRequestViewDto processApprovalAction(Long leaveRequestId, ApprovalActionDto actionDto, Long approverUserId);

    /**
     * 员工取消自己提交的请假申请。
     * @param leaveRequestId 要取消的请假申请ID。
     * @param applicantId    执行取消操作的申请人的ID。
     * @return 更新状态后的请假申请视图DTO。
     */
    LeaveRequestViewDto cancelLeaveRequest(Long leaveRequestId, Long applicantId);


    /**
     * 根据ID获取请假申请的详细信息。
     * @param leaveRequestId 请假申请ID。
     * @return 包含请假详情的DTO的Optional对象。
     */
    Optional<LeaveRequestViewDto> getLeaveRequestDetailsById(Long leaveRequestId);


    /**
     * 获取指定用户提交的所有请假申请（分页）。
     * @param applicantId 申请人的用户ID。
     * @param pageable    分页参数对象。
     * @return 请假申请视图DTO的分页列表。
     */
    Page<LeaveRequestViewDto> getMyLeaveRequests(Long applicantId, Pageable pageable);

    /**
     * 获取分配给指定审批人的特定状态的请假申请列表（分页）。
     * @param approverId 指定审批人的用户ID。
     * @param status     要查询的请假状态。如果为null，则通常查询 PENDING_APPROVAL。
     * @param pageable   分页参数对象。
     * @return 符合条件的请假申请视图DTO的分页列表。
     */
    Page<LeaveRequestViewDto> getPendingApprovalRequestsForUser(Long approverId, LeaveStatus status, Pageable pageable);

    /**
     * 【新增方法】管理员获取所有处于特定状态（默认为 PENDING_APPROVAL）的请假申请（分页）。
     * 此方法不限制审批人。
     * @param status 要查询的请假状态。如果为null，则查询 PENDING_APPROVAL。
     * @param pageable 分页参数对象。
     * @return 符合条件的请假申请视图DTO的分页列表。
     */
    Page<LeaveRequestViewDto> adminGetAllPendingRequests(LeaveStatus status, Pageable pageable); // <<--- 新增此方法

}