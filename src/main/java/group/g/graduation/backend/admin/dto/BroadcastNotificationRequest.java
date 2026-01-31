package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for broadcasting notifications to multiple users
 * طلب إرسال إشعار جماعي
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastNotificationRequest {
    
    @NotBlank(message = "Arabic title is required")
    private String titleAr;
    
    private String titleEn;
    
    @NotBlank(message = "Arabic message is required")
    private String messageAr;
    
    private String messageEn;
    
    private NotificationType type;  // Default: GENERAL
    
    /**
     * Target audience - الجمهور المستهدف
     * Options: "all", "active", "specific"
     */
    private String targetAudience;
    
    /**
     * Specific user IDs (when targetAudience = "specific")
     */
    private List<Long> userIds;
    
    /**
     * Schedule time (optional) - if null, send immediately
     */
    private Instant scheduledAt;
}
