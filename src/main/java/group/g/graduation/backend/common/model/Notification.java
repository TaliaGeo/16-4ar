package group.g.graduation.backend.common.model;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.common.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Notification entity - الإشعارات
 * إشعارات التذكير والتنبيهات للمستخدم
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // ===== المحتوى بلغتين =====
    private String titleAr;  // عنوان الإشعار - عربي
    
    private String titleEn;  // عنوان الإشعار - إنجليزي
    
    @Column(columnDefinition = "TEXT")
    private String messageAr;  // رسالة الإشعار - عربي
    
    @Column(columnDefinition = "TEXT")
    private String messageEn;  // رسالة الإشعار - إنجليزي
    
    // ===== النوع =====
    @Enumerated(EnumType.STRING)
    private NotificationType type;  // نوع الإشعار
    
    // ===== الربط (اختياري) =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_plant_id")
    private UserPlant userPlant;  // ربط بنبتة المستخدم
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private UserPlantTask task;  // ربط بمهمة
    
    // ===== الحالة =====
    private Boolean isRead = false;  // هل قرأها المستخدم
    
    private Boolean isPushed = false;  // هل أُرسلت كـ Push Notification
    
    // ===== التوقيت =====
    private Instant scheduledAt;  // متى المفروض ترسل
    
    private Instant sentAt;  // متى أرسلت فعلياً
    
    @Column(updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
