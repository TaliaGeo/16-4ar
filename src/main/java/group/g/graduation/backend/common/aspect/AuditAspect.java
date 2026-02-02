package group.g.graduation.backend.common.aspect;

import group.g.graduation.backend.admin.service.AuditService;
import group.g.graduation.backend.common.annotation.Auditable;
import group.g.graduation.backend.common.model.AuditLog.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Audit Aspect - تسجيل العمليات تلقائياً باستخدام AOP
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    
    private final AuditService auditService;
    
    // قائمة الـ Endpoints المستثناة من التسجيل التلقائي
    private static final List<String> EXCLUDED_PATTERNS = List.of(
            "audit", "health", "actuator", "swagger", "api-docs"
    );
    
    /**
     * تسجيل جميع عمليات الـ Admin Controllers
     */
    @Around("execution(* group.g.graduation.backend.admin.controller..*.*(..))" +
            " && !execution(* group.g.graduation.backend.admin.controller.AdminAuditController.*(..))")
    public Object auditAdminOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String methodName = method.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // فحص إذا كان هناك @Auditable annotation
        Auditable auditable = method.getAnnotation(Auditable.class);
        if (auditable != null && !auditable.enabled()) {
            // تخطي التسجيل إذا كان معطل
            return joinPoint.proceed();
        }
        
        // تحديد نوع العملية والكيان
        AuditAction action = auditable != null && auditable.action() != AuditAction.READ 
                ? auditable.action() 
                : determineAction(method);
        String entityType = auditable != null && !auditable.entityType().isEmpty() 
                ? auditable.entityType() 
                : determineEntityType(className);
        
        // محاولة استخراج معرف الكيان من المعاملات
        Long entityId = extractEntityId(joinPoint.getArgs());
        
        Object result = null;
        Exception exception = null;
        
        try {
            result = joinPoint.proceed();
            
            // تسجيل العملية الناجحة
            long executionTime = System.currentTimeMillis() - startTime;
            
            // استخراج معرف الكيان من النتيجة إذا كانت عملية إنشاء
            if (action == AuditAction.CREATE && result != null) {
                Long createdId = extractIdFromResult(result);
                if (createdId != null) {
                    entityId = createdId;
                }
            }
            
            // استخدام الوصف المخصص إذا وجد
            String description = auditable != null && !auditable.description().isEmpty()
                    ? auditable.description()
                    : buildDescription(action, entityType, methodName);
                    
            boolean logValues = auditable != null && auditable.logValues();
            
            auditService.logAction(action, entityType, entityId, description, null, 
                    logValues && (action == AuditAction.CREATE || action == AuditAction.UPDATE) ? result : null, 
                    executionTime);
            
            return result;
            
        } catch (Exception e) {
            exception = e;
            long executionTime = System.currentTimeMillis() - startTime;
            
            // تسجيل العملية الفاشلة
            String description = buildDescription(action, entityType, methodName);
            auditService.logFailedAction(action, entityType, entityId, 
                    description + " - فشل", e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * تحديد نوع العملية بناءً على الـ Annotation
     */
    private AuditAction determineAction(Method method) {
        // فحص HTTP Method Annotations
        if (method.isAnnotationPresent(PostMapping.class)) {
            String methodName = method.getName().toLowerCase();
            if (methodName.contains("bulk") || methodName.contains("batch")) {
                return AuditAction.BULK_CREATE;
            }
            if (methodName.contains("upload")) {
                return AuditAction.UPLOAD;
            }
            if (methodName.contains("broadcast") || methodName.contains("send")) {
                return AuditAction.BROADCAST;
            }
            return AuditAction.CREATE;
        }
        
        if (method.isAnnotationPresent(PutMapping.class) || 
            method.isAnnotationPresent(PatchMapping.class)) {
            String methodName = method.getName().toLowerCase();
            if (methodName.contains("bulk") || methodName.contains("batch")) {
                return AuditAction.BULK_UPDATE;
            }
            if (methodName.contains("activate")) {
                return AuditAction.ACTIVATE;
            }
            if (methodName.contains("deactivate")) {
                return AuditAction.DEACTIVATE;
            }
            return AuditAction.UPDATE;
        }
        
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            String methodName = method.getName().toLowerCase();
            if (methodName.contains("bulk") || methodName.contains("batch")) {
                return AuditAction.BULK_DELETE;
            }
            return AuditAction.DELETE;
        }
        
        if (method.isAnnotationPresent(GetMapping.class)) {
            String methodName = method.getName().toLowerCase();
            if (methodName.contains("export")) {
                return AuditAction.EXPORT;
            }
            if (methodName.contains("download")) {
                return AuditAction.DOWNLOAD;
            }
            return AuditAction.READ;
        }
        
        return AuditAction.READ;
    }
    
    /**
     * تحديد نوع الكيان بناءً على اسم الـ Controller
     */
    private String determineEntityType(String className) {
        // إزالة "Admin" و "Controller" من الاسم
        String entityType = className
                .replace("Admin", "")
                .replace("Controller", "")
                .toUpperCase();
        
        // معالجة حالات خاصة
        if (entityType.equals("DASHBOARD")) return "DASHBOARD";
        if (entityType.equals("USERS")) return "USER";
        if (entityType.equals("PLANTS")) return "PLANT";
        
        return entityType;
    }
    
    /**
     * استخراج معرف الكيان من المعاملات
     */
    private Long extractEntityId(Object[] args) {
        if (args == null || args.length == 0) return null;
        
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
            if (arg instanceof Integer) {
                return ((Integer) arg).longValue();
            }
        }
        
        return null;
    }
    
    /**
     * استخراج المعرف من نتيجة العملية
     */
    private Long extractIdFromResult(Object result) {
        if (result == null) return null;
        
        try {
            // محاولة الحصول على getId()
            Method getIdMethod = result.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(result);
            if (id instanceof Long) {
                return (Long) id;
            }
            if (id instanceof Integer) {
                return ((Integer) id).longValue();
            }
        } catch (Exception e) {
            // تجاهل إذا لم يوجد getId
        }
        
        return null;
    }
    
    /**
     * بناء وصف العملية
     */
    private String buildDescription(AuditAction action, String entityType, String methodName) {
        String entityAr = getEntityArabic(entityType);
        
        return switch (action) {
            case CREATE -> "إنشاء " + entityAr + " جديد";
            case READ -> "عرض " + entityAr;
            case UPDATE -> "تحديث " + entityAr;
            case DELETE -> "حذف " + entityAr;
            case BULK_CREATE -> "إنشاء متعدد لـ " + entityAr;
            case BULK_UPDATE -> "تحديث متعدد لـ " + entityAr;
            case BULK_DELETE -> "حذف متعدد لـ " + entityAr;
            case UPLOAD -> "رفع ملف لـ " + entityAr;
            case DOWNLOAD -> "تحميل ملف من " + entityAr;
            case EXPORT -> "تصدير " + entityAr;
            case IMPORT -> "استيراد " + entityAr;
            case ACTIVATE -> "تفعيل " + entityAr;
            case DEACTIVATE -> "تعطيل " + entityAr;
            case BROADCAST -> "بث إشعار";
            case SEND_NOTIFICATION -> "إرسال إشعار";
            default -> methodName;
        };
    }
    
    /**
     * ترجمة نوع الكيان للعربية
     */
    private String getEntityArabic(String entityType) {
        if (entityType == null) return "عنصر";
        
        return switch (entityType.toUpperCase()) {
            case "PLANT" -> "نبتة";
            case "USER" -> "مستخدم";
            case "QUOTE" -> "اقتباس";
            case "MONTH" -> "شهر";
            case "TASKTYPE" -> "نوع مهمة";
            case "PLANTTASK" -> "مهمة نبتة";
            case "PLANTINGQUESTION" -> "سؤال زراعي";
            case "QUESTIONOPTION" -> "خيار سؤال";
            case "PLANTSUITABILITY" -> "ملاءمة نبتة";
            case "NOTIFICATION" -> "إشعار";
            case "MONTHPLANT" -> "نبتة-شهر";
            case "PLANTIMAGE" -> "صورة نبتة";
            case "DASHBOARD" -> "لوحة التحكم";
            case "ROLE" -> "دور";
            case "PERMISSION" -> "صلاحية";
            default -> entityType;
        };
    }
}
