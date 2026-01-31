package group.g.graduation.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO for Dashboard Overview Response
 * استجابة لوحة التحكم الرئيسية
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {
    
    // ===== User Statistics =====
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsersToday;
    private Long newUsersThisWeek;
    private Long newUsersThisMonth;
    
    // ===== Plant Statistics =====
    private Long totalPlants;
    private Long activePlants;
    
    // ===== Content Statistics =====
    private Long totalQuotes;
    private Long totalQuestions;
    private Long totalTaskTypes;
    private Long totalPlantTasks;
    private Long totalMonthPlantRelations;
    private Long totalSuitabilities;
    
    // ===== Notification Statistics =====
    private Long totalNotifications;
    private Long unreadNotifications;
    
    // ===== System Info =====
    private Instant lastUpdated;
    
    /**
     * Nested DTO for popular plants
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularPlant {
        private Long plantId;
        private String nameAr;
        private String nameEn;
        private Long userCount;
    }
    
    /**
     * Nested DTO for growth data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrowthData {
        private String period;
        private Long count;
        private Double percentageChange;
    }
    
    /**
     * Nested DTO for recent activity
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String type;
        private String description;
        private String descriptionAr;
        private Instant timestamp;
        private Long userId;
        private String userName;
    }
}
