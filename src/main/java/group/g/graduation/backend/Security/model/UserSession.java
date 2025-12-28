package group.g.graduation.backend.Security.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity to track user login sessions across multiple devices.
 * Allows users to view all active sessions and logout from specific devices.
 */
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_session_user_id", columnList = "user_id"),
    @Index(name = "idx_user_session_token", columnList = "token_id"),
    @Index(name = "idx_user_session_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_id", nullable = false, unique = true)
    private String tokenId; // Unique identifier for the JWT token

    @Column(name = "device_name")
    private String deviceName; // e.g., "iPhone 15 Pro", "Samsung Galaxy S24"

    @Column(name = "device_type")
    private String deviceType; // MOBILE, TABLET, DESKTOP, UNKNOWN

    @Column(name = "operating_system")
    private String operatingSystem; // iOS, Android, Windows, macOS, Linux

    @Column(name = "browser")
    private String browser; // Chrome, Safari, Firefox, Mobile App

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "location")
    private String location; // City, Country (based on IP)

    @Column(name = "login_time", nullable = false)
    private Instant loginTime;

    @Column(name = "last_activity")
    private Instant lastActivity;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "logged_out_at")
    private Instant loggedOutAt;

    @Column(name = "is_current_session")
    @Builder.Default
    private boolean currentSession = false;

    /**
     * Mark this session as inactive (logged out)
     */
    public void logout() {
        this.active = false;
        this.loggedOutAt = Instant.now();
    }

    /**
     * Update last activity timestamp
     */
    public void updateActivity() {
        this.lastActivity = Instant.now();
    }

    /**
     * Check if session is expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
