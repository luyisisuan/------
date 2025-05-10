package com.example.leaveapproval.repository;

import com.example.leaveapproval.model.LeaveRequest;
import com.example.leaveapproval.model.LeaveStatus;
import com.example.leaveapproval.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

// 移除了 java.util.List 的导入，因为 findByStatusEnum 现在返回 Page

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    Page<LeaveRequest> findByApplicant(User applicant, Pageable pageable);

    Page<LeaveRequest> findByApplicantId(Long applicantId, Pageable pageable);

    /**
     * 根据当前审批人和状态枚举查找请假申请（分页）。
     * @param currentApprover 当前审批人。
     * @param statusEnum 请假状态枚举。
     * @param pageable 分页信息。
     * @return 请假申请的分页列表。
     */
    Page<LeaveRequest> findByCurrentApproverAndStatusEnum(User currentApprover, LeaveStatus statusEnum, Pageable pageable);

    /**
     * 根据当前审批人ID和状态枚举查找请假申请（分页）。
     * @param currentApproverId 当前审批人ID。
     * @param statusEnum 请假状态枚举。
     * @param pageable 分页信息。
     * @return 请假申请的分页列表。
     */
    Page<LeaveRequest> findByCurrentApproverIdAndStatusEnum(Long currentApproverId, LeaveStatus statusEnum, Pageable pageable);

    /**
     * 根据状态枚举分页查找请假单。
     * @param statusEnum 请假状态枚举。
     * @param pageable 分页信息。
     * @return 符合条件的请假申请的分页列表。
     */
    Page<LeaveRequest> findByStatusEnum(LeaveStatus statusEnum, Pageable pageable); // <<--- 修改了返回值和参数
}