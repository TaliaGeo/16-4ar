package group.g.graduation.backend.Security.oauth2;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import group.g.graduation.backend.Security.model.User;
import lombok.Getter;

/**
 * Custom OAuth2User that wraps our User entity with OAuth2 attributes
 */
@Getter
public class CustomOAuth2User implements OAuth2User {
    
    private final User user;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String nameAttributeKey;

    public CustomOAuth2User(User user, Map<String, Object> attributes, 
                           Collection<? extends GrantedAuthority> authorities,
                           String nameAttributeKey) {
        this.user = user;
        this.attributes = attributes;
        this.authorities = authorities;
        this.nameAttributeKey = nameAttributeKey;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return attributes.get(nameAttributeKey).toString();
    }
    
    public Long getId() {
        return user.getId();
    }
    
    public String getEmail() {
        return user.getEmail();
    }
    
    public String getFullName() {
        return user.getFullName();
    }
}
