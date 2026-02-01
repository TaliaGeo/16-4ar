package group.g.graduation.backend.common.config;

import group.g.graduation.backend.Security.model.Role;
import group.g.graduation.backend.Security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Data Initializer - تهيئة البيانات الأساسية
 * 
 * يتم تشغيله عند بدء التطبيق لإنشاء:
 * - ADMIN: صلاحيات المسؤول
 * - USER: صلاحيات المستخدم العادي
 * 
 * ملاحظة: الأسماء بدون ROLE_ لأن UserDetailsServiceImpl يضيف ROLE_ تلقائياً
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    
    private final RoleRepository roleRepository;
    
    @Bean
    @Order(1)
    public CommandLineRunner initRoles() {
        return args -> {
            log.info("🔄 Initializing roles...");
            
            // إنشاء ADMIN (يصبح ROLE_ADMIN في Spring Security)
            createRoleIfNotExists("ADMIN", "Administrator role with full access - صلاحيات المسؤول الكاملة");
            
            // إنشاء USER (يصبح ROLE_USER في Spring Security)
            createRoleIfNotExists("USER", "Regular user role - صلاحيات المستخدم العادي");
            
            log.info("✅ Roles initialized successfully");
        };
    }
    
    /**
     * إنشاء الصلاحية إذا لم تكن موجودة
     */
    private void createRoleIfNotExists(String name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setActive(true);
            roleRepository.save(role);
            log.info("📌 Created role: {}", name);
        } else {
            log.debug("Role already exists: {}", name);
        }
    }
}
