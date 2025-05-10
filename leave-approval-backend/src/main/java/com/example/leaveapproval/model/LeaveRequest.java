package com.example.leaveapproval.model;

import com.example.leaveapproval.model.state.*; // 导入所有状态类
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    private static final Logger logger = LoggerFactory.getLogger(LeaveRequest.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 50)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    // 持久化的状态枚举，代表数据库中存储的实际状态
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private LeaveStatus statusEnum;

    // @Transient 表示此字段不直接映射到数据库列
    // currentState 是当前状态的行为对象
    @Transient
    private LeaveState currentState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approver_id")
    private User currentApprover;

    @Column(name = "attachments_path", length = 255)
    private String attachmentsPath;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * JPA回调方法：在实体从数据库加载完成后，根据持久化的 statusEnum 初始化 currentState。
     */
    @PostLoad
    private void initializeCurrentStateAfterLoad() {
        if (this.statusEnum != null) {
            this.currentState = mapStatusEnumToStateObject(this.statusEnum);
        } else {
            // 如果 statusEnum 为 null（理论上不应该发生，因为status字段是nullable=false），
            // 可以设置一个默认状态或记录错误。
            logger.warn("LeaveRequest ID {} 从数据库加载时 statusEnum 为 null，尝试设置为 PENDING_APPROVAL。", this.id);
            this.statusEnum = LeaveStatus.PENDING_APPROVAL; // 数据库层面应有默认值
            this.currentState = PendingApprovalState.getInstance();
        }
    }

    /**
     * 核心方法：设置新的状态枚举值，并同步更新 currentState 对象。
     * 所有状态变更都应通过此方法进行，以确保一致性。
     *
     * @param newStatusEnum 新的请假状态枚举。
     */
    public void setStatusEnumAndUpdateState(LeaveStatus newStatusEnum) {
        if (newStatusEnum == null) {
            throw new IllegalArgumentException("新的状态枚举值不能为空。");
        }
        this.statusEnum = newStatusEnum;
        this.currentState = mapStatusEnumToStateObject(newStatusEnum);
        logger.debug("LeaveRequest ID {} 状态已更新为: {}, 当前状态对象: {}",
                this.id, this.statusEnum, this.currentState.getClass().getSimpleName());
    }

    // 辅助方法：将 LeaveStatus 枚举映射到对应的 LeaveState 单例对象
    private LeaveState mapStatusEnumToStateObject(LeaveStatus statusEnumToMap) {
        switch (statusEnumToMap) {
            case PENDING_APPROVAL:
                return PendingApprovalState.getInstance();
            case APPROVED:
                return ApprovedState.getInstance();
            case REJECTED:
                return RejectedState.getInstance();
            case CANCELLED:
                return CancelledState.getInstance();
            // case PROCESSING: // 如果有 ProcessingState
            //     return ProcessingState.getInstance();
            default:
                logger.error("未知的请假状态枚举: {}，无法映射到状态对象。", statusEnumToMap);
                throw new IllegalStateException("未知的请假状态枚举: " + statusEnumToMap);
        }
    }

    /**
     * 获取当前的状态对象。如果尚未初始化，则根据 statusEnum 进行初始化。
     * @return 当前的 LeaveState 对象。
     */
    public LeaveState getCurrentState() {
        if (this.currentState == null) {
            if (this.statusEnum != null) {
                initializeCurrentStateAfterLoad(); // 尝试根据持久化的枚举初始化
            } else {
                // 如果是一个全新的对象，statusEnum也可能为null，此时应设为默认状态
                logger.debug("LeaveRequest ID {} 的 currentState 和 statusEnum 均为 null，初始化为 PENDING_APPROVAL。", this.id);
                setStatusEnumAndUpdateState(LeaveStatus.PENDING_APPROVAL);
            }
        }
        return this.currentState;
    }

    // --- 将操作委托给当前状态对象 ---
    // 这些方法允许外部代码以统一的方式与LeaveRequest交互，而具体行为由当前状态决定。

    /**
     * 尝试批准当前请假申请。具体行为由当前状态对象定义。
     * 注意：在我们的职责链优先设计中，此方法更多用于校验，实际状态变更由 Approver 完成。
     */
    public void approve(User actionTakingUser, ApprovalHistory.Decision decision, String comments) {
        getCurrentState().approve(this, actionTakingUser, decision, comments);
    }

    /**
     * 尝试驳回当前请假申请。具体行为由当前状态对象定义。
     */
    public void reject(User actionTakingUser, ApprovalHistory.Decision decision, String comments) {
        getCurrentState().reject(this, actionTakingUser, decision, comments);
    }

    /**
     * 尝试取消当前请假申请。具体行为由当前状态对象定义。
     * 这个方法会被 Service 层调用，例如 LeaveRequestManagementServiceImpl.cancelLeaveRequest。
     */
    public void cancel(User actionTakingUser) {
        getCurrentState().cancel(this, actionTakingUser);
    }

    // 为了方便，我们可以重命名数据库中的 status 字段，避免与 getter/setter 冲突，
    // 或者确保 Lombok 生成的 getter/setter 与 statusEnum 字段对应。
    // Lombok 会为 statusEnum 生成 getStatusEnum() 和 setStatusEnum()。
    // 我们保留 statusEnum 作为持久化字段名。
}