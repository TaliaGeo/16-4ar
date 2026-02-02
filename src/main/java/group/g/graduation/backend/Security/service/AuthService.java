package group.g.graduation.backend.Security.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import group.g.graduation.backend.Security.dto.*;
import group.g.graduation.backend.Security.exception.TokenRefreshException;
import group.g.graduation.backend.Security.jwt.JwtTokenProvider;
import group.g.graduation.backend.Security.model.RefreshToken;
import group.g.graduation.backend.Security.model.UserSession;
import group.g.graduation.backend.Security.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionService sessionService;
    
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Transactional
    public AuthResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Get user details
        UserDTO user = userService.getUserByEmail(loginRequest.getEmail());
        
        log.info("User {} logging in with roles: {}", user.getEmail(), user.getRoleNames());
        
        // Create session and get tokenId
        UserSession session = sessionService.createSession(user.getId(), jwtExpirationMs, request);
        String tokenId = session.getTokenId();

        // Generate JWT access token with tokenId
        String jwt = tokenProvider.generateTokenWithSessionId(authentication, request, tokenId);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        // Join role names with comma for the response
        String roleNames = String.join(",", user.getRoleNames());

        log.info("User {} successfully logged in from {} with roles: {} and authorities: {}", 
                user.getEmail(), session.getDeviceType(), roleNames,
                authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList());

        return new AuthResponse(jwt, tokenId, user.getId(), user.getEmail(), roleNames, refreshToken.getToken());
    }

  @Transactional
public AuthResponse register(RegisterRequest registerRequest, HttpServletRequest request) {
    // Create user DTO
    UserDTO userDTO = new UserDTO();
    userDTO.setEmail(registerRequest.getEmail());
    userDTO.setFullName(registerRequest.getFullName());
    userDTO.setPassword(registerRequest.getPassword());
    userDTO.setRoleNames(registerRequest.getRoleNames());
    
    // Save user
    UserDTO savedUser = userService.createUser(userDTO);
    
    log.info("User {} successfully registered", savedUser.getEmail());

    // Return response without JWT token
    return new AuthResponse(
        null,  // No access token - user needs to login
        null,  // No token ID
        savedUser.getId(), 
        savedUser.getEmail(), 
        String.join(",", savedUser.getRoleNames())
    );
}
    @Transactional
    public TokenRefreshResponse refreshToken(String refreshToken, HttpServletRequest request) {
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(this::getUserFromRefreshToken)
                .map(userDTO -> {
                    // Create authentication object
                    Authentication authentication = createAuthentication(userDTO);
                    
                    // Generate new access token
                    String accessToken = tokenProvider.generateToken(authentication, request);
                    
                    return new TokenRefreshResponse(accessToken, refreshToken);
                })
                .orElseThrow(() -> new TokenRefreshException(refreshToken, "Refresh token not found"));
    }
    
   @Transactional
public void logout(String refreshToken) {
    try {
        // Find and revoke the refresh token
        refreshTokenService.findByToken(refreshToken)
                .ifPresent(token -> {
                    Long userId = token.getUserId();
                    // Revoke all refresh tokens for this user
                    refreshTokenService.revokeAllUserTokens(userId);
                });
        
        // Clear security context
        SecurityContextHolder.clearContext();
        
        log.info("User successfully logged out");
        
    } catch (Exception e) {
        log.error("Error during logout: {}", e.getMessage());
        throw e;
    }
}
    
    private UserDTO getUserFromRefreshToken(RefreshToken refreshToken) {
        Long userId = refreshToken.getUserId();
        return userService.getUserById(userId);
    }
    
    private Authentication createAuthentication(UserDTO userDTO) {
        // Create authorities from roles
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add role-based authorities
        userDTO.getRoleNames().forEach(roleName -> 
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName)));
            
        // Here we would also add permission-based authorities if needed
        
        UserDetails userDetails = new User(
                userDTO.getEmail(),
                "", // password not needed for token refresh
                authorities
        );
        
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }
}