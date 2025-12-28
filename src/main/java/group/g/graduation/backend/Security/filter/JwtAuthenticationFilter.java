package group.g.graduation.backend.Security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import group.g.graduation.backend.Security.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import group.g.graduation.backend.Security.jwt.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh-token",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/api/auth/oauth2",
        "/oauth2/",
        "/api/security/health",
        "/actuator/health",
        "/swagger-ui/",
        "/v3/api-docs/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip filter for public paths and OPTIONS requests
        if (shouldSkipFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt)) {
                if (tokenProvider.validateToken(jwt)) {
                    Authentication authentication = tokenProvider.getAuthentication(jwt);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            
            filterChain.doFilter(request, response);
            
        } catch (ExpiredJwtException ex) {
            log.error("JWT token has expired: {}", ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token has expired. Please login again");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid token format");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Unsupported token");
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid token signature");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token claims are empty");
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
            filterChain.doFilter(request, response);
        }
    }
    
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                message,
                LocalDateTime.now()
        );
        
        objectMapper.findAndRegisterModules(); // For LocalDateTime serialization
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean shouldSkipFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip filter for public endpoints
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        
        // Skip filter for OPTIONS requests (CORS preflight)
        return "OPTIONS".equals(request.getMethod());
    }
}