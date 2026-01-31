package group.g.graduation.backend.admin.controller;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.admin.service.AdminNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Controller for Notification Management
 * تحكم إدارة الإشعارات للأدمن
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Notifications", description = "إدارة الإشعارات - APIs للأدمن")
public class AdminNotificationController {
    
    private final AdminNotificationService notificationService;
    
    // ===================== Create Operations =====================
    
    @PostMapping("/user/{userId}")
    @Operation(summary = "إنشاء إشعار لمستخدم", description = "إنشاء إشعار جديد لمستخدم محدد")
    public ResponseEntity<NotificationResponse> createNotification(
            @PathVariable Long userId,
            @Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.createNotification(userId, request));
    }
    
    @PostMapping("/broadcast")
    @Operation(summary = "إرسال إشعار جماعي", description = "إرسال إشعار لمجموعة من المستخدمين")
    public ResponseEntity<Map<String, Object>> broadcastNotification(
            @Valid @RequestBody BroadcastNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.broadcastNotification(request));
    }
    
    @PostMapping("/send-to-all")
    @Operation(summary = "إرسال للجميع", description = "إرسال إشعار لجميع المستخدمين")
    public ResponseEntity<Map<String, Object>> sendToAll(
            @Valid @RequestBody NotificationRequest request) {
        BroadcastNotificationRequest broadcastRequest = BroadcastNotificationRequest.builder()
                .titleAr(request.getTitleAr())
                .titleEn(request.getTitleEn())
                .messageAr(request.getMessageAr())
                .messageEn(request.getMessageEn())
                .type(request.getType())
                .scheduledAt(request.getScheduledAt())
                .targetAudience("all")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.broadcastNotification(broadcastRequest));
    }
    
    @PostMapping("/send-to-active")
    @Operation(summary = "إرسال للنشطين", description = "إرسال إشعار للمستخدمين النشطين فقط")
    public ResponseEntity<Map<String, Object>> sendToActiveUsers(
            @Valid @RequestBody NotificationRequest request) {
        BroadcastNotificationRequest broadcastRequest = BroadcastNotificationRequest.builder()
                .titleAr(request.getTitleAr())
                .titleEn(request.getTitleEn())
                .messageAr(request.getMessageAr())
                .messageEn(request.getMessageEn())
                .type(request.getType())
                .scheduledAt(request.getScheduledAt())
                .targetAudience("active")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.broadcastNotification(broadcastRequest));
    }
    
    // ===================== Read Operations =====================
    
    @GetMapping
    @Operation(summary = "جلب كل الإشعارات", description = "جلب قائمة بجميع الإشعارات")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "جلب إشعار", description = "جلب إشعار بمعرفه")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "جلب إشعارات مستخدم", description = "جلب كل إشعارات مستخدم معين")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId));
    }
    
    @GetMapping("/user/{userId}/unread-count")
    @Operation(summary = "عدد غير المقروءة", description = "جلب عدد الإشعارات غير المقروءة لمستخدم")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }
    
    @GetMapping("/stats")
    @Operation(summary = "إحصائيات الإشعارات", description = "جلب إحصائيات عامة عن الإشعارات")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        return ResponseEntity.ok(notificationService.getNotificationStats());
    }
    
    // ===================== Update Operations =====================
    
    @PutMapping("/{id}")
    @Operation(summary = "تعديل إشعار", description = "تعديل إشعار موجود")
    public ResponseEntity<NotificationResponse> updateNotification(
            @PathVariable Long id,
            @Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.updateNotification(id, request));
    }
    
    // ===================== Delete Operations =====================
    
    @DeleteMapping("/{id}")
    @Operation(summary = "حذف إشعار", description = "حذف إشعار بمعرفه")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/user/{userId}")
    @Operation(summary = "حذف إشعارات مستخدم", description = "حذف كل إشعارات مستخدم معين")
    public ResponseEntity<Void> deleteNotificationsByUser(@PathVariable Long userId) {
        notificationService.deleteNotificationsByUser(userId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/old/{daysOld}")
    @Operation(summary = "حذف الإشعارات القديمة", description = "حذف الإشعارات الأقدم من عدد أيام محدد")
    public ResponseEntity<Map<String, Object>> deleteOldNotifications(@PathVariable int daysOld) {
        return ResponseEntity.ok(notificationService.deleteOldNotifications(daysOld));
    }
}
