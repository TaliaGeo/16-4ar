package group.g.graduation.backend.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate Limit Annotation - للتحكم بمعدل الطلبات على مستوى الـ Method أو Class
 * 
 * استخدام:
 * @RateLimit(requests = 10, duration = 60) // 10 طلبات في الدقيقة
 * @RateLimit(requests = 5, duration = 1, timeUnit = TimeUnit.SECONDS) // 5 طلبات في الثانية
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * عدد الطلبات المسموح بها
     */
    int requests() default 60;
    
    /**
     * المدة الزمنية بالثواني
     */
    int duration() default 60;
    
    /**
     * مفتاح التجميع (IP, USER, ENDPOINT)
     */
    RateLimitKey key() default RateLimitKey.IP;
    
    /**
     * رسالة الخطأ المخصصة
     */
    String message() default "لقد تجاوزت الحد المسموح من الطلبات. الرجاء المحاولة لاحقاً.";
    
    /**
     * تخطي Rate Limiting للمشرفين
     */
    boolean skipForAdmin() default true;
    
    enum RateLimitKey {
        IP,           // تحديد بناءً على IP
        USER,         // تحديد بناءً على المستخدم
        ENDPOINT,     // تحديد بناءً على الـ endpoint
        IP_ENDPOINT,  // تحديد بناءً على IP + endpoint
        USER_ENDPOINT // تحديد بناءً على المستخدم + endpoint
    }
}
