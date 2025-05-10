package com.example.leaveapproval.dto;

import com.example.leaveapproval.model.LeaveType; // 确保导入你的枚举
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class LeaveRequestCreateDto {

    @NotNull(message = "请假类型不能为空")
    private LeaveType leaveType;

    @NotNull(message = "请假开始日期不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // 确保日期格式正确解析
    @FutureOrPresent(message = "请假开始日期不能早于当前日期")
    private LocalDate startDate;

    @NotNull(message = "请假结束日期不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @FutureOrPresent(message = "请假结束日期不能早于当前日期")
    // 可以添加一个自定义校验器来确保 endDate >= startDate
    private LocalDate endDate;

    @NotBlank(message = "请假理由不能为空")
    @Size(min = 5, max = 500, message = "请假理由长度必须在 {min} 到 {max} 字符之间")
    private String reason;

    // 附件上传是可选的，通常会单独处理或传递文件标识符
    // private String attachmentId;
}