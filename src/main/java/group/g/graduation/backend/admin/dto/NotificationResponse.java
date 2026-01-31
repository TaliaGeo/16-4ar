package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for notification response
 * استجابة الإشعار
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    private Long id;
    
    // User info
    private Long userId;
    private String userEmail;
    private String userName;
    
    // Content
    private String titleAr;
    private String titleEn;
    private String messageAr;
    private String messageEn;
    private NotificationType type;
    private String typeDisplayAr;
    
    // Status
    private Boolean isRead;
    private Boolean isPushed;
    
    // Related entities (optional)
    private Long userPlantId;
    private String plantNameAr;
    private Long taskId;
    
    // Timestamps
    private Instant scheduledAt;
    private Instant sentAt;
    private Instant createdAt;
}
