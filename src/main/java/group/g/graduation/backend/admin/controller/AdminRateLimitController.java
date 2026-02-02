package group.g.graduation.backend.admin.controller;

import group.g.graduation.backend.common.ratelimit.RateLimitConfig;
import group.g.graduation.backend.common.ratelimit.RateLimitService;
import group.g.graduation.backend.common.ratelimit.RateLimitStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Rate Limit Controller - إدارة Rate Limiting للأدمن
 */
@RestController
@RequestMapping("/api/admin/rate-limit")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Rate Limiting", description = "إدارة تحديد معدل الطلبات - Rate Limit Management")
@SecurityRequirement(name = "bearerAuth")
public class AdminRateLimitController {
    
    private final RateLimitService rateLimitService;
    private final RateLimitConfig rateLimitConfig;
    
    // ===================== Configuration =====================
    
    @GetMapping("/config")
    @Operation(summary = "جلب إعدادات Rate Limit", description = "Get current rate limit configuration")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", rateLimitConfig.isEnabled());
        config.put("defaultRequestsPerMinute", rateLimitConfig.getDefaultRequestsPerMinute());
        config.put("defaultRequestsPerHour", rateLimitConfig.getDefaultRequestsPerHour());
        config.put("blockDurationSeconds", rateLimitConfig.getBlockDurationSeconds());
        config.put("warningsBeforeBlock", rateLimitConfig.getWarningsBeforeBlock());
        config.put("whitelistedIps", rateLimitConfig.getWhitelistedIps());
        config.put("blacklistedIps", rateLimitConfig.getBlacklistedIps());
        config.put("userTypeLimits", Map.of(
                "anonymous", rateLimitConfig.getUserTypeLimits().getAnonymousRequestsPerMinute(),
                "user", rateLimitConfig.getUserTypeLimits().getUserRequestsPerMinute(),
                "admin", rateLimitConfig.getUserTypeLimits().getAdminRequestsPerMinute()
        ));
        return ResponseEntity.ok(config);
    }
    
    // ===================== Statistics =====================
    
    @GetMapping("/stats")
    @Operation(summary = "إحصائيات Rate Limit", description = "Get rate limit statistics for all IPs")
    public ResponseEntity<Map<String, Object>> getAllStatistics() {
        Map<String, RateLimitStats> allStats = rateLimitService.getAllStats();
        
        // تحويل الإحصائيات لتنسيق مناسب
        List<Map<String, Object>> statsList = allStats.entrySet().stream()
                .map(entry -> {
                    RateLimitStats stats = entry.getValue();
                    Map<String, Object> statsMap = new HashMap<>();
                    statsMap.put("key", entry.getKey());
                    statsMap.put("totalRequests", stats.getTotal());
                    statsMap.put("allowedRequests", stats.getAllowed());
                    statsMap.put("blockedRequests", stats.getBlocked());
                    statsMap.put("blockRate", String.format("%.2f%%", stats.getBlockRate()));
                    statsMap.put("firstRequestTime", stats.getFirstRequestTime());
                    statsMap.put("lastRequestTime", stats.getLastRequestTime());
                    return statsMap;
                })
                .collect(Collectors.toList());
        
        // إحصائيات إجمالية
        long totalRequests = allStats.values().stream().mapToLong(RateLimitStats::getTotal).sum();
        long totalAllowed = allStats.values().stream().mapToLong(RateLimitStats::getAllowed).sum();
        long totalBlocked = allStats.values().stream().mapToLong(RateLimitStats::getBlocked).sum();
        
        Map<String, Object> response = new HashMap<>();
        response.put("summary", Map.of(
                "totalUniqueKeys", allStats.size(),
                "totalRequests", totalRequests,
                "totalAllowed", totalAllowed,
                "totalBlocked", totalBlocked,
                "overallBlockRate", totalRequests > 0 ? 
                        String.format("%.2f%%", (double) totalBlocked / totalRequests * 100) : "0%"
        ));
        response.put("details", statsList);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/summary")
    @Operation(summary = "ملخص إحصائيات Rate Limit", description = "Get summary of rate limit statistics (alias for /stats)")
    public ResponseEntity<?> getSummary() {
        try {
            log.info("Getting rate limit summary");
            Map<String, Object> result = getAllStatistics().getBody();
            log.info("Successfully retrieved rate limit summary");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting rate limit summary: {}", e.getMessage(), e);
            Map<String, Object> error = Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "Error retrieving rate limit summary: " + e.getMessage(),
                "timestamp", Instant.now(),
                "details", e.getClass().getSimpleName()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/stats/{key}")
    @Operation(summary = "إحصائيات IP/User محدد", description = "Get rate limit statistics for specific IP or user")
    public ResponseEntity<Map<String, Object>> getStatistics(@PathVariable String key) {
        RateLimitStats stats = rateLimitService.getStats(key);
        
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("totalRequests", stats.getTotal());
        response.put("allowedRequests", stats.getAllowed());
        response.put("blockedRequests", stats.getBlocked());
        response.put("blockRate", String.format("%.2f%%", stats.getBlockRate()));
        response.put("firstRequestTime", stats.getFirstRequestTime());
        response.put("lastRequestTime", stats.getLastRequestTime());
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/stats")
    @Operation(summary = "مسح الإحصائيات", description = "Clear all rate limit statistics")
    public ResponseEntity<Map<String, String>> clearStatistics() {
        rateLimitService.clearStats();
        return ResponseEntity.ok(Map.of(
                "message", "تم مسح جميع الإحصائيات",
                "messageEn", "All statistics cleared"
        ));
    }
    
    // ===================== Blocked IPs =====================
    
    @GetMapping("/blocked")
    @Operation(summary = "قائمة IPs المحظورة", description = "Get list of temporarily blocked IPs")
    public ResponseEntity<Map<String, Object>> getBlockedIps() {
        Map<String, Instant> blockedIps = rateLimitService.getBlockedIps();
        
        List<Map<String, Object>> blockedList = blockedIps.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "ip", entry.getKey(),
                        "unblockTime", entry.getValue(),
                        "remainingSeconds", java.time.Duration.between(Instant.now(), entry.getValue()).getSeconds()
                ))
                .filter(m -> (long) m.get("remainingSeconds") > 0)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
                "count", blockedList.size(),
                "blockedIps", blockedList
        ));
    }
    
    @PostMapping("/block/{ip}")
    @Operation(summary = "حظر IP مؤقتاً", description = "Temporarily block an IP address")
    public ResponseEntity<Map<String, String>> blockIp(@PathVariable String ip) {
        rateLimitService.blockTemporarily(ip);
        return ResponseEntity.ok(Map.of(
                "message", "تم حظر " + ip + " مؤقتاً",
                "messageEn", "IP " + ip + " has been temporarily blocked",
                "duration", rateLimitConfig.getBlockDurationSeconds() + " seconds"
        ));
    }
    
    @DeleteMapping("/block/{ip}")
    @Operation(summary = "إلغاء حظر IP", description = "Unblock an IP address")
    public ResponseEntity<Map<String, String>> unblockIp(@PathVariable String ip) {
        rateLimitService.unblock(ip);
        return ResponseEntity.ok(Map.of(
                "message", "تم إلغاء حظر " + ip,
                "messageEn", "IP " + ip + " has been unblocked"
        ));
    }
    
    // ===================== Reset =====================
    
    @PostMapping("/reset/{key}")
    @Operation(summary = "إعادة تعيين Rate Limit", description = "Reset rate limit bucket for specific key")
    public ResponseEntity<Map<String, String>> resetRateLimit(@PathVariable String key) {
        rateLimitService.resetBucket(key);
        return ResponseEntity.ok(Map.of(
                "message", "تم إعادة تعيين Rate Limit لـ " + key,
                "messageEn", "Rate limit reset for " + key
        ));
    }
    
    // ===================== Test Endpoint =====================
    
    @GetMapping("/test")
    @Operation(summary = "اختبار Rate Limit", description = "Test endpoint to verify rate limiting is working")
    public ResponseEntity<Map<String, Object>> testRateLimit() {
        return ResponseEntity.ok(Map.of(
                "message", "Rate Limit يعمل بشكل صحيح",
                "messageEn", "Rate limiting is working correctly",
                "timestamp", Instant.now(),
                "enabled", rateLimitConfig.isEnabled()
        ));
    }
    
    @GetMapping("/test/{ip}")
    @Operation(summary = "اختبار Rate Limit لـ IP محدد", description = "Test rate limit status for specific IP")
    public ResponseEntity<?> testRateLimitForIp(@PathVariable String ip) {
        try {
            log.info("Testing rate limit for IP: {}", ip);
            RateLimitStats stats = rateLimitService.getStats(ip);
            
            Map<String, Object> result = Map.of(
                    "message", "Rate Limit يعمل بشكل صحيح لـ IP: " + ip,
                    "messageEn", "Rate limiting is working correctly for IP: " + ip,
                    "ip", ip,
                    "timestamp", Instant.now(),
                    "enabled", rateLimitConfig.isEnabled(),
                    "stats", Map.of(
                            "totalRequests", stats.getTotal(),
                            "allowedRequests", stats.getAllowed(),
                            "blockedRequests", stats.getBlocked(),
                            "blockRate", String.format("%.2f%%", stats.getBlockRate())
                    )
            );
            
            log.info("Successfully tested rate limit for IP: {}", ip);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error testing rate limit for IP {}: {}", ip, e.getMessage(), e);
            Map<String, Object> error = Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "Error testing rate limit for IP " + ip + ": " + e.getMessage(),
                "timestamp", Instant.now(),
                "details", e.getClass().getSimpleName()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
