package group.g.graduation.backend.common.ratelimit;

import lombok.Data;

/**
 * Rate Limit Result - نتيجة فحص Rate Limit
 */
@Data
public class RateLimitResult {
    
    private boolean allowed;
    private boolean blocked;
    private String message;
    private long remainingTokens;
    private long retryAfterSeconds;
    private int warningCount;
    
    private RateLimitResult() {}
    
    /**
     * طلب مسموح
     */
    public static RateLimitResult allowed() {
        RateLimitResult result = new RateLimitResult();
        result.allowed = true;
        result.blocked = false;
        return result;
    }
    
    /**
     * طلب مسموح مع عدد التوكنات المتبقية
     */
    public static RateLimitResult allowed(long remainingTokens) {
        RateLimitResult result = allowed();
        result.remainingTokens = remainingTokens;
        return result;
    }
    
    /**
     * طلب محدود (تجاوز الحد)
     */
    public static RateLimitResult limited(int warningCount, long retryAfterSeconds) {
        RateLimitResult result = new RateLimitResult();
        result.allowed = false;
        result.blocked = false;
        result.warningCount = warningCount;
        result.retryAfterSeconds = retryAfterSeconds;
        result.message = "تجاوزت الحد المسموح. الرجاء الانتظار " + retryAfterSeconds + " ثانية";
        return result;
    }
    
    /**
     * طلب محظور
     */
    public static RateLimitResult blocked(String message) {
        RateLimitResult result = new RateLimitResult();
        result.allowed = false;
        result.blocked = true;
        result.message = message;
        return result;
    }
    
    /**
     * هل الطلب مرفوض؟
     */
    public boolean isDenied() {
        return !allowed;
    }
}
