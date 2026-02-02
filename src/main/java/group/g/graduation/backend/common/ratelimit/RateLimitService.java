package group.g.graduation.backend.common.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limit Service - خدمة تحديد معدل الطلبات
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final RateLimitConfig config;
    
    // Cache للـ Buckets (IP/User -> Bucket)
    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(100_000)
            .build();
    
    // تتبع التحذيرات لكل IP
    private final Map<String, Integer> warningCounts = new ConcurrentHashMap<>();
    
    // تتبع IPs المحظورة مؤقتاً
    private final Map<String, Instant> blockedIps = new ConcurrentHashMap<>();
    
    // تتبع إحصائيات الطلبات
    private final Map<String, RateLimitStats> statsMap = new ConcurrentHashMap<>();
    
    /**
     * التحقق من Rate Limit وإرجاع النتيجة
     */
    public RateLimitResult checkRateLimit(String key, int requestsPerMinute) {
        // تنظيف IPs المحظورة منتهية الصلاحية
        cleanupExpiredBlocks();
        
        // التحقق من القائمة البيضاء
        if (config.isWhitelisted(key)) {
            return RateLimitResult.allowed();
        }
        
        // التحقق من القائمة السوداء
        if (config.isBlacklisted(key)) {
            return RateLimitResult.blocked("IP محظور بشكل دائم");
        }
        
        // التحقق من الحظر المؤقت
        if (isTemporarilyBlocked(key)) {
            Instant unblockTime = blockedIps.get(key);
            long remainingSeconds = Duration.between(Instant.now(), unblockTime).getSeconds();
            return RateLimitResult.blocked("محظور مؤقتاً. الرجاء الانتظار " + remainingSeconds + " ثانية");
        }
        
        // الحصول على Bucket أو إنشاء جديد
        Bucket bucket = bucketCache.get(key, k -> createBucket(requestsPerMinute));
        
        // محاولة استهلاك token
        if (bucket.tryConsume(1)) {
            updateStats(key, true);
            return RateLimitResult.allowed(bucket.getAvailableTokens());
        } else {
            // تسجيل تحذير
            int warnings = warningCounts.merge(key, 1, Integer::sum);
            updateStats(key, false);
            
            // حظر مؤقت إذا تجاوز عدد التحذيرات
            if (warnings >= config.getWarningsBeforeBlock()) {
                blockTemporarily(key);
                warningCounts.remove(key);
                log.warn("🚫 IP {} تم حظره مؤقتاً بعد {} تحذيرات", key, warnings);
                return RateLimitResult.blocked("تم حظرك مؤقتاً لتجاوز الحد المسموح");
            }
            
            long waitTime = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
            return RateLimitResult.limited(warnings, waitTime);
        }
    }
    
    /**
     * التحقق من Rate Limit مع إعدادات مخصصة
     */
    public RateLimitResult checkRateLimit(String key, int requests, int durationSeconds) {
        String bucketKey = key + ":" + requests + ":" + durationSeconds;
        
        Bucket bucket = bucketCache.get(bucketKey, k -> createCustomBucket(requests, durationSeconds));
        
        if (bucket.tryConsume(1)) {
            return RateLimitResult.allowed(bucket.getAvailableTokens());
        } else {
            long waitTime = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
            return RateLimitResult.limited(0, waitTime);
        }
    }
    
    /**
     * إنشاء Bucket جديد
     */
    private Bucket createBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.classic(
                requestsPerMinute,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
    /**
     * إنشاء Bucket مخصص
     */
    private Bucket createCustomBucket(int requests, int durationSeconds) {
        Bandwidth limit = Bandwidth.classic(
                requests,
                Refill.greedy(requests, Duration.ofSeconds(durationSeconds))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
    /**
     * حظر IP مؤقتاً
     */
    public void blockTemporarily(String key) {
        Instant unblockTime = Instant.now().plusSeconds(config.getBlockDurationSeconds());
        blockedIps.put(key, unblockTime);
        log.info("⛔ تم حظر {} حتى {}", key, unblockTime);
    }
    
    /**
     * التحقق من الحظر المؤقت
     */
    public boolean isTemporarilyBlocked(String key) {
        Instant unblockTime = blockedIps.get(key);
        if (unblockTime == null) {
            return false;
        }
        if (Instant.now().isAfter(unblockTime)) {
            blockedIps.remove(key);
            return false;
        }
        return true;
    }
    
    /**
     * إلغاء الحظر
     */
    public void unblock(String key) {
        blockedIps.remove(key);
        warningCounts.remove(key);
        bucketCache.invalidate(key);
        log.info("✅ تم إلغاء حظر {}", key);
    }
    
    /**
     * تنظيف الحظر المنتهي
     */
    private void cleanupExpiredBlocks() {
        Instant now = Instant.now();
        blockedIps.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
    
    /**
     * تحديث الإحصائيات
     */
    private void updateStats(String key, boolean allowed) {
        statsMap.compute(key, (k, stats) -> {
            if (stats == null) {
                stats = new RateLimitStats();
            }
            stats.incrementTotal();
            if (allowed) {
                stats.incrementAllowed();
            } else {
                stats.incrementBlocked();
            }
            return stats;
        });
    }
    
    /**
     * الحصول على الإحصائيات
     */
    public RateLimitStats getStats(String key) {
        return statsMap.getOrDefault(key, new RateLimitStats());
    }
    
    /**
     * الحصول على جميع الإحصائيات
     */
    public Map<String, RateLimitStats> getAllStats() {
        return Map.copyOf(statsMap);
    }
    
    /**
     * الحصول على قائمة IPs المحظورة
     */
    public Map<String, Instant> getBlockedIps() {
        cleanupExpiredBlocks();
        return Map.copyOf(blockedIps);
    }
    
    /**
     * مسح الإحصائيات
     */
    public void clearStats() {
        statsMap.clear();
    }
    
    /**
     * إعادة تعيين Bucket
     */
    public void resetBucket(String key) {
        bucketCache.invalidate(key);
        warningCounts.remove(key);
    }
}
