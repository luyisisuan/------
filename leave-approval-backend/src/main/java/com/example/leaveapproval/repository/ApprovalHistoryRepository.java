package com.example.leaveapproval.repository;

import com.example.leaveapproval.model.ApprovalHistory;
import com.example.leaveapproval.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {

    /**
     * 根据请假申请查找其所有的审批历史记录。
     * 通常按审批时间排序。
     * @param leaveRequest 关联的请假申请。
     * @return 该请假申请的审批历史列表。
     */
    List<ApprovalHistory> findByLeaveRequestOrderByApprovedAtAsc(LeaveRequest leaveRequest);

    /**
     * 根据请假申请ID查找其所有的审批历史记录。
     * @param requestId 请假申请ID。
     * @return 该请假申请的审批历史列表。
     */
    List<ApprovalHistory> findByLeaveRequestIdOrderByApprovedAtAsc(Long requestId);
}