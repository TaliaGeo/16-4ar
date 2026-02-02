package group.g.graduation.backend.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Rate Limit Filter - فلتر تحديد معدل الطلبات
 */
@Slf4j
@Component
@Order(1) // يتم تنفيذه قبل فلاتر الأمان
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RateLimitService rateLimitService;
    private final RateLimitConfig config;
    
    // Endpoints مستثناة من Rate Limiting
    private static final String[] EXCLUDED_PATHS = {
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/actuator",
            "/health",
            "/favicon.ico"
    };
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // التحقق من تفعيل Rate Limiting
        if (!config.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // تخطي المسارات المستثناة
        String path = request.getRequestURI();
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // الحصول على IP العميل
        String clientIp = getClientIp(request);
        
        // التحقق من القائمة البيضاء
        if (config.isWhitelisted(clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // تحديد نوع المستخدم وحد الطلبات
        String userType = getUserType();
        int requestsPerMinute = config.getRequestsPerMinuteForUserType(userType);
        
        // مفتاح Rate Limiting
        String rateLimitKey = buildRateLimitKey(clientIp, path, userType);
        
        // فحص Rate Limit
        RateLimitResult result = rateLimitService.checkRateLimit(rateLimitKey, requestsPerMinute);
        
        // إضافة Headers
        addRateLimitHeaders(response, result, requestsPerMinute);
        
        if (result.isAllowed()) {
            filterChain.doFilter(request, response);
        } else {
            handleRateLimitExceeded(request, response, result);
        }
    }
    
    /**
     * التحقق من المسارات المستثناة
     */
    private boolean isExcludedPath(String path) {
        for (String excluded : EXCLUDED_PATHS) {
            if (path.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * الحصول على IP العميل (يدعم Proxy)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * الحصول على نوع المستخدم
     */
    private String getUserType() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return "ANONYMOUS";
        }
        
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        return isAdmin ? "ADMIN" : "USER";
    }
    
    /**
     * بناء مفتاح Rate Limiting
     */
    private String buildRateLimitKey(String ip, String path, String userType) {
        // يمكن تخصيصه حسب الحاجة
        return ip + ":" + userType;
    }
    
    /**
     * إضافة Rate Limit Headers للاستجابة
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result, int limit) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemainingTokens()));
        
        if (result.isDenied()) {
            response.setHeader("X-RateLimit-Reset", String.valueOf(Instant.now().plusSeconds(result.getRetryAfterSeconds()).getEpochSecond()));
            response.setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
        }
    }
    
    /**
     * معالجة تجاوز Rate Limit
     */
    private void handleRateLimitExceeded(HttpServletRequest request, 
                                          HttpServletResponse response, 
                                          RateLimitResult result) throws IOException {
        
        String clientIp = getClientIp(request);
        log.warn("⚠️ Rate limit exceeded - IP: {}, Path: {}, Blocked: {}", 
                clientIp, request.getRequestURI(), result.isBlocked());
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format("""
            {
                "timestamp": "%s",
                "status": 429,
                "error": "Too Many Requests",
                "message": "%s",
                "messageAr": "%s",
                "path": "%s",
                "retryAfter": %d,
                "blocked": %b,
                "warningCount": %d
            }
            """,
                Instant.now().toString(),
                "Rate limit exceeded. Please try again later.",
                result.getMessage() != null ? result.getMessage() : "لقد تجاوزت الحد المسموح من الطلبات",
                request.getRequestURI(),
                result.getRetryAfterSeconds(),
                result.isBlocked(),
                result.getWarningCount()
        );
        
        response.getWriter().write(jsonResponse);
    }
}
