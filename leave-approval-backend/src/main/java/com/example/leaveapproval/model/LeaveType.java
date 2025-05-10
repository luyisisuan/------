package com.example.leaveapproval.model;

/**
 * 定义了系统中支持的请假类型。
 */
public enum LeaveType {
    ANNUAL_LEAVE,      // 年假
    SICK_LEAVE,        // 病假
    PERSONAL_LEAVE,    // 事假
    MATERNITY_LEAVE,   // 产假
    PATERNITY_LEAVE,   // 陪产假
    BEREAVEMENT_LEAVE, // 丧假
    UNPAID_LEAVE,      // 无薪假
    OTHER              // 其他 (可能需要附加说明)
}