package group.g.graduation.backend.Security.model;

/**
 * Authentication provider types supported by the application
 */
public enum AuthProvider {
    LOCAL,      // Email/Password registration
    GOOGLE,     // Google OAuth2
    FACEBOOK    // Facebook OAuth2
}
