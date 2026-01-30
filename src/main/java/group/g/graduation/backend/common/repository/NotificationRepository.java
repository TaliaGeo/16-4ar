package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.enums.NotificationType;
import group.g.graduation.backend.common.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // جلب إشعارات مستخدم
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // جلب الإشعارات غير المقروءة
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    
    // جلب إشعارات بنوع معين
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type);
    
    // جلب الإشعارات التي لم ترسل بعد
    List<Notification> findByIsPushedFalseAndScheduledAtBefore(Instant now);
    
    // جلب الإشعارات المجدولة
    @Query("SELECT n FROM Notification n WHERE n.isPushed = false AND n.scheduledAt <= :now")
    List<Notification> findScheduledNotificationsToSend(@Param("now") Instant now);
    
    // عدد الإشعارات غير المقروءة
    long countByUserIdAndIsReadFalse(Long userId);
    
    // تحديث كل الإشعارات كمقروءة
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadForUser(@Param("userId") Long userId);
    
    // حذف الإشعارات القديمة
    void deleteByCreatedAtBefore(Instant date);
}
