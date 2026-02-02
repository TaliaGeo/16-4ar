package group.g.graduation.backend.common.annotation;

import group.g.graduation.backend.common.model.AuditLog.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Auditable Annotation - للتحكم في تسجيل العمليات
 * 
 * يستخدم هذا الـ Annotation لتعطيل التسجيل التلقائي أو تحديد نوع العملية يدوياً
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    
    /**
     * نوع العملية
     */
    AuditAction action() default AuditAction.READ;
    
    /**
     * نوع الكيان
     */
    String entityType() default "";
    
    /**
     * وصف العملية
     */
    String description() default "";
    
    /**
     * هل يجب تسجيل هذه العملية؟
     */
    boolean enabled() default true;
    
    /**
     * هل يجب تسجيل القيم القديمة والجديدة؟
     */
    boolean logValues() default false;
}
