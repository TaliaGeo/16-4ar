package group.g.graduation.backend.Security.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import group.g.graduation.backend.Security.jwt.JwtTokenProvider;
import group.g.graduation.backend.Security.model.RefreshToken;
import group.g.graduation.backend.Security.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles successful OAuth2 authentication by generating JWT tokens
 * and redirecting to the mobile app with the tokens
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    
    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        
        if (response.isCommitted()) {
            log.debug("Response has already been committed");
            return;
        }

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        
        // Generate JWT tokens
        String accessToken = jwtTokenProvider.generateToken(authentication, request);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(oAuth2User.getId());
        
        log.info("OAuth2 login successful for user: {}", oAuth2User.getEmail());
        
        // Build redirect URL with tokens (for mobile app deep link)
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("access_token", accessToken)
                .queryParam("refresh_token", refreshToken.getToken())
                .queryParam("token_type", "Bearer")
                .queryParam("user_id", oAuth2User.getId())
                .queryParam("email", URLEncoder.encode(oAuth2User.getEmail(), StandardCharsets.UTF_8))
                .queryParam("name", URLEncoder.encode(oAuth2User.getFullName(), StandardCharsets.UTF_8))
                .build().toUriString();
        
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
