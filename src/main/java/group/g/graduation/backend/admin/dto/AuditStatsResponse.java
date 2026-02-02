package group.g.graduation.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Audit Statistics Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditStatsResponse {
    
    private Long totalLogs;
    private Long totalSuccess;
    private Long totalFailure;
    private Long totalPartial;
    
    // إحصائيات حسب العملية
    private List<ActionStats> actionStats;
    
    // إحصائيات حسب الكيان
    private List<EntityStats> entityStats;
    
    // إحصائيات حسب المستخدم
    private List<UserStats> userStats;
    
    // إحصائيات يومية
    private List<DailyStats> dailyStats;
    
    // متوسط وقت التنفيذ
    private Double averageExecutionTime;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionStats {
        private String action;
        private String actionAr;
        private Long count;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityStats {
        private String entityType;
        private String entityTypeAr;
        private Long count;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private Long userId;
        private String userName;
        private String userEmail;
        private Long actionsCount;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats {
        private String date;
        private Long count;
        private Long successCount;
        private Long failureCount;
    }
}
