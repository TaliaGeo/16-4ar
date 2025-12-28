package group.g.graduation.backend.Security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when there's an issue with refresh token processing
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenRefreshException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final String token;
    
    /**
     * Constructor with token and message
     * 
     * @param token The refresh token that caused the exception
     * @param message The error message
     */
    public TokenRefreshException(String token, String message) {
        super(String.format("Failed for [%s]: %s", token, message));
        this.token = token;
    }
    
    /**
     * Constructor with token, message and cause
     * 
     * @param token The refresh token that caused the exception
     * @param message The error message
     * @param cause The underlying cause
     */
    public TokenRefreshException(String token, String message, Throwable cause) {
        super(String.format("Failed for [%s]: %s", token, message), cause);
        this.token = token;
    }
    
    /**
     * Get the token that caused this exception
     * 
     * @return The refresh token
     */
    public String getToken() {
        return token;
    }
    
    /**
     * Create exception for expired token
     * 
     * @param token The expired token
     * @return TokenRefreshException instance
     */
    public static TokenRefreshException expired(String token) {
        return new TokenRefreshException(token, "Refresh token was expired. Please make a new login request");
    }
    
    /**
     * Create exception for revoked token
     * 
     * @param token The revoked token
     * @return TokenRefreshException instance
     */
    public static TokenRefreshException revoked(String token) {
        return new TokenRefreshException(token, "Refresh token was revoked. Please make a new login request");
    }
    
    /**
     * Create exception for invalid token
     * 
     * @param token The invalid token
     * @return TokenRefreshException instance
     */
    public static TokenRefreshException invalid(String token) {
        return new TokenRefreshException(token, "Refresh token is invalid. Please make a new login request");
    }
    
    /**
     * Create exception for not found token
     * 
     * @param token The token that was not found
     * @return TokenRefreshException instance
     */
    public static TokenRefreshException notFound(String token) {
        return new TokenRefreshException(token, "Refresh token not found. Please make a new login request");
    }
}