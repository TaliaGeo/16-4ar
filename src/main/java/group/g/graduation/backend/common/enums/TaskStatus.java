package group.g.graduation.backend.common.enums;

/**
 * Status of a task - حالة المهمة
 */
public enum TaskStatus {
    PENDING,    // قيد الانتظار
    DUE_TODAY,  // مستحق اليوم
    OVERDUE,    // متأخر
    COMPLETED,  // تم إنجازها
    SNOOZED     // تأجيل/تذكير لاحقاً
}
