package group.g.graduation.backend.Security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when OAuth2 authentication processing fails
 */
public class OAuth2AuthenticationProcessingException extends AuthenticationException {
    
    public OAuth2AuthenticationProcessingException(String msg) {
        super(msg);
    }
    
    public OAuth2AuthenticationProcessingException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
