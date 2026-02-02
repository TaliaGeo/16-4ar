package group.g.graduation.backend.common.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limit Configuration - إعدادات تحديد معدل الطلبات
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {
    
    /**
     * تفعيل/تعطيل Rate Limiting
     */
    private boolean enabled = true;
    
    /**
     * الحد الافتراضي للطلبات في الدقيقة
     */
    private int defaultRequestsPerMinute = 60;
    
    /**
     * الحد الافتراضي للطلبات في الساعة
     */
    private int defaultRequestsPerHour = 1000;
    
    /**
     * مدة حظر IP بعد تجاوز الحد (بالثواني)
     */
    private int blockDurationSeconds = 300; // 5 minutes
    
    /**
     * عدد التحذيرات قبل الحظر
     */
    private int warningsBeforeBlock = 3;
    
    /**
     * إعدادات مخصصة لكل endpoint
     */
    private Map<String, EndpointRateLimit> endpoints = new HashMap<>();
    
    /**
     * قائمة IPs المستثناة (whitelist)
     */
    private String[] whitelistedIps = {"127.0.0.1", "::1"};
    
    /**
     * قائمة IPs المحظورة (blacklist)
     */
    private String[] blacklistedIps = {};
    
    /**
     * حدود مخصصة لأنواع المستخدمين
     */
    private UserTypeRateLimits userTypeLimits = new UserTypeRateLimits();
    
    @Data
    public static class EndpointRateLimit {
        private int requestsPerMinute = 30;
        private int requestsPerHour = 500;
        private boolean strictMode = false;
    }
    
    @Data
    public static class UserTypeRateLimits {
        private int anonymousRequestsPerMinute = 30;
        private int userRequestsPerMinute = 60;
        private int adminRequestsPerMinute = 200;
    }
    
    /**
     * الحصول على حد الطلبات حسب نوع المستخدم
     */
    public int getRequestsPerMinuteForUserType(String userType) {
        return switch (userType.toUpperCase()) {
            case "ADMIN" -> userTypeLimits.getAdminRequestsPerMinute();
            case "USER" -> userTypeLimits.getUserRequestsPerMinute();
            default -> userTypeLimits.getAnonymousRequestsPerMinute();
        };
    }
    
    /**
     * التحقق إذا كان IP في القائمة البيضاء
     */
    public boolean isWhitelisted(String ip) {
        for (String whitelistedIp : whitelistedIps) {
            if (whitelistedIp.equals(ip)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * التحقق إذا كان IP في القائمة السوداء
     */
    public boolean isBlacklisted(String ip) {
        for (String blacklistedIp : blacklistedIps) {
            if (blacklistedIp.equals(ip)) {
                return true;
            }
        }
        return false;
    }
}
