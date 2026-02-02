package group.g.graduation.backend.common.ratelimit;

import lombok.Data;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate Limit Stats - إحصائيات Rate Limiting
 */
@Data
public class RateLimitStats {
    
    private AtomicLong totalRequests = new AtomicLong(0);
    private AtomicLong allowedRequests = new AtomicLong(0);
    private AtomicLong blockedRequests = new AtomicLong(0);
    private Instant firstRequestTime = Instant.now();
    private Instant lastRequestTime = Instant.now();
    
    public void incrementTotal() {
        totalRequests.incrementAndGet();
        lastRequestTime = Instant.now();
    }
    
    public void incrementAllowed() {
        allowedRequests.incrementAndGet();
    }
    
    public void incrementBlocked() {
        blockedRequests.incrementAndGet();
    }
    
    public long getTotal() {
        return totalRequests.get();
    }
    
    public long getAllowed() {
        return allowedRequests.get();
    }
    
    public long getBlocked() {
        return blockedRequests.get();
    }
    
    public double getBlockRate() {
        long total = totalRequests.get();
        if (total == 0) return 0.0;
        return (double) blockedRequests.get() / total * 100;
    }
}
