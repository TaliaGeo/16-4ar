package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.model.AuditLog.AuditAction;
import group.g.graduation.backend.common.model.AuditLog.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Audit Log Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private AuditAction action;
    private String actionAr;  // الترجمة العربية للعملية
    private String entityType;
    private String entityTypeAr;  // الترجمة العربية للكيان
    private Long entityId;
    private String description;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String endpoint;
    private String httpMethod;
    private AuditStatus status;
    private String statusAr;
    private String errorMessage;
    private Long executionTime;
    private Instant createdAt;
    
    // Helper method للترجمة العربية للعملية
    public static String getActionArabic(AuditAction action) {
        if (action == null) return null;
        return switch (action) {
            case CREATE -> "إنشاء";
            case READ -> "قراءة";
            case UPDATE -> "تحديث";
            case DELETE -> "حذف";
            case BULK_CREATE -> "إنشاء متعدد";
            case BULK_UPDATE -> "تحديث متعدد";
            case BULK_DELETE -> "حذف متعدد";
            case LOGIN -> "تسجيل دخول";
            case LOGOUT -> "تسجيل خروج";
            case PASSWORD_CHANGE -> "تغيير كلمة المرور";
            case ACTIVATE -> "تفعيل";
            case DEACTIVATE -> "تعطيل";
            case ASSIGN_ROLE -> "إسناد دور";
            case REMOVE_ROLE -> "إزالة دور";
            case UPLOAD -> "رفع ملف";
            case DOWNLOAD -> "تحميل ملف";
            case BROADCAST -> "بث إشعار";
            case SEND_NOTIFICATION -> "إرسال إشعار";
            case EXPORT -> "تصدير";
            case IMPORT -> "استيراد";
        };
    }
    
    // Helper method للترجمة العربية للكيان
    public static String getEntityTypeArabic(String entityType) {
        if (entityType == null) return null;
        return switch (entityType.toUpperCase()) {
            case "PLANT" -> "نبتة";
            case "USER" -> "مستخدم";
            case "QUOTE" -> "اقتباس";
            case "MONTH" -> "شهر";
            case "TASKTYPE" -> "نوع مهمة";
            case "PLANTTASK" -> "مهمة نبتة";
            case "PLANTINGQUESTION" -> "سؤال زراعي";
            case "QUESTIONOPTION" -> "خيار سؤال";
            case "PLANTSUITABILITY" -> "ملاءمة نبتة";
            case "NOTIFICATION" -> "إشعار";
            case "MONTHPLANT" -> "نبتة-شهر";
            case "PLANTIMAGE" -> "صورة نبتة";
            case "ROLE" -> "دور";
            case "PERMISSION" -> "صلاحية";
            default -> entityType;
        };
    }
    
    // Helper method للترجمة العربية للحالة
    public static String getStatusArabic(AuditStatus status) {
        if (status == null) return null;
        return switch (status) {
            case SUCCESS -> "نجاح";
            case FAILURE -> "فشل";
            case PARTIAL -> "جزئي";
        };
    }
}
