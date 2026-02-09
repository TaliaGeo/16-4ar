package group.g.graduation.backend.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Audit Log Entity - سجل تتبع الأنشطة
 * يسجل كل عمليات الأدمن على النظام
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "userId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_entity_type", columnList = "entityType"),
    @Index(name = "idx_audit_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // المستخدم الذي قام بالعملية (nullable for system actions)
    @Column(nullable = true)
    private Long userId;
    
    private String userName;
    
    private String userEmail;
    
    // نوع العملية
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditAction action;
    
    // نوع الكيان المتأثر
    @Column(nullable = false)
    private String entityType;
    
    // معرف الكيان المتأثر
    private Long entityId;
    
    // وصف العملية
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // البيانات القديمة (JSON)
    @Column(columnDefinition = "TEXT")
    private String oldValue;
    
    // البيانات الجديدة (JSON)
    @Column(columnDefinition = "TEXT")
    private String newValue;
    
    // عنوان IP
    private String ipAddress;
    
    // User Agent
    private String userAgent;
    
    // الـ Endpoint المستدعى
    private String endpoint;
    
    // HTTP Method
    private String httpMethod;
    
    // حالة العملية
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuditStatus status = AuditStatus.SUCCESS;
    
    // رسالة الخطأ (في حالة الفشل)
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    // وقت تنفيذ العملية (بالميلي ثانية)
    private Long executionTime;
    
    // تاريخ الإنشاء
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    // ===== Enums =====
    
    public enum AuditAction {
        // CRUD Operations
        CREATE,
        READ,
        UPDATE,
        DELETE,
        
        // Bulk Operations
        BULK_CREATE,
        BULK_UPDATE,
        BULK_DELETE,
        
        // Auth Operations
        LOGIN,
        LOGOUT,
        PASSWORD_CHANGE,
        
        // Admin Operations
        ACTIVATE,
        DEACTIVATE,
        ASSIGN_ROLE,
        REMOVE_ROLE,
        
        // File Operations
        UPLOAD,
        DOWNLOAD,
        
        // Notification Operations
        BROADCAST,
        SEND_NOTIFICATION,
        
        // Export/Import
        EXPORT,
        IMPORT
    }
    
    public enum AuditStatus {
        SUCCESS,
        FAILURE,
        PARTIAL
    }
}
