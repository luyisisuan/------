package com.example.leaveapproval.model.state;

import com.example.leaveapproval.model.ApprovalHistory;
import com.example.leaveapproval.model.LeaveRequest;
import com.example.leaveapproval.model.LeaveStatus;
import com.example.leaveapproval.model.User;
import com.example.leaveapproval.service.approval.chain.Approver; // 需要注入或获取Approver
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// 注意：具体状态类通常不直接是Spring Bean，除非它们有复杂的依赖需要注入。
// 通常，状态转换的逻辑（包括调用职责链）会放在 Service 层或由 Context（LeaveRequest）委托给 Service。
// 为了简化，我们这里假设某些必要操作可以被传递进来或者通过 Context 的 Service 引用来完成。

public class PendingApprovalState implements LeaveState {
    private static final Logger logger = LoggerFactory.getLogger(PendingApprovalState.class);
    private static final PendingApprovalState INSTANCE = new PendingApprovalState(); // 单例

    private PendingApprovalState() {}

    public static PendingApprovalState getInstance() {
        return INSTANCE;
    }

    @Override
    public void approve(LeaveRequest context, User actionTakingUser, ApprovalHistory.Decision decision, String comments) {
        logger.info("请假申请 ID: {} 在 PENDING_APPROVAL 状态下被用户 ID: {} 尝试批准。", context.getId(), actionTakingUser.getId());
        // 这里的核心逻辑是调用职责链中的 Approver 实例
        // 这个 Approver 实例应该从 Service 层获取并传递，或者 Context (LeaveRequest) 持有对相关服务的引用
        // 为了演示状态模式，我们假设这个操作会由 Service 层协调，这里仅改变状态（简化版）
        // 实际中，职责链的 Approver.handleApprovalAction 会负责改变状态和 currentApprover

        // **重要**：这里的逻辑需要与 LeaveRequestManagementServiceImpl 中的 processApprovalAction 协调。
        // 在实际应用中，processApprovalAction 会找到正确的 Approver Bean，并调用其 handleApprovalAction。
        // Approver 的 handleApprovalAction 内部会调用其 performApprove 方法，
        // performApprove 方法会调用 determineNextApproverUser 并设置新的状态和 currentApprover。
        // 所以，这个 approve 方法更多的是一个“允许批准”的信号，实际的状态变更和审批人更新
        // 由职责链中的 Approver 完成。
        // 为了状态模式的纯粹性，这里可以只抛出异常如果不能批准，实际改变状态由外部调用者（Service层）完成。
        // 或者，状态对象持有对职责链中对应审批者的引用。这会使状态对象变重。

        // 简化：我们假设如果到这里，说明权限已校验，职责链会处理状态变更。
        // 这里仅示意性地打印日志，实际状态变更由职责链的 Approver.performApprove() 完成。
        // 理论上，当外部调用 context.approve() 时，这个方法会被调用。
        // 但由于我们的审批逻辑在职责链中，职责链的 handleApprovalAction 更适合直接更新 context 的状态。
        // 所以，此处的 approve 方法可能更多的是一个校验点，或者如果状态模式完全控制流程，则它会调用职责链。

        // 为了课程设计演示：可以认为，Service层在调用职责链前，先调用当前状态的approve方法进行前置检查。
        // 如果检查通过，再调用职责链。职责链处理完后，Service层再调用context.setState()。
        // 或者，职责链内部在批准后，直接调用 context.setState(ApprovedState.getInstance()) 或类似。

        // 此处不直接改变状态，状态的改变由职责链中的 Approver.performApprove() 方法负责。
        // 这个方法被调用，表明“当前状态允许进行批准这个动作”。
        logger.debug("PendingApprovalState: 允许批准操作。实际状态变更由职责链处理。");
    }

    @Override
    public void reject(LeaveRequest context, User actionTakingUser, ApprovalHistory.Decision decision, String comments) {
        logger.info("请假申请 ID: {} 在 PENDING_APPROVAL 状态下被用户 ID: {} 尝试驳回。", context.getId(), actionTakingUser.getId());
        // 类似 approve，实际状态变更由职责链的 Approver.performReject() 方法负责。
        logger.debug("PendingApprovalState: 允许驳回操作。实际状态变更由职责链处理。");
    }

    @Override
    public void cancel(LeaveRequest context, User actionTakingUser) {
        logger.info("请假申请 ID: {} 在 PENDING_APPROVAL 状态下被用户 ID: {} 尝试取消。", context.getId(), actionTakingUser.getId());
        if (!context.getApplicant().getId().equals(actionTakingUser.getId())) {
            throw new IllegalStateException("只有申请人才能取消请假申请。");
        }
        context.setStatusEnumAndUpdateState(LeaveStatus.CANCELLED); // 直接转换到 CancelledState
        context.setCurrentApprover(null);
        logger.info("请假申请 ID: {} 已被成功取消。", context.getId());
    }

    @Override
    public LeaveStatus getStatusEnum() {
        return LeaveStatus.PENDING_APPROVAL;
    }
}