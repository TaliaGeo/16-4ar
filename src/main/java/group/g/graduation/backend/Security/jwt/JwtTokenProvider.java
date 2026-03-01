package group.g.graduation.backend.Security.jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import group.g.graduation.backend.Security.dto.UserDTO;
import group.g.graduation.backend.Security.oauth2.CustomOAuth2User;
import group.g.graduation.backend.Security.service.UserService;
import group.g.graduation.backend.Security.service.UserSessionService;
import group.g.graduation.backend.Security.token.model.UserToken;
import group.g.graduation.backend.Security.token.service.TokenStoreService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret:mySecretKeyThatShouldBeAtLeast256BitsLongForHMACAlgorithmToWork}")
    private String jwtSecretStr;

    @Value("${app.jwt.expiration-ms:3600000}") // Default: 1 hour
    private long jwtExpirationMs;

    @Value("${app.jwt.issuer:lms-application}")
    private String jwtIssuer;

    private SecretKey jwtSecret;
    
    private final TokenStoreService tokenStoreService;
    private final UserSessionService userSessionService;
    private final UserService userService;

    public JwtTokenProvider(TokenStoreService tokenStoreService, UserSessionService userSessionService, @Lazy UserService userService) {
        this.tokenStoreService = tokenStoreService;
        this.userSessionService = userSessionService;
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        // Ensure the secret is at least 256 bits (32 bytes) for HS256
        String secretToUse = jwtSecretStr;
        if (secretToUse.length() < 32) {
            // Pad the secret to at least 32 characters
            secretToUse = String.format("%-32s", secretToUse).replace(' ', '0');
            log.warn("JWT secret was too short, padded to 32 characters for security");
        }
        
        this.jwtSecret = Keys.hmacShaKeyFor(secretToUse.getBytes());
        log.info("JWT Provider initialized successfully with expiration: {} ms", jwtExpirationMs);
    }

    public String generateToken(Authentication authentication, HttpServletRequest request) {
        String username;
        Map<String, Object> claims = new HashMap<>();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    
        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            username = userDetails.getUsername();
        } else if (authentication.getPrincipal() instanceof CustomOAuth2User) {
            // For OAuth2 login, getName() returns the provider sub ID, not email.
            // Use the actual email stored on our user object.
            username = ((CustomOAuth2User) authentication.getPrincipal()).getEmail();
        } else {
            username = authentication.getName();
        }
    
        // Process roles and permissions
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .collect(Collectors.toList());
    
        List<String> permissions = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_"))
                .collect(Collectors.toList());
    
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("type", "access");
    
        // Get user from the user service
        UserDTO user = userService.getUserByEmail(username);
        
        // Create token in the token store
        UserToken userToken = tokenStoreService.createToken(
            user.getId(),
            request.getHeader("User-Agent"),
            request.getRemoteAddr()
        );
        
        claims.put("userId", user.getId());
        claims.put("tokenId", userToken.getTokenId());
        claims.put("issuedAt", new Date().getTime());
    
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
    
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer(jwtIssuer)
                .setId(UUID.randomUUID().toString())
                .signWith(jwtSecret)
                .compact();
    }

    /**
     * Generate token with a specific session ID (used with UserSession tracking)
     */
    public String generateTokenWithSessionId(Authentication authentication, HttpServletRequest request, String sessionTokenId) {
        String username;
        Map<String, Object> claims = new HashMap<>();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    
        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            username = userDetails.getUsername();
        } else if (authentication.getPrincipal() instanceof CustomOAuth2User) {
            username = ((CustomOAuth2User) authentication.getPrincipal()).getEmail();
        } else {
            username = authentication.getName();
        }
    
        // Process roles and permissions
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .collect(Collectors.toList());
    
        List<String> permissions = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_"))
                .collect(Collectors.toList());
    
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("type", "access");
    
        // Get user from the user service
        UserDTO user = userService.getUserByEmail(username);
        
        claims.put("userId", user.getId());
        claims.put("tokenId", sessionTokenId); // Use session token ID
        claims.put("issuedAt", new Date().getTime());
    
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
    
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer(jwtIssuer)
                .setId(UUID.randomUUID().toString())
                .signWith(jwtSecret)
                .compact();
    }

    public UserDetails getUserDetailsFromJWT(String token) {
        Claims claims = parseToken(token);

        String username = claims.getSubject();

        // Get roles and permissions from claims
        List<String> roles = claims.get("roles", List.class);
        List<String> permissions = claims.get("permissions", List.class);

        // Build authorities list from roles and permissions
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        }

        // Add permissions
        if (permissions != null) {
            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        }

        return new org.springframework.security.core.userdetails.User(username, "", authorities);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public String getTokenIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("tokenId", String.class);
        } catch (Exception e) {
            log.error("Error extracting tokenId from JWT: {}", e.getMessage());
            return null;
        }
    }
    
    public boolean validateToken(String token) {
        try {
            // Parse and validate token
            Claims claims = parseToken(token);

            // Check if token ID exists
            String tokenId = claims.get("tokenId", String.class);
            if (tokenId == null) {
                log.error("Token ID is missing in the token claims");
                return false;
            }

            // Check if token is revoked in session store
            if (!userSessionService.isSessionValid(tokenId)) {
                log.error("Token {} has been revoked or expired in session store", tokenId);
                return false;
            }

            // Token is valid
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Malformed JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }

        return false;
    }

    public Authentication getAuthentication(String token) {
        UserDetails userDetails = getUserDetailsFromJWT(token);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}