package com.example.leaveapproval.dto;

import com.example.leaveapproval.model.ApprovalHistory; // 确保导入 Decision 枚举
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApprovalActionDto {

    @NotNull(message = "审批决定不能为空")
    private ApprovalHistory.Decision decision; // APPROVED or REJECTED

    @Size(max = 500, message = "审批意见长度不能超过 {max} 字符")
    private String comments; // 审批意见 (可以为空，如果只是简单批准)
}