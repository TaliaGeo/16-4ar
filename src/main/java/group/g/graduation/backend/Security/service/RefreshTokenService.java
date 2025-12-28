package group.g.graduation.backend.Security.service;

import group.g.graduation.backend.Security.exception.TokenRefreshException;
import group.g.graduation.backend.Security.model.RefreshToken;
import group.g.graduation.backend.Security.repository.RefreshTokenRepository;
import group.g.graduation.backend.Security.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    @Value("${app.jwt.refresh-token.expiration-ms:604800000}") // Default 7 days
    private long refreshTokenExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        // Get user to verify existence
        UserDTO userDTO = userService.getUserById(userId);
        
        if (userDTO == null) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        
        // Check if user already has a token
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        if (existingToken.isPresent()) {
            // Update the existing token
            RefreshToken token = existingToken.get();
            token.setToken(UUID.randomUUID().toString());
            token.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
            token.setRevoked(false);
            return refreshTokenRepository.save(token);
        } else {
            // Create new refresh token with secure random UUID
            RefreshToken refreshToken = RefreshToken.builder()
                    .userId(userId)
                    .token(UUID.randomUUID().toString())
                    .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                    .revoked(false)
                    .build();
            
            log.info("Created new refresh token for user ID: {}", userId);
            return refreshTokenRepository.save(refreshToken);
        }
    }

    @Transactional(readOnly = true)
    public boolean isUserActivelyLoggedIn(Long userId) {
        return refreshTokenRepository.existsByUserIdAndRevokedFalse(userId);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new login");
        }
        
        if (token.isRevoked()) {
            throw new TokenRefreshException(token.getToken(), "Refresh token was revoked. Please make a new login");
        }
        
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException(token, "Refresh token not found"));
        
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        log.info("Revoked refresh token for user ID: {}", refreshToken.getUserId());
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        // Get all non-revoked tokens for the user and revoke them
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserIdAndRevoked(userId, false);
        userTokens.forEach(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
        log.info("Revoked all refresh tokens for user ID: {}", userId);
    }

    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        log.info("Cleaned up expired refresh tokens");
    }
}