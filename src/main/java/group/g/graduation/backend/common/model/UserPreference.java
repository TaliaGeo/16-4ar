package group.g.graduation.backend.common.model;

import group.g.graduation.backend.Security.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * UserPreference entity - تفضيلات المستخدم
 * إعدادات المستخدم الشخصية
 */
@Entity
@Table(name = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    // ===== الموقع (للطقس) =====
    private String city;  // اسم المدينة
    
    private Double latitude;  // خط العرض
    
    private Double longitude;  // خط الطول
    
    // ===== الإشعارات =====
    private Boolean notificationEnabled = true;  // تفعيل الإشعارات عموماً
    
    private Boolean pushEnabled = true;  // تفعيل Push Notifications
    
    private String fcmToken;  // Firebase Cloud Messaging Token
    
    // ===== أوقات التذكير =====
    private LocalTime morningReminderTime;  // وقت تذكير الصباح
    
    private LocalTime eveningReminderTime;  // وقت تذكير المساء
    
    // ===== اللغة =====
    @Column(length = 5)
    private String language = "ar";  // ar أو en
}
