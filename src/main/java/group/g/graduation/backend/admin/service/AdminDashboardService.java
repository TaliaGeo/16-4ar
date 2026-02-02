package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.DashboardOverviewResponse;
import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.UserRepository;
import group.g.graduation.backend.common.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Dashboard Service
 * خدمة لوحة التحكم للأدمن
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {
    
    private final UserRepository userRepository;
    private final PlantRepository plantRepository;
    private final DailyQuoteRepository quoteRepository;
    private final PlantingQuestionRepository questionRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final PlantTaskRepository plantTaskRepository;
    private final MonthPlantRepository monthPlantRepository;
    private final PlantSuitabilityRepository suitabilityRepository;
    private final NotificationRepository notificationRepository;
    
    /**
     * Get dashboard overview with all statistics
     */
    public DashboardOverviewResponse getDashboardOverview() {
        log.info("Fetching dashboard overview");
        
        Instant now = Instant.now();
        Instant todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant weekStart = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant monthStart = LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        List<User> allUsers = userRepository.findAll();
        
        return DashboardOverviewResponse.builder()
                // User stats
                .totalUsers((long) allUsers.size())
                .activeUsers(allUsers.stream().filter(User::isActive).count())
                .newUsersToday(countUsersCreatedAfter(allUsers, todayStart))
                .newUsersThisWeek(countUsersCreatedAfter(allUsers, weekStart))
                .newUsersThisMonth(countUsersCreatedAfter(allUsers, monthStart))
                // Plant stats
                .totalPlants(plantRepository.count())
                .activePlants(plantRepository.count()) // All plants are active (no isActive field)
                // Content stats
                .totalQuotes(quoteRepository.count())
                .totalQuestions(questionRepository.count())
                .totalTaskTypes(taskTypeRepository.count())
                .totalPlantTasks(plantTaskRepository.count())
                .totalMonthPlantRelations(monthPlantRepository.count())
                .totalSuitabilities(suitabilityRepository.count())
                // Notification stats
                .totalNotifications(notificationRepository.count())
                .unreadNotifications(countUnreadNotifications())
                // System info
                .lastUpdated(now)
                .build();
    }
    
    /**
     * Get user statistics
     */
    public Map<String, Object> getUserStats() {
        List<User> allUsers = userRepository.findAll();
        
        Instant todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant weekStart = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant monthStart = LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", allUsers.size());
        stats.put("activeUsers", allUsers.stream().filter(User::isActive).count());
        stats.put("inactiveUsers", allUsers.stream().filter(u -> !u.isActive()).count());
        stats.put("newUsersToday", countUsersCreatedAfter(allUsers, todayStart));
        stats.put("newUsersThisWeek", countUsersCreatedAfter(allUsers, weekStart));
        stats.put("newUsersThisMonth", countUsersCreatedAfter(allUsers, monthStart));
        
        // By auth provider
        Map<String, Long> byProvider = allUsers.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getAuthProvider().name(),
                        Collectors.counting()));
        stats.put("byAuthProvider", byProvider);
        
        return stats;
    }
    
    /**
     * Get user growth data (last 30 days)
     */
    public List<Map<String, Object>> getUserGrowth() {
        List<User> allUsers = userRepository.findAll();
        List<Map<String, Object>> growth = new ArrayList<>();
        
        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Instant dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            
            long count = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null 
                            && u.getCreatedAt().isAfter(dayStart) 
                            && u.getCreatedAt().isBefore(dayEnd))
                    .count();
            
            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", date.toString());
            dayData.put("newUsers", count);
            growth.add(dayData);
        }
        
        return growth;
    }
    
    /**
     * Get plant statistics
     */
    public Map<String, Object> getPlantStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long totalPlants = plantRepository.count();
        stats.put("totalPlants", totalPlants);
        stats.put("activePlants", totalPlants); // All plants are active (no isActive field)
        stats.put("inactivePlants", 0L);
        stats.put("totalPlantTasks", plantTaskRepository.count());
        stats.put("totalSuitabilities", suitabilityRepository.count());
        stats.put("totalMonthRelations", monthPlantRepository.count());
        
        return stats;
    }
    
    /**
     * Get content statistics
     */
    public Map<String, Object> getContentStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("quotes", quoteRepository.count());
        stats.put("questions", questionRepository.count());
        stats.put("taskTypes", taskTypeRepository.count());
        stats.put("plantTasks", plantTaskRepository.count());
        stats.put("monthPlantRelations", monthPlantRepository.count());
        stats.put("suitabilities", suitabilityRepository.count());
        
        return stats;
    }
    
    /**
     * Get notification statistics
     */
    public Map<String, Object> getNotificationStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        var allNotifications = notificationRepository.findAll();
        
        stats.put("total", allNotifications.size());
        stats.put("read", allNotifications.stream().filter(n -> Boolean.TRUE.equals(n.getIsRead())).count());
        stats.put("unread", allNotifications.stream().filter(n -> Boolean.FALSE.equals(n.getIsRead())).count());
        stats.put("pushed", allNotifications.stream().filter(n -> Boolean.TRUE.equals(n.getIsPushed())).count());
        stats.put("pending", allNotifications.stream().filter(n -> Boolean.FALSE.equals(n.getIsPushed())).count());
        
        // By type
        Map<String, Long> byType = allNotifications.stream()
                .filter(n -> n.getType() != null)
                .collect(Collectors.groupingBy(
                        n -> n.getType().name(),
                        Collectors.counting()));
        stats.put("byType", byType);
        
        return stats;
    }
    
    /**
     * Get system health/summary
     */
    public Map<String, Object> getSystemSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        
        summary.put("users", Map.of(
                "total", userRepository.count(),
                "active", userRepository.findAll().stream().filter(User::isActive).count()
        ));
        
        summary.put("plants", Map.of(
                "total", plantRepository.count(),
                "active", plantRepository.count() // All plants are active
        ));
        
        summary.put("content", Map.of(
                "quotes", quoteRepository.count(),
                "questions", questionRepository.count(),
                "taskTypes", taskTypeRepository.count()
        ));
        
        summary.put("notifications", Map.of(
                "total", notificationRepository.count(),
                "unread", countUnreadNotifications()
        ));
        
        summary.put("lastUpdated", Instant.now());
        
        return summary;
    }
    
    /**
     * Get all statistics in one call
     */
    public Map<String, Object> getAllStats() {
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("overview", getDashboardOverview());
        all.put("users", getUserStats());
        all.put("plants", getPlantStats());
        all.put("content", getContentStats());
        all.put("notifications", getNotificationStats());
        return all;
    }
    
    // ===================== Helper Methods =====================
    
    private long countUsersCreatedAfter(List<User> users, Instant after) {
        return users.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(after))
                .count();
    }
    
    private long countUnreadNotifications() {
        return notificationRepository.findAll().stream()
                .filter(n -> Boolean.FALSE.equals(n.getIsRead()))
                .count();
    }
}
