package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for creating/updating notifications
 * طلب إنشاء/تعديل إشعار
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    private String titleAr;        // عنوان الإشعار - عربي
    private String titleEn;        // عنوان الإشعار - إنجليزي
    private String messageAr;      // رسالة الإشعار - عربي
    private String messageEn;      // رسالة الإشعار - إنجليزي
    private NotificationType type; // نوع الإشعار
    private Instant scheduledAt;   // وقت الإرسال المجدول (اختياري)
}
