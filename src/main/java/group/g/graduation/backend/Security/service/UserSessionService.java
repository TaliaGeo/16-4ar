package group.g.graduation.backend.Security.service;

import group.g.graduation.backend.Security.dto.UserSessionDTO;
import group.g.graduation.backend.Security.model.UserSession;
import group.g.graduation.backend.Security.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final UserSessionRepository sessionRepository;

    /**
     * Create a new session when user logs in
     */
    @Transactional
    public UserSession createSession(Long userId, long expirationMs, HttpServletRequest request) {
        String tokenId = UUID.randomUUID().toString();
        
        // Parse device info from User-Agent
        String userAgent = request.getHeader("User-Agent");
        DeviceInfo deviceInfo = parseUserAgent(userAgent);
        
        // Get IP address
        String ipAddress = getClientIpAddress(request);
        
        UserSession session = UserSession.builder()
                .userId(userId)
                .tokenId(tokenId)
                .deviceName(deviceInfo.deviceName)
                .deviceType(deviceInfo.deviceType)
                .operatingSystem(deviceInfo.operatingSystem)
                .browser(deviceInfo.browser)
                .ipAddress(ipAddress)
                .location(getLocationFromIp(ipAddress))
                .loginTime(Instant.now())
                .lastActivity(Instant.now())
                .expiresAt(Instant.now().plusMillis(expirationMs))
                .active(true)
                .currentSession(false)
                .build();
        
        log.info("Created new session for user {} from {} ({})", userId, ipAddress, deviceInfo.deviceType);
        return sessionRepository.save(session);
    }

    /**
     * Get all active sessions for a user
     */
    public List<UserSessionDTO> getActiveSessions(Long userId, String currentTokenId) {
        List<UserSession> sessions = sessionRepository.findByUserIdAndActiveOrderByLastActivityDesc(userId, true);
        
        return sessions.stream()
                .map(session -> convertToDTO(session, currentTokenId))
                .collect(Collectors.toList());
    }

    /**
     * Get session count for a user
     */
    public long getActiveSessionCount(Long userId) {
        return sessionRepository.countByUserIdAndActive(userId, true);
    }

    /**
     * Logout from a specific session
     */
    @Transactional
    public boolean logoutSession(Long userId, Long sessionId) {
        Optional<UserSession> sessionOpt = sessionRepository.findById(sessionId);
        
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            if (session.getUserId().equals(userId) && session.isActive()) {
                session.logout();
                sessionRepository.save(session);
                log.info("User {} logged out from session {}", userId, sessionId);
                return true;
            }
        }
        return false;
    }

    /**
     * Logout from all devices except current
     */
    @Transactional
    public int logoutOtherSessions(Long userId, String currentTokenId) {
        int count = sessionRepository.deactivateOtherSessions(userId, currentTokenId, Instant.now());
        log.info("User {} logged out from {} other sessions", userId, count);
        return count;
    }

    /**
     * Logout from all devices
     */
    @Transactional
    public int logoutAllSessions(Long userId) {
        int count = sessionRepository.deactivateAllUserSessions(userId, Instant.now());
        log.info("User {} logged out from all {} sessions", userId, count);
        return count;
    }

    /**
     * Logout by token ID
     */
    @Transactional
    public void logoutByTokenId(String tokenId) {
        sessionRepository.findByTokenId(tokenId).ifPresent(session -> {
            session.logout();
            sessionRepository.save(session);
            log.info("Session {} logged out", tokenId);
        });
    }

    /**
     * Check if session is valid
     */
    public boolean isSessionValid(String tokenId) {
        return sessionRepository.existsByTokenIdAndActive(tokenId, true);
    }

    /**
     * Update session activity
     */
    @Transactional
    public void updateSessionActivity(String tokenId) {
        sessionRepository.findByTokenIdAndActive(tokenId, true).ifPresent(session -> {
            session.updateActivity();
            sessionRepository.save(session);
        });
    }

    /**
     * Get session by token ID
     */
    public Optional<UserSession> getSessionByTokenId(String tokenId) {
        return sessionRepository.findByTokenId(tokenId);
    }

    /**
     * Cleanup expired sessions - runs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredSessions() {
        int deleted = sessionRepository.deleteExpiredSessions(Instant.now());
        log.info("Cleaned up {} expired sessions", deleted);
    }

    /**
     * Convert entity to DTO
     */
    private UserSessionDTO convertToDTO(UserSession session, String currentTokenId) {
        return UserSessionDTO.builder()
                .id(session.getId())
                .deviceName(session.getDeviceName())
                .deviceType(session.getDeviceType())
                .operatingSystem(session.getOperatingSystem())
                .browser(session.getBrowser())
                .ipAddress(maskIpAddress(session.getIpAddress()))
                .location(session.getLocation())
                .loginTime(session.getLoginTime())
                .lastActivity(session.getLastActivity())
                .expiresAt(session.getExpiresAt())
                .active(session.isActive())
                .currentSession(session.getTokenId().equals(currentTokenId))
                .build();
    }

    /**
     * Get client IP address (handles proxies)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Take first IP if multiple (X-Forwarded-For can have multiple)
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Parse User-Agent string to extract device info
     */
    private DeviceInfo parseUserAgent(String userAgent) {
        DeviceInfo info = new DeviceInfo();
        
        if (userAgent == null || userAgent.isEmpty()) {
            info.deviceType = "UNKNOWN";
            info.operatingSystem = "Unknown";
            info.browser = "Unknown";
            info.deviceName = "Unknown Device";
            return info;
        }
        
        userAgent = userAgent.toLowerCase();
        
        // Detect Operating System
        if (userAgent.contains("iphone")) {
            info.operatingSystem = "iOS";
            info.deviceType = "MOBILE";
            info.deviceName = "iPhone";
        } else if (userAgent.contains("ipad")) {
            info.operatingSystem = "iPadOS";
            info.deviceType = "TABLET";
            info.deviceName = "iPad";
        } else if (userAgent.contains("android")) {
            info.operatingSystem = "Android";
            info.deviceType = userAgent.contains("mobile") ? "MOBILE" : "TABLET";
            info.deviceName = "Android Device";
        } else if (userAgent.contains("windows")) {
            info.operatingSystem = "Windows";
            info.deviceType = "DESKTOP";
            info.deviceName = "Windows PC";
        } else if (userAgent.contains("macintosh") || userAgent.contains("mac os")) {
            info.operatingSystem = "macOS";
            info.deviceType = "DESKTOP";
            info.deviceName = "Mac";
        } else if (userAgent.contains("linux")) {
            info.operatingSystem = "Linux";
            info.deviceType = "DESKTOP";
            info.deviceName = "Linux PC";
        } else {
            info.operatingSystem = "Unknown";
            info.deviceType = "UNKNOWN";
            info.deviceName = "Unknown Device";
        }
        
        // Detect Browser
        if (userAgent.contains("flutter") || userAgent.contains("dart")) {
            info.browser = "Gharsih App";
        } else if (userAgent.contains("edg/")) {
            info.browser = "Microsoft Edge";
        } else if (userAgent.contains("chrome")) {
            info.browser = "Google Chrome";
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            info.browser = "Safari";
        } else if (userAgent.contains("firefox")) {
            info.browser = "Firefox";
        } else if (userAgent.contains("opera") || userAgent.contains("opr/")) {
            info.browser = "Opera";
        } else {
            info.browser = "Unknown Browser";
        }
        
        return info;
    }

    /**
     * Get location from IP (placeholder - implement with GeoIP service)
     */
    private String getLocationFromIp(String ipAddress) {
        // For now, return a placeholder
        // In production, use a GeoIP service like MaxMind
        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            return "Local";
        }
        return "Unknown Location";
    }

    /**
     * Mask IP address for privacy (show only first two octets)
     */
    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null) return null;
        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            return ipAddress;
        }
        String[] parts = ipAddress.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*.*";
        }
        return ipAddress;
    }

    /**
     * Inner class for device info
     */
    private static class DeviceInfo {
        String deviceName;
        String deviceType;
        String operatingSystem;
        String browser;
    }
}
