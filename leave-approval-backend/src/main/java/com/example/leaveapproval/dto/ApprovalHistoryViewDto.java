package com.example.leaveapproval.dto;

import com.example.leaveapproval.model.ApprovalHistory; // 用于转换
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ApprovalHistoryViewDto {

    private Long id;
    private UserDto approver; // 审批人信息
    private ApprovalHistory.Decision decision; // 审批决定 (APPROVED, REJECTED)
    private String comments;
    private LocalDateTime approvedAt;

    public static ApprovalHistoryViewDto fromEntity(ApprovalHistory approvalHistory) {
        if (approvalHistory == null) {
            return null;
        }
        ApprovalHistoryViewDto dto = new ApprovalHistoryViewDto();
        dto.setId(approvalHistory.getId());
        if (approvalHistory.getApprover() != null) {
            dto.setApprover(UserDto.fromEntity(approvalHistory.getApprover()));
        }
        dto.setDecision(approvalHistory.getDecision());
        dto.setComments(approvalHistory.getComments());
        dto.setApprovedAt(approvalHistory.getApprovedAt());
        return dto;
    }
}