package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.AuditLogResponse;
import group.g.graduation.backend.admin.dto.AuditStatsResponse;
import group.g.graduation.backend.common.model.AuditLog;
import group.g.graduation.backend.common.model.AuditLog.AuditAction;
import group.g.graduation.backend.common.model.AuditLog.AuditStatus;
import group.g.graduation.backend.common.repository.AuditLogRepository;
import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Audit Service - خدمة تسجيل وإدارة سجلات التدقيق
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    // ===================== Audit Logging Methods =====================
    
    /**
     * استخراج معلومات المستخدم الحالي من SecurityContext
     * يُستدعى في main thread قبل أي async operations
     */
    public AuditUserContext getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String email = auth.getName();
                return userRepository.findByEmail(email)
                        .map(user -> AuditUserContext.builder()
                                .userId(user.getId())
                                .userName(user.getFullName())
                                .userEmail(user.getEmail())
                                .build())
                        .orElse(AuditUserContext.system());
            }
        } catch (Exception e) {
            log.warn("Could not extract user from SecurityContext: {}", e.getMessage());
        }
        return AuditUserContext.system();
    }
    
    /**
     * تسجيل عملية ناجحة مع user context
     */
    public void logAction(AuditUserContext userContext, AuditAction action, String entityType, 
                          Long entityId, String description, Object oldValue, Object newValue) {
        logActionAsync(userContext, action, entityType, entityId, description, oldValue, newValue, null);
    }
    
    /**
     * تسجيل عملية ناجحة مع وقت التنفيذ
     */
    public void logAction(AuditUserContext userContext, AuditAction action, String entityType, 
                          Long entityId, String description, Object oldValue, Object newValue, 
                          long executionTime) {
        logActionAsync(userContext, action, entityType, entityId, description, oldValue, newValue, executionTime);
    }
    
    /**
     * تسجيل عملية فاشلة مع user context
     */
    public void logFailedAction(AuditUserContext userContext, AuditAction action, String entityType, 
                                Long entityId, String description, String errorMessage) {
        logFailedActionAsync(userContext, action, entityType, entityId, description, errorMessage);
    }
    
    /**
     * تسجيل عملية بسيطة
     */
    public void logSimpleAction(AuditUserContext userContext, AuditAction action, String description) {
        logSimpleActionAsync(userContext, action, description);
    }
    
    // ===== Backward Compatibility Methods (auto-extract user) =====
    
    public void logAction(AuditAction action, String entityType, Long entityId, 
                          String description, Object oldValue, Object newValue) {
        logAction(getCurrentUser(), action, entityType, entityId, description, oldValue, newValue);
    }
    
    public void logAction(AuditAction action, String entityType, Long entityId, 
                          String description, Object oldValue, Object newValue, long executionTime) {
        logAction(getCurrentUser(), action, entityType, entityId, description, oldValue, newValue, executionTime);
    }
    
    public void logFailedAction(AuditAction action, String entityType, Long entityId, 
                                String description, String errorMessage) {
        logFailedAction(getCurrentUser(), action, entityType, entityId, description, errorMessage);
    }
    
    public void logSimpleAction(AuditAction action, String description) {
        logSimpleAction(getCurrentUser(), action, description);
    }
    
    // ===== Internal Async Methods =====
    
    @Async
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    void logActionAsync(AuditUserContext userContext, AuditAction action, String entityType, 
                        Long entityId, String description, Object oldValue, Object newValue, 
                        Long executionTime) {
        try {
            AuditLog auditLog = buildAuditLog(userContext, action, entityType, entityId, description, 
                    toJson(oldValue), toJson(newValue), AuditStatus.SUCCESS, null, executionTime);
            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} - {} - {} by user {}", action, entityType, entityId, 
                    userContext.getUserId());
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
            // لا نرمي exception - الـ audit لا يجب أن يفشل العملية الأساسية
        }
    }
    
    @Async
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    void logFailedActionAsync(AuditUserContext userContext, AuditAction action, String entityType, 
                              Long entityId, String description, String errorMessage) {
        try {
            AuditLog auditLog = buildAuditLog(userContext, action, entityType, entityId, description, 
                    null, null, AuditStatus.FAILURE, errorMessage, null);
            auditLogRepository.save(auditLog);
            log.debug("Failed action logged: {} - {} - {} by user {}", action, entityType, entityId, 
                    userContext.getUserId());
        } catch (Exception e) {
            log.error("Failed to save audit log for failed action: {}", e.getMessage(), e);
            // لا نرمي exception - الـ audit لا يجب أن يفشل العملية الأساسية
        }
    }
    
    @Async
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    void logSimpleActionAsync(AuditUserContext userContext, AuditAction action, String description) {
        try {
            AuditLog auditLog = buildAuditLog(userContext, action, null, null, description, 
                    null, null, AuditStatus.SUCCESS, null, null);
            auditLogRepository.save(auditLog);
            log.debug("Simple action logged: {} by user {}", action, userContext.getUserId());
        } catch (Exception e) {
            log.error("Failed to save simple audit log: {}", e.getMessage(), e);
            // لا نرمي exception - الـ audit لا يجب أن يفشل العملية الأساسية
        }
    }
    
    /**
     * تسجيل عملية إنشاء
     */
    public void logCreate(String entityType, Long entityId, Object newEntity) {
        logAction(AuditAction.CREATE, entityType, entityId, 
                "تم إنشاء " + getEntityArabic(entityType) + " جديد", null, newEntity);
    }
    
    /**
     * تسجيل عملية تحديث
     */
    public void logUpdate(String entityType, Long entityId, Object oldEntity, Object newEntity) {
        logAction(AuditAction.UPDATE, entityType, entityId, 
                "تم تحديث " + getEntityArabic(entityType), oldEntity, newEntity);
    }
    
    /**
     * تسجيل عملية حذف
     */
    public void logDelete(String entityType, Long entityId, Object deletedEntity) {
        logAction(AuditAction.DELETE, entityType, entityId, 
                "تم حذف " + getEntityArabic(entityType), deletedEntity, null);
    }
    
    /**
     * تسجيل عملية حذف متعدد
     */
    public void logBulkDelete(String entityType, List<Long> ids) {
        logAction(AuditAction.BULK_DELETE, entityType, null, 
                "تم حذف " + ids.size() + " " + getEntityArabic(entityType), 
                Map.of("deletedIds", ids), null);
    }
    
    // ===================== Audit Retrieval Methods =====================
    
    /**
     * جلب جميع السجلات مع الترقيم
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(this::toResponse);
    }
    
    /**
     * جلب سجل بالمعرف
     */
    @Transactional(readOnly = true)
    public Optional<AuditLogResponse> getLogById(Long id) {
        return auditLogRepository.findById(id)
                .map(this::toResponse);
    }
    
    /**
     * جلب سجلات مستخدم معين
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }
    
    /**
     * جلب سجلات عملية معينة
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByAction(AuditAction action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable)
                .map(this::toResponse);
    }
    
    /**
     * جلب سجلات كيان معين
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByEntity(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(this::toResponse);
    }
    
    /**
     * جلب سجلات نوع كيان معين
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityType(entityType, pageable)
                .map(this::toResponse);
    }
    
    /**
     * جلب سجلات فترة زمنية
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetween(startDate, endDate, pageable)
                .map(this::toResponse);
    }
    
    /**
     * بحث متقدم في السجلات
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchLogs(Long userId, AuditAction action, 
                                              String entityType, AuditStatus status,
                                              Instant startDate, Instant endDate, 
                                              Pageable pageable) {
        return auditLogRepository.findByFilters(userId, action, entityType, status, 
                        startDate, endDate, pageable)
                .map(this::toResponse);
    }
    
    // ===================== Statistics Methods =====================
    
    /**
     * جلب إحصائيات السجلات
     */
    @Transactional(readOnly = true)
    public AuditStatsResponse getStatistics(Instant startDate, Instant endDate) {
        // إذا لم يتم تحديد فترة، استخدم آخر 30 يوم
        if (startDate == null) {
            startDate = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endDate == null) {
            endDate = Instant.now();
        }
        
        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetween(startDate, endDate);
        
        long total = logs.size();
        long successCount = logs.stream().filter(l -> l.getStatus() == AuditStatus.SUCCESS).count();
        long failureCount = logs.stream().filter(l -> l.getStatus() == AuditStatus.FAILURE).count();
        long partialCount = logs.stream().filter(l -> l.getStatus() == AuditStatus.PARTIAL).count();
        
        // إحصائيات حسب العملية
        List<AuditStatsResponse.ActionStats> actionStats = logs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()))
                .entrySet().stream()
                .map(e -> AuditStatsResponse.ActionStats.builder()
                        .action(e.getKey().name())
                        .actionAr(AuditLogResponse.getActionArabic(e.getKey()))
                        .count(e.getValue())
                        .percentage(total > 0 ? (e.getValue() * 100.0 / total) : 0)
                        .build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
        
        // إحصائيات حسب الكيان
        List<AuditStatsResponse.EntityStats> entityStats = logs.stream()
                .filter(l -> l.getEntityType() != null)
                .collect(Collectors.groupingBy(AuditLog::getEntityType, Collectors.counting()))
                .entrySet().stream()
                .map(e -> AuditStatsResponse.EntityStats.builder()
                        .entityType(e.getKey())
                        .entityTypeAr(AuditLogResponse.getEntityTypeArabic(e.getKey()))
                        .count(e.getValue())
                        .percentage(total > 0 ? (e.getValue() * 100.0 / total) : 0)
                        .build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
        
        // إحصائيات حسب المستخدم (أعلى 10)
        List<AuditStatsResponse.UserStats> userStats = logs.stream()
                .filter(l -> l.getUserId() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getUserId() + "|" + l.getUserName() + "|" + l.getUserEmail(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> {
                    String[] parts = e.getKey().split("\\|");
                    return AuditStatsResponse.UserStats.builder()
                            .userId(Long.parseLong(parts[0]))
                            .userName(parts.length > 1 ? parts[1] : null)
                            .userEmail(parts.length > 2 ? parts[2] : null)
                            .actionsCount(e.getValue())
                            .percentage(total > 0 ? (e.getValue() * 100.0 / total) : 0)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getActionsCount(), a.getActionsCount()))
                .limit(10)
                .collect(Collectors.toList());
        
        // إحصائيات يومية
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<AuditStatsResponse.DailyStats> dailyStats = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> LocalDate.ofInstant(l.getCreatedAt(), ZoneId.systemDefault())
                ))
                .entrySet().stream()
                .map(e -> {
                    List<AuditLog> dayLogs = e.getValue();
                    return AuditStatsResponse.DailyStats.builder()
                            .date(e.getKey().format(formatter))
                            .count((long) dayLogs.size())
                            .successCount(dayLogs.stream().filter(l -> l.getStatus() == AuditStatus.SUCCESS).count())
                            .failureCount(dayLogs.stream().filter(l -> l.getStatus() == AuditStatus.FAILURE).count())
                            .build();
                })
                .sorted(Comparator.comparing(AuditStatsResponse.DailyStats::getDate))
                .collect(Collectors.toList());
        
        // متوسط وقت التنفيذ
        Double avgExecTime = logs.stream()
                .filter(l -> l.getExecutionTime() != null)
                .mapToLong(AuditLog::getExecutionTime)
                .average()
                .orElse(0);
        
        return AuditStatsResponse.builder()
                .totalLogs(total)
                .totalSuccess(successCount)
                .totalFailure(failureCount)
                .totalPartial(partialCount)
                .actionStats(actionStats)
                .entityStats(entityStats)
                .userStats(userStats)
                .dailyStats(dailyStats)
                .averageExecutionTime(avgExecTime)
                .build();
    }
    
    // ===================== Cleanup Methods =====================
    
    /**
     * حذف السجلات القديمة
     */
    @Transactional
    public long cleanupOldLogs(int daysToKeep) {
        Instant cutoffDate = Instant.now().minus(daysToKeep, ChronoUnit.DAYS);
        long count = auditLogRepository.countByCreatedAtBefore(cutoffDate);
        auditLogRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("Deleted {} audit logs older than {} days", count, daysToKeep);
        return count;
    }
    
    // ===================== Helper Methods =====================
    
    private AuditLog buildAuditLog(AuditUserContext userContext, AuditAction action, String entityType, 
                                    Long entityId, String description, String oldValue, String newValue,
                                    AuditStatus status, String errorMessage, Long executionTime) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDescription(description);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setStatus(status);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setExecutionTime(executionTime);
        auditLog.setCreatedAt(Instant.now());
        
        // تعيين معلومات المستخدم من userContext
        if (userContext != null && userContext.getUserId() != null) {
            auditLog.setUserId(userContext.getUserId());
            auditLog.setUserName(userContext.getUserName());
            auditLog.setUserEmail(userContext.getUserEmail());
        } else {
            // عمليات النظام (بدون مستخدم مسجل دخول)
            auditLog.setUserId(-1L);
            auditLog.setUserName("SYSTEM");
            auditLog.setUserEmail("system@internal");
        }
        
        // الحصول على معلومات الطلب HTTP
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) 
                    RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setEndpoint(request.getRequestURI());
                auditLog.setHttpMethod(request.getMethod());
            }
        } catch (Exception e) {
            log.warn("Could not get request info for audit log: {}", e.getMessage());
        }
        
        return auditLog;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert object to JSON: {}", e.getMessage());
            return obj.toString();
        }
    }
    
    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userName(log.getUserName())
                .userEmail(log.getUserEmail())
                .action(log.getAction())
                .actionAr(AuditLogResponse.getActionArabic(log.getAction()))
                .entityType(log.getEntityType())
                .entityTypeAr(AuditLogResponse.getEntityTypeArabic(log.getEntityType()))
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .endpoint(log.getEndpoint())
                .httpMethod(log.getHttpMethod())
                .status(log.getStatus())
                .statusAr(AuditLogResponse.getStatusArabic(log.getStatus()))
                .errorMessage(log.getErrorMessage())
                .executionTime(log.getExecutionTime())
                .createdAt(log.getCreatedAt())
                .build();
    }
    
    private String getEntityArabic(String entityType) {
        return AuditLogResponse.getEntityTypeArabic(entityType);
    }
}
