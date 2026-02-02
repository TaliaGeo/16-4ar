package group.g.graduation.backend.common.repository;

import group.g.graduation.backend.common.model.AuditLog;
import group.g.graduation.backend.common.model.AuditLog.AuditAction;
import group.g.graduation.backend.common.model.AuditLog.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Audit Log Repository - مستودع سجلات التتبع
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // البحث حسب المستخدم
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // البحث حسب نوع العملية
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);
    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);
    
    // البحث حسب نوع الكيان
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);
    Page<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);
    
    // البحث حسب الحالة
    Page<AuditLog> findByStatusOrderByCreatedAtDesc(AuditStatus status, Pageable pageable);
    
    // البحث حسب نطاق زمني
    Page<AuditLog> findByCreatedAtBetween(Instant startDate, Instant endDate, Pageable pageable);
    List<AuditLog> findByCreatedAtBetween(Instant startDate, Instant endDate);
    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            Instant startDate, Instant endDate, Pageable pageable);
    
    // البحث حسب الكيان المحدد
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId);
    
    // البحث المتقدم
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByFilters(
            @Param("userId") Long userId,
            @Param("action") AuditAction action,
            @Param("entityType") String entityType,
            @Param("status") AuditStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);
    
    // إحصائيات حسب العملية
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByAction();
    
    // إحصائيات حسب الكيان
    @Query("SELECT a.entityType, COUNT(a) FROM AuditLog a GROUP BY a.entityType ORDER BY COUNT(a) DESC")
    List<Object[]> countByEntityType();
    
    // إحصائيات حسب المستخدم
    @Query("SELECT a.userId, a.userName, COUNT(a) FROM AuditLog a GROUP BY a.userId, a.userName ORDER BY COUNT(a) DESC")
    List<Object[]> countByUser();
    
    // إحصائيات يومية
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count " +
                   "FROM audit_logs " +
                   "WHERE created_at >= :startDate " +
                   "GROUP BY DATE(created_at) " +
                   "ORDER BY date DESC", nativeQuery = true)
    List<Object[]> countByDay(@Param("startDate") Instant startDate);
    
    // عدد العمليات الفاشلة
    long countByStatus(AuditStatus status);
    
    // عدد السجلات قبل تاريخ معين
    long countByCreatedAtBefore(Instant date);
    
    // حذف السجلات القديمة
    void deleteByCreatedAtBefore(Instant date);
    
    // آخر عمليات مستخدم معين
    List<AuditLog> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
    
    // آخر عمليات على كيان معين
    List<AuditLog> findTop10ByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId);
}
