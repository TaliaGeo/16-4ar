package group.g.graduation.backend.user.controller;

import group.g.graduation.backend.admin.dto.NotificationResponse;
import group.g.graduation.backend.common.enums.NotificationType;
import group.g.graduation.backend.user.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * User Notification Controller - واجهة إشعارات المستخدم 🔔
 * 
 * يوفر كل الـ APIs اللازمة لتطبيق Flutter:
 * عرض الإشعارات، عدد غير المقروء، تحديد كمقروء، حذف
 */
@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
@Tag(name = "User - Notifications", description = "إشعارات المستخدم — عرض وإدارة")
@SecurityRequirement(name = "bearerAuth")
public class UserNotificationController {

    private final UserNotificationService notificationService;

    // ═══════════════════════════════════════════════════════
    // عرض الإشعارات
    // ═══════════════════════════════════════════════════════

    @GetMapping
    @Operation(summary = "جلب كل إشعاراتي", description = "يرجع كل الإشعارات مرتبة من الأحدث")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications());
    }

    @GetMapping("/unread")
    @Operation(summary = "جلب الإشعارات غير المقروءة فقط")
    public ResponseEntity<List<NotificationResponse>> getMyUnreadNotifications() {
        return ResponseEntity.ok(notificationService.getMyUnreadNotifications());
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "جلب إشعارات بنوع معين", description = "الأنواع: WATERING_REMINDER, FERTILIZING_REMINDER, HARVEST_REMINDER, TASK_OVERDUE, DAILY_TIP, GENERAL")
    public ResponseEntity<List<NotificationResponse>> getMyNotificationsByType(
            @Parameter(description = "نوع الإشعار") @PathVariable NotificationType type) {
        return ResponseEntity.ok(notificationService.getMyNotificationsByType(type));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "عدد الإشعارات غير المقروءة", description = "مفيد لعرض badge على أيقونة الجرس 🔔")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }

    // ═══════════════════════════════════════════════════════
    // تحديد كمقروء
    // ═══════════════════════════════════════════════════════

    @PutMapping("/{id}/read")
    @Operation(summary = "تحديد إشعار واحد كمقروء")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    @Operation(summary = "تحديد كل الإشعارات كمقروءة")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        return ResponseEntity.ok(notificationService.markAllAsRead());
    }

    // ═══════════════════════════════════════════════════════
    // حذف
    // ═══════════════════════════════════════════════════════

    @DeleteMapping("/{id}")
    @Operation(summary = "حذف إشعار واحد")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
