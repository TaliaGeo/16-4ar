package group.g.graduation.backend.Security.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import group.g.graduation.backend.Security.jwt.JwtTokenProvider;
import group.g.graduation.backend.Security.model.RefreshToken;
import group.g.graduation.backend.Security.model.UserSession;
import group.g.graduation.backend.Security.service.RefreshTokenService;
import group.g.graduation.backend.Security.service.UserSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles successful OAuth2 authentication by generating JWT tokens.
 * Redirects to the app's custom URL scheme with JWT tokens as query params.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserSessionService userSessionService;
    
    @Value("#{'${app.oauth2.authorized-redirect-uris}'.split(',')}")
    private List<String> authorizedRedirectUris;
    
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        
        if (response.isCommitted()) {
            log.debug("Response has already been committed");
            return;
        }

        // Get redirect_uri from cookie (stored during authorization request)
        String targetRedirectUri = getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .orElse(null);
        
        boolean hasRedirectCookie = (targetRedirectUri != null && isAuthorizedRedirectUri(targetRedirectUri));
        
        if (!hasRedirectCookie) {
            log.info("No redirect_uri cookie — request is likely from a browser test");
            // Default to the first authorized URI (mobile deep link)
            targetRedirectUri = authorizedRedirectUris.get(0);
        }

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        
        // Create a UserSession (required for token validation)
        UserSession session = userSessionService.createSession(oAuth2User.getId(), jwtExpirationMs, request);
        String tokenId = session.getTokenId();
        
        // Generate JWT tokens using the session's tokenId
        String accessToken = jwtTokenProvider.generateTokenWithSessionId(authentication, request, tokenId);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(oAuth2User.getId());
        
        log.info("OAuth2 login successful for user: {} (id={})", oAuth2User.getEmail(), oAuth2User.getId());
        
        clearAuthenticationAttributes(request);
        
        // Clean up cookies using ResponseCookie for consistency
        removeResponseCookie(response, HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        removeResponseCookie(response, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME);

        // Always redirect — never show an HTML page.
        // If the cookie was found → use it (deep link or auth.html).
        // If the cookie was lost → default to the first authorized URI (gharsih://oauth2/callback).
        String targetUrl = UriComponentsBuilder.fromUriString(targetRedirectUri)
                .queryParam("access_token", accessToken)
                .queryParam("refresh_token", refreshToken.getToken())
                .queryParam("token_type", "Bearer")
                .queryParam("user_id", oAuth2User.getId())
                .queryParam("email", URLEncoder.encode(oAuth2User.getEmail(), StandardCharsets.UTF_8))
                .queryParam("name", URLEncoder.encode(oAuth2User.getFullName(), StandardCharsets.UTF_8))
                .build().toUriString();
        log.info("Redirecting after OAuth2 success — cookie={}, target={}", hasRedirectCookie, targetRedirectUri);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
    
    /**
     * Validates that the redirect URI is in the whitelist.
     * Exact match first, then localhost with any port (for development).
     */
    private boolean isAuthorizedRedirectUri(String uri) {
        // Exact match
        if (authorizedRedirectUris.stream().anyMatch(a -> uri.equals(a))) {
            return true;
        }
        // Dev convenience: accept any localhost port with or without /auth.html
        // Examples: http://localhost:8080, http://localhost:8080/auth.html
        if (uri.matches("^http://localhost:\\d+(/(auth\\.html)?)?$")) {
            log.debug("Accepting localhost redirect URI with dynamic port: {}", uri);
            return true;
        }
        // Also accept the gharsih deep-link scheme
        if (uri.startsWith("gharsih://")) {
            return true;
        }
        return false;
    }
    
    /**
     * Get cookie value by name
     */
    private Optional<String> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> name.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
        }
        return Optional.empty();
    }
    
    /**
     * Remove cookie using ResponseCookie (consistent with how it was created)
     */
    private void removeResponseCookie(HttpServletResponse response, String name) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
