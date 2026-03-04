package group.g.graduation.backend.Security.oauth2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Stores OAuth2 authorization requests in cookies instead of the HTTP session.
 * Required because the app uses STATELESS session management, so the server-side
 * session is not maintained across redirects.
 * 
 * Uses JSON serialization (Jackson) because OAuth2AuthorizationRequest is NOT 
 * Serializable in Spring Security 6+/7+.
 */
@Slf4j
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    // URL-safe Base64 without padding — avoids '=' char issues in cookie values
    private static final Base64.Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder(); // handles both padded & unpadded

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        log.debug("Loading OAuth2 authorization request from cookie");
        Optional<String> cookieValue = getCookieValue(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        if (cookieValue.isEmpty()) {
            log.warn("OAuth2 authorization request cookie NOT found in request");
            return null;
        }
        log.debug("Cookie found, length: {}", cookieValue.get().length());
        return deserialize(cookieValue.get());
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        String serialized = serialize(authorizationRequest);
        log.info("Saving OAuth2 auth request — cookie size: {} chars, state: {}, code_verifier present: {}",
                serialized.length(),
                authorizationRequest.getState(),
                authorizationRequest.getAttributes().containsKey("code_verifier"));
        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialized, COOKIE_EXPIRE_SECONDS);

        // ALWAYS delete old redirect_uri cookie first (prevents pollution across sessions)
        deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
        
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isBlank()) {
            log.info("Storing NEW redirect_uri cookie: {}", redirectUriAfterLogin);
            addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME,
                    redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
        } else {
            log.warn("No redirect_uri parameter provided — cookie deleted, will use default later");
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            log.info("Removing OAuth2 auth request — state: {}, code_verifier present: {}, redirectUri: {}",
                    authorizationRequest.getState(),
                    authorizationRequest.getAttributes().containsKey("code_verifier"),
                    authorizationRequest.getRedirectUri());
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        } else {
            log.error("removeAuthorizationRequest: cookie was missing or could not be deserialized!");
        }
        return authorizationRequest;
    }

    // ── JSON Serialization ────────────────────────────────────────────────────────

    /**
     * Serialize OAuth2AuthorizationRequest to JSON then Base64.
     * We manually extract the fields since the class is not directly Jackson-serializable.
     * NOTE: authorizationRequestUri is intentionally excluded to reduce cookie size.
     */
    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("authorizationUri", authorizationRequest.getAuthorizationUri());
            data.put("clientId", authorizationRequest.getClientId());
            data.put("redirectUri", authorizationRequest.getRedirectUri());
            data.put("scopes", authorizationRequest.getScopes());
            data.put("state", authorizationRequest.getState());
            data.put("additionalParameters", authorizationRequest.getAdditionalParameters());
            // authorizationRequestUri excluded — the builder reconstructs it from the above fields
            data.put("attributes", authorizationRequest.getAttributes());
            // Grant type (usually "authorization_code")
            if (authorizationRequest.getGrantType() != null) {
                data.put("grantType", authorizationRequest.getGrantType().getValue());
            }
            // Response type (usually "code")
            if (authorizationRequest.getResponseType() != null) {
                data.put("responseType", authorizationRequest.getResponseType().getValue());
            }
            
            String json = objectMapper.writeValueAsString(data);
            return B64_ENCODER.encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to serialize OAuth2AuthorizationRequest", e);
            throw new IllegalStateException("Failed to serialize OAuth2 authorization request", e);
        }
    }

    /**
     * Deserialize OAuth2AuthorizationRequest from Base64-encoded JSON.
     */
    @SuppressWarnings("unchecked")
    private OAuth2AuthorizationRequest deserialize(String base64Value) {
        try {
            String json = new String(B64_DECODER.decode(base64Value), StandardCharsets.UTF_8);
            Map<String, Object> data = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            
            Set<String> scopes = new HashSet<>();
            Object scopesObj = data.get("scopes");
            if (scopesObj instanceof Iterable) {
                for (Object s : (Iterable<?>) scopesObj) {
                    scopes.add(s.toString());
                }
            }
            
            Map<String, Object> additionalParameters = new HashMap<>();
            Object additionalObj = data.get("additionalParameters");
            if (additionalObj instanceof Map) {
                additionalParameters.putAll((Map<String, Object>) additionalObj);
            }
            
            Map<String, Object> attributes = new HashMap<>();
            Object attrsObj = data.get("attributes");
            if (attrsObj instanceof Map) {
                attributes.putAll((Map<String, Object>) attrsObj);
            }

            log.debug("Deserializing OAuth2 request — state: {}, code_verifier: {}, redirectUri: {}, attrs keys: {}",
                    data.get("state"),
                    attributes.containsKey("code_verifier") ? attributes.get("code_verifier").toString().substring(0, 10) + "..." : "MISSING",
                    data.get("redirectUri"),
                    attributes.keySet());

            OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri((String) data.get("authorizationUri"))
                    .clientId((String) data.get("clientId"))
                    .redirectUri((String) data.get("redirectUri"))
                    .scopes(scopes)
                    .state((String) data.get("state"))
                    .additionalParameters(additionalParameters)
                    .attributes(attrs -> attrs.putAll(attributes))
                    .build();

            // Verify the built object retained code_verifier
            log.info("Deserialized OAuth2AuthorizationRequest — code_verifier in built attrs: {}, additionalParams keys: {}",
                    request.getAttributes().containsKey("code_verifier"),
                    request.getAdditionalParameters().keySet());

            return request;
        } catch (Exception e) {
            log.error("Failed to deserialize OAuth2AuthorizationRequest: {}", e.getMessage(), e);
            return null; // Return null instead of throwing — prevents 500 errors
        }
    }

    // ── Cookie helpers ────────────────────────────────────────────────────────

    private static void addCookie(HttpServletResponse response, String name,
            String value, int maxAge) {
        // Use ResponseCookie for proper SameSite support (required for OAuth2 cross-site redirects)
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    @SuppressWarnings("unused")
    private static void deleteCookie(HttpServletRequest request,
            HttpServletResponse response, String name) {
        // Use ResponseCookie for deletion — must match the attributes used during creation
        // (Path, HttpOnly, SameSite) so the browser recognises it as the same cookie.
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Clear both OAuth2 cookies (called after login or on error).
     */
    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}
