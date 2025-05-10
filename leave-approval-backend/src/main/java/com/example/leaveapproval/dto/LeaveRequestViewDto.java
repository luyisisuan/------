package com.example.leaveapproval.dto;

import com.example.leaveapproval.model.LeaveRequest; // 用于转换
import com.example.leaveapproval.model.LeaveStatus;
import com.example.leaveapproval.model.LeaveType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List; // 用于审批历史

@Data
@NoArgsConstructor
public class LeaveRequestViewDto {

    private Long id;
    private UserDto applicant; // 申请人信息 (复用之前创建的 UserDto)
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LeaveStatus status;
    private UserDto currentApprover; // 当前审批人信息 (可以为null)
    private String attachmentsPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long leaveDurationInDays; // 请假天数

    private List<ApprovalHistoryViewDto> approvalHistory; // 审批历史记录

    /**
     * 从 LeaveRequest 实体转换为 LeaveRequestViewDto。
     * @param leaveRequest 实体对象。
     * @return DTO 对象。
     */
    public static LeaveRequestViewDto fromEntity(com.example.leaveapproval.model.LeaveRequest leaveRequest) {
        if (leaveRequest == null) {
            return null;
        }
        LeaveRequestViewDto dto = new LeaveRequestViewDto();
        dto.setId(leaveRequest.getId());
        if (leaveRequest.getApplicant() != null) {
            dto.setApplicant(UserDto.fromEntity(leaveRequest.getApplicant()));
        }
        dto.setLeaveType(leaveRequest.getLeaveType());
        dto.setStartDate(leaveRequest.getStartDate());
        dto.setEndDate(leaveRequest.getEndDate());
        dto.setReason(leaveRequest.getReason());
        dto.setStatus(leaveRequest.getStatusEnum()); // <<--- 修改这里
        if (leaveRequest.getCurrentApprover() != null) {
            dto.setCurrentApprover(UserDto.fromEntity(leaveRequest.getCurrentApprover()));
        }
        dto.setAttachmentsPath(leaveRequest.getAttachmentsPath());
        dto.setCreatedAt(leaveRequest.getCreatedAt());
        dto.setUpdatedAt(leaveRequest.getUpdatedAt());

        if (leaveRequest.getStartDate() != null && leaveRequest.getEndDate() != null) {
            dto.setLeaveDurationInDays(java.time.temporal.ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1);
        } else {
            dto.setLeaveDurationInDays(0L);
        }
        // approvalHistory 的填充在 Service 层
        return dto;
    }
}