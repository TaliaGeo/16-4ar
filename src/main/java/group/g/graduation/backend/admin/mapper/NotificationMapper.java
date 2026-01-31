package group.g.graduation.backend.admin.mapper;

import group.g.graduation.backend.admin.dto.NotificationResponse;
import group.g.graduation.backend.common.enums.NotificationType;
import group.g.graduation.backend.common.model.Notification;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Notification - محوّل الإشعارات
 */
@Component
public class NotificationMapper {
    
    /**
     * Convert Notification entity to NotificationResponse DTO
     */
    public NotificationResponse toResponse(Notification entity) {
        if (entity == null) return null;
        
        NotificationResponse.NotificationResponseBuilder builder = NotificationResponse.builder()
                .id(entity.getId())
                .titleAr(entity.getTitleAr())
                .titleEn(entity.getTitleEn())
                .messageAr(entity.getMessageAr())
                .messageEn(entity.getMessageEn())
                .type(entity.getType())
                .typeDisplayAr(getTypeDisplayAr(entity.getType()))
                .isRead(entity.getIsRead())
                .isPushed(entity.getIsPushed())
                .scheduledAt(entity.getScheduledAt())
                .sentAt(entity.getSentAt())
                .createdAt(entity.getCreatedAt());
        
        // User info
        try {
            if (entity.getUser() != null) {
                builder.userId(entity.getUser().getId())
                       .userEmail(entity.getUser().getEmail())
                       .userName(entity.getUser().getFullName());
            }
        } catch (Exception e) {
            // LazyInitializationException
        }
        
        // UserPlant info
        try {
            if (entity.getUserPlant() != null) {
                builder.userPlantId(entity.getUserPlant().getId());
                if (entity.getUserPlant().getPlant() != null) {
                    builder.plantNameAr(entity.getUserPlant().getPlant().getNameAr());
                }
            }
        } catch (Exception e) {
            // LazyInitializationException
        }
        
        // Task info
        try {
            if (entity.getTask() != null) {
                builder.taskId(entity.getTask().getId());
            }
        } catch (Exception e) {
            // LazyInitializationException
        }
        
        return builder.build();
    }
    
    /**
     * Convert list of Notification entities to NotificationResponse DTOs
     */
    public List<NotificationResponse> toResponseList(List<Notification> entities) {
        if (entities == null) return Collections.emptyList();
        
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get Arabic display name for notification type
     */
    public String getTypeDisplayAr(NotificationType type) {
        if (type == null) return "عام";
        return switch (type) {
            case WATERING_REMINDER -> "تذكير ري";
            case FERTILIZING_REMINDER -> "تذكير تسميد";
            case HARVEST_REMINDER -> "تذكير حصاد";
            case TASK_OVERDUE -> "مهمة متأخرة";
            case DAILY_TIP -> "نصيحة يومية";
            case GENERAL -> "عام";
        };
    }
}
