package group.g.graduation.backend.admin.controller;

import group.g.graduation.backend.admin.dto.AuditLogResponse;
import group.g.graduation.backend.admin.dto.AuditStatsResponse;
import group.g.graduation.backend.admin.service.AuditService;
import group.g.graduation.backend.common.model.AuditLog.AuditAction;
import group.g.graduation.backend.common.model.AuditLog.AuditStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Audit Controller - التحكم بسجلات التدقيق
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Audit Logs", description = "إدارة سجلات التدقيق - Audit Logs Management")
@SecurityRequirement(name = "bearerAuth")
public class AdminAuditController {
    
    private final AuditService auditService;
    
    // ===================== GET Endpoints =====================
    
    /**
     * جلب جميع سجلات التدقيق مع الترقيم والترتيب
     */
    @GetMapping
    @Operation(summary = "Get all audit logs", description = "جلب جميع سجلات التدقيق مع الترقيم")
    public ResponseEntity<Page<AuditLogResponse>> getAllLogs(
            @Parameter(description = "رقم الصفحة") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "حجم الصفحة") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "الترتيب حسب") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "اتجاه الترتيب") @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return ResponseEntity.ok(auditService.getAllLogs(pageable));
    }
    
    /**
     * جلب سجل تدقيق بالمعرف
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID", description = "جلب سجل تدقيق محدد")
    public ResponseEntity<AuditLogResponse> getLogById(
            @Parameter(description = "معرف السجل") @PathVariable Long id) {
        
        return auditService.getLogById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * جلب سجلات مستخدم معين
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get logs by user", description = "جلب سجلات مستخدم محدد")
    public ResponseEntity<Page<AuditLogResponse>> getLogsByUser(
            @Parameter(description = "معرف المستخدم") @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditService.getLogsByUser(userId, pageable));
    }
    
    /**
     * جلب سجلات عملية معينة
     */
    @GetMapping("/action/{action}")
    @Operation(summary = "Get logs by action", description = "جلب سجلات عملية محددة")
    public ResponseEntity<Page<AuditLogResponse>> getLogsByAction(
            @Parameter(description = "نوع العملية") @PathVariable AuditAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditService.getLogsByAction(action, pageable));
    }
    
    /**
     * جلب سجلات نوع كيان معين
     */
    @GetMapping("/entity-type/{entityType}")
    @Operation(summary = "Get logs by entity type", description = "جلب سجلات نوع كيان محدد")
    public ResponseEntity<Page<AuditLogResponse>> getLogsByEntityType(
            @Parameter(description = "نوع الكيان") @PathVariable String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditService.getLogsByEntityType(entityType.toUpperCase(), pageable));
    }
    
    /**
     * جلب سجلات كيان محدد
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get logs for specific entity", description = "جلب سجلات كيان محدد بمعرفه")
    public ResponseEntity<Page<AuditLogResponse>> getLogsByEntity(
            @Parameter(description = "نوع الكيان") @PathVariable String entityType,
            @Parameter(description = "معرف الكيان") @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditService.getLogsByEntity(entityType.toUpperCase(), entityId, pageable));
    }
    
    /**
     * جلب سجلات فترة زمنية
     */
    @GetMapping("/date-range")
    @Operation(summary = "Get logs by date range", description = "جلب سجلات فترة زمنية محددة")
    public ResponseEntity<Page<AuditLogResponse>> getLogsByDateRange(
            @Parameter(description = "تاريخ البداية (yyyy-MM-dd)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "تاريخ النهاية (yyyy-MM-dd)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditService.getLogsByDateRange(start, end, pageable));
    }
    
    /**
     * بحث متقدم في السجلات
     */
    @GetMapping("/search")
    @Operation(summary = "Advanced search", description = "بحث متقدم في السجلات")
    public ResponseEntity<Page<AuditLogResponse>> searchLogs(
            @Parameter(description = "معرف المستخدم") @RequestParam(required = false) Long userId,
            @Parameter(description = "نوع العملية") @RequestParam(required = false) AuditAction action,
            @Parameter(description = "نوع الكيان") @RequestParam(required = false) String entityType,
            @Parameter(description = "حالة العملية") @RequestParam(required = false) AuditStatus status,
            @Parameter(description = "تاريخ البداية") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "تاريخ النهاية") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Instant start = startDate != null ? 
                startDate.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant end = endDate != null ? 
                endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        String normalizedEntityType = entityType != null ? entityType.toUpperCase() : null;
        
        return ResponseEntity.ok(auditService.searchLogs(
                userId, action, normalizedEntityType, status, start, end, pageable));
    }
    
    // ===================== Statistics Endpoints =====================
    
    /**
     * جلب إحصائيات السجلات
     */
    @GetMapping("/stats")
    @Operation(summary = "Get audit statistics", description = "جلب إحصائيات سجلات التدقيق")
    public ResponseEntity<AuditStatsResponse> getStatistics(
            @Parameter(description = "تاريخ البداية") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "تاريخ النهاية") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Instant start = startDate != null ? 
                startDate.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant end = endDate != null ? 
                endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        
        return ResponseEntity.ok(auditService.getStatistics(start, end));
    }
    
    /**
     * جلب قائمة أنواع العمليات المتاحة
     */
    @GetMapping("/actions")
    @Operation(summary = "Get available actions", description = "جلب قائمة أنواع العمليات")
    public ResponseEntity<List<Map<String, String>>> getAvailableActions() {
        List<Map<String, String>> actions = Arrays.stream(AuditAction.values())
                .map(action -> Map.of(
                        "value", action.name(),
                        "label", action.name(),
                        "labelAr", AuditLogResponse.getActionArabic(action)
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(actions);
    }
    
    /**
     * جلب قائمة أنواع الكيانات المتاحة
     */
    @GetMapping("/entity-types")
    @Operation(summary = "Get available entity types", description = "جلب قائمة أنواع الكيانات")
    public ResponseEntity<List<Map<String, String>>> getAvailableEntityTypes() {
        List<String> entityTypes = List.of(
                "PLANT", "USER", "QUOTE", "MONTH", "TASKTYPE", 
                "PLANTTASK", "PLANTINGQUESTION", "QUESTIONOPTION",
                "PLANTSUITABILITY", "NOTIFICATION", "MONTHPLANT", 
                "PLANTIMAGE", "ROLE", "PERMISSION"
        );
        
        List<Map<String, String>> types = entityTypes.stream()
                .map(type -> Map.of(
                        "value", type,
                        "label", type,
                        "labelAr", AuditLogResponse.getEntityTypeArabic(type)
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(types);
    }
    
    /**
     * جلب قائمة حالات العمليات
     */
    @GetMapping("/statuses")
    @Operation(summary = "Get available statuses", description = "جلب قائمة حالات العمليات")
    public ResponseEntity<List<Map<String, String>>> getAvailableStatuses() {
        List<Map<String, String>> statuses = Arrays.stream(AuditStatus.values())
                .map(status -> Map.of(
                        "value", status.name(),
                        "label", status.name(),
                        "labelAr", AuditLogResponse.getStatusArabic(status)
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(statuses);
    }
    
    // ===================== Cleanup Endpoints =====================
    
    /**
     * حذف السجلات القديمة
     */
    @DeleteMapping("/cleanup")
    @Operation(summary = "Cleanup old logs", description = "حذف السجلات الأقدم من عدد أيام محدد")
    public ResponseEntity<Map<String, Object>> cleanupOldLogs(
            @Parameter(description = "عدد الأيام للاحتفاظ بالسجلات") 
            @RequestParam(defaultValue = "90") int daysToKeep) {
        
        if (daysToKeep < 7) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "يجب الاحتفاظ بالسجلات لمدة 7 أيام على الأقل"
                    ));
        }
        
        long deletedCount = auditService.cleanupOldLogs(daysToKeep);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "تم حذف " + deletedCount + " سجل قديم",
                "deletedCount", deletedCount
        ));
    }
}
