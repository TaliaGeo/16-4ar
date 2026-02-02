package group.g.graduation.backend.common.ratelimit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Rate Limit Exceeded Exception - استثناء تجاوز الحد المسموح
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {
    
    private final long retryAfterSeconds;
    private final int warningCount;
    private final boolean blocked;
    
    public RateLimitExceededException(String message) {
        super(message);
        this.retryAfterSeconds = 60;
        this.warningCount = 0;
        this.blocked = false;
    }
    
    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.warningCount = 0;
        this.blocked = false;
    }
    
    public RateLimitExceededException(String message, long retryAfterSeconds, int warningCount) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.warningCount = warningCount;
        this.blocked = false;
    }
    
    public RateLimitExceededException(String message, boolean blocked) {
        super(message);
        this.retryAfterSeconds = 0;
        this.warningCount = 0;
        this.blocked = blocked;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
    
    public int getWarningCount() {
        return warningCount;
    }
    
    public boolean isBlocked() {
        return blocked;
    }
}
