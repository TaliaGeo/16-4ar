package group.g.graduation.backend.Security.oauth2;

import java.util.Map;

import group.g.graduation.backend.Security.model.AuthProvider;

/**
 * Abstract class for extracting user info from OAuth2 providers
 */
public abstract class OAuth2UserInfo {
    
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public abstract String getId();
    public abstract String getName();
    public abstract String getEmail();
    public abstract String getImageUrl();
    public abstract AuthProvider getProvider();
    
    /**
     * Factory method to create appropriate OAuth2UserInfo based on provider
     */
    public static OAuth2UserInfo create(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(AuthProvider.GOOGLE.name())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.FACEBOOK.name())) {
            return new FacebookOAuth2UserInfo(attributes);
        } else {
            throw new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId);
        }
    }
}
