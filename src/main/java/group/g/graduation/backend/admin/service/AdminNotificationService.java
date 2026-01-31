package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.admin.mapper.NotificationMapper;
import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.UserRepository;
import group.g.graduation.backend.common.enums.NotificationType;
import group.g.graduation.backend.common.model.Notification;
import group.g.graduation.backend.common.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Service for Notification Management
 * خدمة إدارة الإشعارات للأدمن
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminNotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper mapper;
    
    // ===================== CRUD Operations =====================
    
    /**
     * Create a notification for a specific user
     */
    public NotificationResponse createNotification(Long userId, NotificationRequest request) {
        log.info("Creating notification for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitleAr(request.getTitleAr());
        notification.setTitleEn(request.getTitleEn());
        notification.setMessageAr(request.getMessageAr());
        notification.setMessageEn(request.getMessageEn());
        notification.setType(request.getType() != null ? request.getType() : NotificationType.GENERAL);
        notification.setScheduledAt(request.getScheduledAt());
        notification.setIsRead(false);
        notification.setIsPushed(false);
        
        Notification saved = notificationRepository.save(notification);
        log.info("Created notification with ID: {}", saved.getId());
        
        return mapper.toResponse(saved);
    }
    
    /**
     * Broadcast notification to multiple users
     */
    public Map<String, Object> broadcastNotification(BroadcastNotificationRequest request) {
        log.info("Broadcasting notification to: {}", request.getTargetAudience());
        
        List<User> targetUsers = getTargetUsers(request);
        List<Notification> notifications = new ArrayList<>();
        
        for (User user : targetUsers) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitleAr(request.getTitleAr());
            notification.setTitleEn(request.getTitleEn());
            notification.setMessageAr(request.getMessageAr());
            notification.setMessageEn(request.getMessageEn());
            notification.setType(request.getType() != null ? request.getType() : NotificationType.GENERAL);
            notification.setScheduledAt(request.getScheduledAt());
            notification.setIsRead(false);
            notification.setIsPushed(false);
            
            notifications.add(notification);
        }
        
        List<Notification> saved = notificationRepository.saveAll(notifications);
        log.info("Broadcasted {} notifications", saved.size());
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalSent", saved.size());
        result.put("targetAudience", request.getTargetAudience());
        result.put("scheduledAt", request.getScheduledAt());
        result.put("titleAr", request.getTitleAr());
        
        return result;
    }
    
    /**
     * Get all notifications (with pagination support later)
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        return mapper.toResponseList(notificationRepository.findAll());
    }
    
    /**
     * Get notification by ID
     */
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + id));
        return mapper.toResponse(notification);
    }
    
    /**
     * Get notifications by user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with ID: " + userId);
        }
        return mapper.toResponseList(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }
    
    /**
     * Get unread notifications count for user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadCount(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with ID: " + userId);
        }
        
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("unreadCount", count);
        
        return result;
    }
    
    /**
     * Update notification
     */
    public NotificationResponse updateNotification(Long id, NotificationRequest request) {
        log.info("Updating notification with ID: {}", id);
        
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + id));
        
        if (request.getTitleAr() != null) {
            notification.setTitleAr(request.getTitleAr());
        }
        if (request.getTitleEn() != null) {
            notification.setTitleEn(request.getTitleEn());
        }
        if (request.getMessageAr() != null) {
            notification.setMessageAr(request.getMessageAr());
        }
        if (request.getMessageEn() != null) {
            notification.setMessageEn(request.getMessageEn());
        }
        if (request.getType() != null) {
            notification.setType(request.getType());
        }
        if (request.getScheduledAt() != null) {
            notification.setScheduledAt(request.getScheduledAt());
        }
        
        Notification saved = notificationRepository.save(notification);
        log.info("Updated notification with ID: {}", saved.getId());
        
        return mapper.toResponse(saved);
    }
    
    /**
     * Delete notification
     */
    public void deleteNotification(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new EntityNotFoundException("Notification not found with ID: " + id);
        }
        notificationRepository.deleteById(id);
        log.info("Deleted notification with ID: {}", id);
    }
    
    /**
     * Delete notifications by user
     */
    public void deleteNotificationsByUser(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notificationRepository.deleteAll(notifications);
        log.info("Deleted {} notifications for user: {}", notifications.size(), userId);
    }
    
    /**
     * Delete old notifications (older than specified days)
     */
    public Map<String, Object> deleteOldNotifications(int daysOld) {
        Instant cutoffDate = Instant.now().minus(daysOld, ChronoUnit.DAYS);
        
        List<Notification> oldNotifications = notificationRepository.findAll().stream()
                .filter(n -> n.getCreatedAt() != null && n.getCreatedAt().isBefore(cutoffDate))
                .collect(Collectors.toList());
        
        notificationRepository.deleteAll(oldNotifications);
        
        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", oldNotifications.size());
        result.put("daysOld", daysOld);
        result.put("cutoffDate", cutoffDate);
        
        log.info("Deleted {} notifications older than {} days", oldNotifications.size(), daysOld);
        return result;
    }
    
    // ===================== Statistics =====================
    
    /**
     * Get notification statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStats() {
        List<Notification> all = notificationRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNotifications", all.size());
        stats.put("totalRead", all.stream().filter(n -> Boolean.TRUE.equals(n.getIsRead())).count());
        stats.put("totalUnread", all.stream().filter(n -> Boolean.FALSE.equals(n.getIsRead())).count());
        stats.put("totalPushed", all.stream().filter(n -> Boolean.TRUE.equals(n.getIsPushed())).count());
        stats.put("totalPending", all.stream().filter(n -> Boolean.FALSE.equals(n.getIsPushed())).count());
        
        // By type
        Map<String, Long> byType = all.stream()
                .filter(n -> n.getType() != null)
                .collect(Collectors.groupingBy(
                        n -> n.getType().name(),
                        Collectors.counting()));
        stats.put("byType", byType);
        
        // Today's notifications
        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long todayCount = all.stream()
                .filter(n -> n.getCreatedAt() != null && n.getCreatedAt().isAfter(todayStart))
                .count();
        stats.put("todayCount", todayCount);
        
        return stats;
    }
    
    // ===================== Helper Methods =====================
    
    private List<User> getTargetUsers(BroadcastNotificationRequest request) {
        String audience = request.getTargetAudience();
        
        if (audience == null || "all".equalsIgnoreCase(audience)) {
            return userRepository.findAll();
        }
        
        if ("specific".equalsIgnoreCase(audience) && request.getUserIds() != null) {
            return userRepository.findAllById(request.getUserIds());
        }
        
        if ("active".equalsIgnoreCase(audience)) {
            // Return active users only
            return userRepository.findAll().stream()
                    .filter(User::isActive)
                    .collect(Collectors.toList());
        }
        
        return userRepository.findAll();
    }
}
