package group.g.graduation.backend.Security.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import group.g.graduation.backend.Security.model.Permission;
import group.g.graduation.backend.Security.model.Role;
import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.PermissionRepository;
import group.g.graduation.backend.Security.repository.RoleRepository;
import group.g.graduation.backend.Security.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityInitializationService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@gharsih.ps}")
    private String adminEmail;

    @Value("${app.admin.password:${ADMIN_PASSWORD:admin123}}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    @PostConstruct
    @Transactional
    public void initializeSecurityData() {
        log.info("Initializing security data...");
        
        // Create default permissions
        createPermissionIfNotExists("user:create", "Can create users");
        createPermissionIfNotExists("user:read", "Can view user information");
        createPermissionIfNotExists("user:update", "Can update users");
        createPermissionIfNotExists("user:delete", "Can delete users");
        
        createPermissionIfNotExists("admin:access", "Access to admin functionality");
        
        // Create default roles
        Role adminRole = createRoleIfNotExists("ADMIN", "Administrator with all permissions");
        Role userRole = createRoleIfNotExists("USER", "Regular user with basic permissions");
        
        // Assign all permissions to admin role
        Set<Permission> adminPermissions = new HashSet<>(permissionRepository.findAll());
        adminRole.setPermissions(adminPermissions);
        roleRepository.save(adminRole);
        
        // Assign user permissions (basic read permissions)
        Set<Permission> userPermissions = new HashSet<>();
        addPermissionIfExists(userPermissions, "user:read");
        userRole.setPermissions(userPermissions);
        roleRepository.save(userRole);
        
        // Create default admin user if not exists
        createDefaultAdminUser(adminRole);
        
        log.info("Security data initialization completed");
    }
    
    private Permission createPermissionIfNotExists(String name, String description) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setName(name);
                    permission.setDescription(description);
                    permission.setActive(true);
                    log.info("Creating permission: {}", name);
                    return permissionRepository.save(permission);
                });
    }
    
    private Role createRoleIfNotExists(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(description);
                    role.setActive(true);
                    log.info("Creating role: {}", name);
                    return roleRepository.save(role);
                });
    }
    
    private void addPermissionIfExists(Set<Permission> permissions, String permissionName) {
        permissionRepository.findByName(permissionName).ifPresent(permissions::add);
    }
    
    private void createDefaultAdminUser(Role adminRole) {
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullName(adminName);
            admin.setActive(true);
            
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);
            
            userRepository.save(admin);
            log.info("Default admin user created: {}", adminEmail);
        } else {
            log.info("Admin user already exists: {}", adminEmail);
        }
    }
}