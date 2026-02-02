package group.g.graduation.backend.Security.service;

import group.g.graduation.backend.Security.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;
    private final RoleService roleService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            // Get user with password for authentication
            UserDTO userDTO = userService.getUserByEmailWithPassword(email);
            
            if (userDTO == null) {
                throw new UsernameNotFoundException("User not found with email: " + email);
            }
            
            // Check if user is active
            if (userDTO.getActive() == null || !userDTO.getActive()) {
                throw new UsernameNotFoundException("User account is disabled: " + email);
            }
            
            // Build authorities from roles
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            
            // Add role-based authorities
            for (String roleName : userDTO.getRoleNames()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                
                // Get permissions for this role
                List<String> permissions = roleService.getPermissionsForRole(roleName);
                
                // Add permission-based authorities
                authorities.addAll(permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
            }
            
            // Log successful loading with authorities
            log.info("User loaded: {}, active: {}, roles: {}, authorities: {}", 
                     email, userDTO.getActive(), userDTO.getRoleNames(),
                     authorities.stream().map(GrantedAuthority::getAuthority).toList());
            
            // Return UserDetails with the actual password for authentication
            return new User(
                    userDTO.getEmail(),
                    userDTO.getPassword(), // Use the actual password from user service
                    Boolean.TRUE.equals(userDTO.getActive()),    // enabled
                    true,                  // accountNonExpired
                    true,                  // credentialsNonExpired
                    Boolean.TRUE.equals(userDTO.getActive()),    // accountNonLocked
                    authorities
            );
            
        } catch (Exception e) {
            log.error("Error loading user by username: {}", email, e);
            throw new UsernameNotFoundException("Error loading user: " + e.getMessage(), e);
        }
    }
}