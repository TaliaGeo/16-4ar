package group.g.graduation.backend.Security.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles OAuth2 authentication failures.
 * For mobile clients: redirects to the app deep link with error info.
 * For browser clients: returns a helpful HTML error page.
 */
@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {
        
        log.error("OAuth2 authentication failed: {} | URL: {} | Query: {}", 
                exception.getMessage(), request.getRequestURI(), request.getQueryString());
        log.error("OAuth2 failure exception class: {}", exception.getClass().getName());
        if (exception.getCause() != null) {
            log.error("OAuth2 failure root cause: {} - {}", exception.getCause().getClass().getName(), exception.getCause().getMessage());
        }

        // Get redirect_uri from cookie or use default
        String redirectUri = getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .orElse(null);

        // Clean up cookies
        removeResponseCookie(response, HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        removeResponseCookie(response, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME);

        if (redirectUri != null && redirectUri.startsWith("http")) {
            // Browser redirect with error
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", URLEncoder.encode(exception.getLocalizedMessage(), StandardCharsets.UTF_8))
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            // Return an HTML error page instead of raw JSON (much friendlier for browser testing)
            String errorMsg = exception.getLocalizedMessage();
            String hint = "";
            if (errorMsg != null && errorMsg.contains("invalid_request")) {
                hint = "<p style='color:#92400e;background:#fef3c7;padding:12px;border-radius:8px'>"
                     + "<strong>Hint:</strong> This error usually means you refreshed the callback page "
                     + "or navigated directly to /oauth2/callback/google without query parameters. "
                     + "Each Google authorization code can only be used once. "
                     + "Please start a new login flow from <a href='/oauth2/authorization/google'>/oauth2/authorization/google</a>.</p>";
            }
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/html; charset=UTF-8");
            String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>OAuth2 Failed</title>"
                    + "<style>body{font-family:system-ui,sans-serif;max-width:600px;margin:60px auto;padding:20px;"
                    + "background:#fef2f2;color:#7f1d1d}h1{color:#dc2626}.error{background:#fff;border:1px solid #fecaca;"
                    + "border-radius:8px;padding:12px;margin:8px 0;word-break:break-all;font-family:monospace;font-size:13px}"
                    + "a{color:#2563eb}</style></head><body>"
                    + "<h1>&#10060; OAuth2 Login Failed</h1>"
                    + "<div class='error'>" + (errorMsg != null ? errorMsg : "Unknown error") + "</div>"
                    + hint
                    + "<p><a href='/oauth2/authorization/google'>&#8594; Try again with Google</a></p>"
                    + "</body></html>";
            response.getWriter().write(html);
        }
    }
    
    /**
     * Remove cookie using ResponseCookie (consistent with creation)
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
}
