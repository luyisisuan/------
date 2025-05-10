package com.example.leaveapproval.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "approval_history")
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private LeaveRequest leaveRequest; // 关联的请假申请

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver; // 执行审批操作的用户

    @Enumerated(EnumType.STRING) // 例如，存储 "APPROVED" 或 "REJECTED"
    @Column(nullable = false, length = 50)
    private Decision decision; // 审批决定 (可以为此创建一个简单的枚举 Decision)

    @Column(columnDefinition = "TEXT")
    private String comments; // 审批意见

    @CreationTimestamp
    @Column(name = "approved_at", updatable = false)
    private LocalDateTime approvedAt; // 审批操作时间

    /**
     * 定义审批决定的枚举。
     */
    public enum Decision {
        APPROVED,
        REJECTED
    }
}