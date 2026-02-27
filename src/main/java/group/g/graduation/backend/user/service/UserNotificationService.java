package group.g.graduation.backend.user.service;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.UserRepository;
import group.g.graduation.backend.Security.util.SecurityUtils;
import group.g.graduation.backend.admin.dto.NotificationResponse;
import group.g.graduation.backend.admin.mapper.NotificationMapper;
import group.g.graduation.backend.common.enums.NotificationType;
import group.g.graduation.backend.common.model.Notification;
import group.g.graduation.backend.common.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * User Notification Service - خدمة إشعارات المستخدم 🔔
 * 
 * تدير كل عمليات الإشعارات من جهة المستخدم:
 * عرض، قراءة، تحديد كمقروء، حذف
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;

    /**
     * جلب كل إشعارات المستخدم الحالي
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        Long userId = getCurrentUser().getId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    /**
     * جلب الإشعارات غير المقروءة فقط
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyUnreadNotifications() {
        Long userId = getCurrentUser().getId();
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    /**
     * جلب إشعارات بنوع معين
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotificationsByType(NotificationType type) {
        Long userId = getCurrentUser().getId();
        return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    /**
     * عدد الإشعارات غير المقروءة
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadCount() {
        Long userId = getCurrentUser().getId();
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return Map.of(
                "unreadCount", count,
                "userId", userId
        );
    }

    /**
     * تحديد إشعار واحد كمقروء
     */
    public NotificationResponse markAsRead(Long notificationId) {
        User currentUser = getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("الإشعار غير موجود — Notification not found"));

        // التأكد أن الإشعار يخص المستخدم الحالي
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("لا يمكنك الوصول لهذا الإشعار — Access denied");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);

        log.debug("✅ Notification #{} marked as read for user #{}", notificationId, currentUser.getId());
        return notificationMapper.toResponse(notification);
    }

    /**
     * تحديد كل الإشعارات كمقروءة
     */
    public Map<String, Object> markAllAsRead() {
        Long userId = getCurrentUser().getId();
        long unreadBefore = notificationRepository.countByUserIdAndIsReadFalse(userId);
        notificationRepository.markAllAsReadForUser(userId);

        log.info("✅ All notifications marked as read for user #{} ({} notifications)", userId, unreadBefore);
        return Map.of(
                "message", "تم تحديد كل الإشعارات كمقروءة",
                "messageEn", "All notifications marked as read",
                "markedCount", unreadBefore
        );
    }

    /**
     * حذف إشعار واحد
     */
    public void deleteNotification(Long notificationId) {
        User currentUser = getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("الإشعار غير موجود — Notification not found"));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("لا يمكنك حذف هذا الإشعار — Access denied");
        }

        notificationRepository.delete(notification);
        log.debug("🗑️ Notification #{} deleted for user #{}", notificationId, currentUser.getId());
    }

    // ═══════════════════════════════════════════════════════

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
