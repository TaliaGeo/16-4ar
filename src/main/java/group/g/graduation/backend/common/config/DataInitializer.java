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
 * - ROLE_ADMIN: صلاحيات المسؤول
 * - ROLE_USER: صلاحيات المستخدم العادي
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
            
            // إنشاء ROLE_ADMIN
            createRoleIfNotExists("ROLE_ADMIN", "Administrator role with full access - صلاحيات المسؤول الكاملة");
            
            // إنشاء ROLE_USER
            createRoleIfNotExists("ROLE_USER", "Regular user role - صلاحيات المستخدم العادي");
            
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
