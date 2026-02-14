package group.g.graduation.backend.Security.oauth2;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import group.g.graduation.backend.Security.exception.OAuth2AuthenticationProcessingException;
import group.g.graduation.backend.Security.model.AuthProvider;
import group.g.graduation.backend.Security.model.Role;
import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.RoleRepository;
import group.g.graduation.backend.Security.repository.UserRepository;
import group.g.graduation.backend.common.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom OAuth2 user service that processes OAuth2 login and creates/updates users
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        
        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationProcessingException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfo.create(registrationId, oAuth2User.getAttributes());
        
        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;
        
        if (userOptional.isPresent()) {
            user = userOptional.get();
            
            // Check if user registered with different provider
            if (!user.getAuthProvider().equals(oAuth2UserInfo.getProvider()) 
                && user.getAuthProvider() != AuthProvider.LOCAL) {
                throw new OAuth2AuthenticationProcessingException(
                    "You're signed up with " + user.getAuthProvider() + " account. " +
                    "Please use your " + user.getAuthProvider() + " account to login."
                );
            }
            
            // Update existing user
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            // Register new user
            user = registerNewUser(oAuth2UserInfo);
        }

        // Build authorities from user roles
        var authorities = user.getRoles().stream()
                .flatMap(role -> {
                    var roleAuthorities = new HashSet<SimpleGrantedAuthority>();
                    roleAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                    role.getPermissions().forEach(permission -> 
                        roleAuthorities.add(new SimpleGrantedAuthority(permission.getName()))
                    );
                    return roleAuthorities.stream();
                })
                .collect(Collectors.toSet());

        String nameAttributeKey = oAuth2UserRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        
        return new CustomOAuth2User(user, oAuth2User.getAttributes(), authorities, nameAttributeKey);
    }

    private User registerNewUser(OAuth2UserInfo oAuth2UserInfo) {
        log.info("Registering new OAuth2 user: {}", oAuth2UserInfo.getEmail());
        
        User user = new User();
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setFullName(oAuth2UserInfo.getName());
        user.setImageUrl(oAuth2UserInfo.getImageUrl());
        user.setAuthProvider(oAuth2UserInfo.getProvider());
        user.setProviderId(oAuth2UserInfo.getId());
        user.setActive(true);
        
        // Assign default USER role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role USER not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        
        User savedUser = userRepository.save(user);
        
        // Send welcome email asynchronously
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName());
            log.info("Welcome email sent to OAuth2 user: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", savedUser.getEmail(), e.getMessage());
            // Don't fail registration if email fails
        }
        
        return savedUser;
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        log.info("Updating existing OAuth2 user: {}", existingUser.getEmail());
        
        existingUser.setFullName(oAuth2UserInfo.getName());
        existingUser.setImageUrl(oAuth2UserInfo.getImageUrl());
        
        // If user was LOCAL and now logging with OAuth2, update provider
        if (existingUser.getAuthProvider() == AuthProvider.LOCAL) {
            existingUser.setAuthProvider(oAuth2UserInfo.getProvider());
            existingUser.setProviderId(oAuth2UserInfo.getId());
        }
        
        return userRepository.save(existingUser);
    }
}
