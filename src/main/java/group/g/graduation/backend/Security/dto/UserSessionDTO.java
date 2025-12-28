package group.g.graduation.backend.Security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for displaying user session/device information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionDTO {
    
    private Long id;
    private String deviceName;
    private String deviceType;
    private String operatingSystem;
    private String browser;
    private String ipAddress;
    private String location;
    private Instant loginTime;
    private Instant lastActivity;
    private Instant expiresAt;
    private boolean active;
    private boolean currentSession;
    
    /**
     * Human-readable device description
     */
    public String getDeviceDescription() {
        StringBuilder sb = new StringBuilder();
        if (deviceName != null && !deviceName.isEmpty()) {
            sb.append(deviceName);
        } else {
            sb.append(deviceType != null ? deviceType : "Unknown Device");
        }
        if (operatingSystem != null && !operatingSystem.isEmpty()) {
            sb.append(" (").append(operatingSystem).append(")");
        }
        return sb.toString();
    }
}
