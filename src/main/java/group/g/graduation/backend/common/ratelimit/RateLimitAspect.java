package group.g.graduation.backend.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * Rate Limit Aspect - للتحكم بـ Rate Limit على مستوى الـ Method
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final RateLimitService rateLimitService;
    private final RateLimitConfig config;
    
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // التحقق من تفعيل Rate Limiting
        if (!config.isEnabled()) {
            return joinPoint.proceed();
        }
        
        // تخطي للمشرفين إذا كان مطلوباً
        if (rateLimit.skipForAdmin() && isAdmin()) {
            return joinPoint.proceed();
        }
        
        // الحصول على المفتاح
        String key = buildKey(joinPoint, rateLimit);
        
        // فحص Rate Limit
        RateLimitResult result = rateLimitService.checkRateLimit(
                key, 
                rateLimit.requests(), 
                rateLimit.duration()
        );
        
        if (result.isDenied()) {
            log.warn("🚫 Rate limit exceeded for method: {}, Key: {}", 
                    joinPoint.getSignature().getName(), key);
            throw new RateLimitExceededException(
                    rateLimit.message(),
                    result.getRetryAfterSeconds(),
                    result.getWarningCount()
            );
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * بناء المفتاح بناءً على نوع الـ Key المحدد
     */
    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String ip = getClientIp();
        String userId = getCurrentUserId();
        String endpoint = getEndpointName(joinPoint);
        
        return switch (rateLimit.key()) {
            case IP -> ip;
            case USER -> userId != null ? userId : ip;
            case ENDPOINT -> endpoint;
            case IP_ENDPOINT -> ip + ":" + endpoint;
            case USER_ENDPOINT -> (userId != null ? userId : ip) + ":" + endpoint;
        };
    }
    
    /**
     * الحصول على IP العميل
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not get client IP", e);
        }
        return "unknown";
    }
    
    /**
     * الحصول على معرف المستخدم الحالي
     */
    private String getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.debug("Could not get current user", e);
        }
        return null;
    }
    
    /**
     * الحصول على اسم الـ Endpoint
     */
    private String getEndpointName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className + "." + method.getName();
    }
    
    /**
     * التحقق إذا كان المستخدم مشرف
     */
    private boolean isAdmin() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            }
        } catch (Exception e) {
            log.debug("Could not check admin status", e);
        }
        return false;
    }
}
