package group.g.graduation.backend.Security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import group.g.graduation.backend.Security.dto.ForgotPasswordRequest;
import group.g.graduation.backend.Security.dto.PasswordResetDTO;
import group.g.graduation.backend.Security.dto.PasswordResetTokenDTO;
import group.g.graduation.backend.Security.dto.UserDTO;
import group.g.graduation.backend.Security.model.PasswordResetToken;
import group.g.graduation.backend.Security.repository.PasswordResetTokenRepository;
import group.g.graduation.backend.common.email.EmailService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {
    
    @Value("${app.password-reset.token-expiration-minutes:30}") // Default 30 minutes
    private int tokenExpirationMinutes;
    
    @Value("${app.password-reset.max-tokens-per-user:3}") // Max tokens per user
    private int maxTokensPerUser;
    
    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    /**
     * Process a forgot password request and generate a reset token
     */
    @Transactional
    public String processForgotPassword(ForgotPasswordRequest request) {
        // Get user
        UserDTO user;
        try {
            user = userService.getUserByEmail(request.getEmail());
        } catch (Exception e) {
            // For security reasons, we don't reveal if the email exists or not
            log.info("Password reset requested for non-existent email: {}", request.getEmail());
            return null; // Return null but handle gracefully in controller
        }
        
        if (user == null || user.getActive() == null || !user.getActive()) {
            log.info("Password reset requested for inactive user: {}", request.getEmail());
            return null;
        }
        
        // Check if user already has too many active tokens using Spring Data JPA method
        long existingTokens = tokenRepository.countByUserIdAndUsedAndExpiryDateAfter(
            user.getId(), false, Instant.now());
        
        if (existingTokens >= maxTokensPerUser) {
            log.warn("User {} has too many active password reset tokens", user.getEmail());
            throw new IllegalStateException("Too many active password reset tokens. Please wait before requesting a new one.");
        }
        
        // Invalidate all existing tokens for this user using Spring Data JPA methods
        List<PasswordResetToken> activeTokens = tokenRepository.findByUserIdAndUsed(user.getId(), false);
        activeTokens.forEach(token -> {
            token.setUsed(true);
            tokenRepository.save(token);
        });
        
        // Create new token (6-digit code)
        PasswordResetToken token = PasswordResetToken.createTokenForUser(
            user.getId(), 
            user.getEmail(), 
            tokenExpirationMinutes
        );
        tokenRepository.save(token);
        
        log.info("Generated password reset code for user: {}", user.getEmail());
        
        // Send verification code via email
        try {
            emailService.sendVerificationCode(user.getEmail(), token.getToken(), user.getFullName());
            log.info("Password reset code sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset code to {}: {}", user.getEmail(), e.getMessage());
            // Still return the token for testing, but log the email failure
        }
        
        return token.getToken();
    }
    
    /**
     * Reset a user's password using a valid token
     */
    @Transactional
    public void resetPassword(PasswordResetDTO request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        
        // Validate password strength
        if (!isValidPassword(request.getPassword())) {
            throw new IllegalArgumentException("Password does not meet security requirements");
        }
        
        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));
        
        if (!token.isValid()) {
            if (token.isUsed()) {
                throw new IllegalArgumentException("Token has already been used");
            } else {
                throw new IllegalArgumentException("Token has expired");
            }
        }
        
        // Get user
        UserDTO user = userService.getUserById(token.getUserId());
        if (user == null || user.getActive() == null || !user.getActive()) {
            throw new IllegalArgumentException("User account is not active");
        }
        
        // Mark token as used
        token.markAsUsed();
        tokenRepository.save(token);
        
        // Invalidate all other tokens for this user using Spring Data JPA methods
        List<PasswordResetToken> otherTokens = tokenRepository.findByUserIdAndUsed(token.getUserId(), false);
        otherTokens.forEach(otherToken -> {
            otherToken.setUsed(true);
            tokenRepository.save(otherToken);
        });
        
        // Update user password
        userService.updateUserPassword(token.getUserId(), request.getPassword());
        
        log.info("Password reset successful for user: {}", user.getEmail());
    }
    
    /**
     * Validate if a token is valid (not expired and not used)
     */
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        Optional<PasswordResetToken> resetToken = tokenRepository.findByToken(token);
        return resetToken.isPresent() && resetToken.get().isValid();
    }
    
    /**
     * Get password reset token information
     */
    @Transactional(readOnly = true)
    public Optional<PasswordResetTokenDTO> getTokenInfo(String token) {
        return tokenRepository.findByToken(token)
                .map(this::mapToDto);
    }
    
    /**
     * Get all valid tokens for a user
     */
    @Transactional(readOnly = true)
    public List<PasswordResetTokenDTO> getValidTokensForUser(Long userId) {
        return tokenRepository.findByUserIdAndUsedAndExpiryDateAfter(userId, false, Instant.now()).stream()
                .map(this::mapToDto)
                .toList();
    }
    
    /**
     * Scheduled task to clean up expired tokens
     * Runs once a day at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        int deletedCount = tokenRepository.deleteByExpiryDateBefore(Instant.now());
        log.info("Cleaned up {} expired password reset tokens", deletedCount);
    }
    
    /**
     * Check if a password meets the complexity requirements
     */
    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
               password.matches(".*\\d.*") &&
               password.matches(".*[a-z].*") &&
               password.matches(".*[A-Z].*") &&
               password.matches(".*[@#$%^&+=].*") &&
               !password.matches(".*\\s.*");
    }
    
    /**
     * Map PasswordResetToken to DTO
     */
    private PasswordResetTokenDTO mapToDto(PasswordResetToken token) {
        return PasswordResetTokenDTO.builder()
                .id(token.getId())
                .token(token.getToken())
                .userId(token.getUserId())
                .userEmail(token.getUserEmail())
                .expiryDate(token.getExpiryDate())
                .used(token.isUsed())
                .createdAt(token.getCreatedAt())
                .build();
    }
}