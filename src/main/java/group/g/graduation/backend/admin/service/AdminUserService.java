package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.Security.model.Role;
import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.RoleRepository;
import group.g.graduation.backend.Security.repository.UserRepository;
import group.g.graduation.backend.common.exception.BadRequestException;
import group.g.graduation.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin User Service - خدمة إدارة صلاحيات المسؤولين
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminUserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * الحصول على صلاحية المسؤول
     * Get admin role
     */
    private Role getAdminRole() {
        return roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ADMIN"));
    }
    
    /**
     * الحصول على صلاحية المستخدم العادي
     * Get user role
     */
    private Role getUserRole() {
        return roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "USER"));
    }
    
    /**
     * ترقية مستخدم إلى مسؤول
     * Promote a user to admin role
     */
    @Transactional
    public void promoteToAdmin(Long userId) {
        log.info("Promoting user {} to admin", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Role adminRole = getAdminRole();
        
        // Idempotent: if already admin, just return success
        if (user.getRoles().contains(adminRole) && user.getRoles().size() == 1) {
            log.info("User {} already has ADMIN role only, no changes needed", userId);
            return;
        }
        
        // Clear all existing roles
        user.getRoles().clear();
        
        // Add ADMIN role only (user should have single role)
        user.getRoles().add(adminRole);
        
        userRepository.save(user);
        
        log.info("User {} promoted to admin successfully. Final role: ADMIN", userId);
    }
    
    /**
     * إزالة صلاحية المسؤول من مستخدم
     * Remove admin role from a user
     */
    @Transactional
    public void demoteFromAdmin(Long userId) {
        log.info("Demoting user {} from admin", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Role adminRole = getAdminRole();
        Role userRole = getUserRole();
        
        if (!user.getRoles().contains(adminRole)) {
            throw new BadRequestException("المستخدم ليس مسؤولاً - User is not an admin");
        }
        
        // Idempotent: if already USER only, just return success
        if (user.getRoles().contains(userRole) && user.getRoles().size() == 1) {
            log.info("User {} already has USER role only, no changes needed", userId);
            return;
        }
        
        // التحقق من عدم إزالة آخر مسؤول
        long adminCount = userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(adminRole))
                .count();
        
        if (adminCount <= 1) {
            throw new BadRequestException("لا يمكن إزالة آخر مسؤول في النظام - Cannot remove the last admin");
        }
        
        // Clear all existing roles
        user.getRoles().clear();
        
        // Add USER role only (user should have single role)
        user.getRoles().add(userRole);
        
        userRepository.save(user);
        
        log.info("User {} demoted from admin successfully. Final role: USER", userId);
    }
    
    /**
     * إنشاء حساب مسؤول جديد (للاستخدام الأولي)
     * Create a new admin account (for initial setup)
     */
    public User createAdminUser(String email, String password, String fullName) {
        log.info("Creating admin user: {}", email);
        
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("البريد الإلكتروني مستخدم بالفعل - Email already exists");
        }
        
        Role adminRole = getAdminRole();
        
        User admin = new User();
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setFullName(fullName);
        admin.setActive(true);
        // Admin users should only have ADMIN role, not USER role
        admin.getRoles().add(adminRole);
        
        User savedAdmin = userRepository.save(admin);
        log.info("Admin user created successfully with ADMIN role only: {}", savedAdmin.getId());
        
        return savedAdmin;
    }
    
    /**
     * التحقق من وجود مسؤول في النظام
     * Check if system has at least one admin
     */
    @Transactional(readOnly = true)
    public boolean hasAdminUser() {
        // Try both naming conventions (ADMIN and ROLE_ADMIN)
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .or(() -> roleRepository.findByName("ADMIN"))
                .orElse(null);
        
        if (adminRole == null) return false;
        
        return userRepository.findAll().stream()
                .anyMatch(u -> u.getRoles().contains(adminRole) && u.isActive());
    }
}
