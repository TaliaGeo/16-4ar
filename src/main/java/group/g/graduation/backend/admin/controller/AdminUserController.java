package group.g.graduation.backend.admin.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.admin.dto.AdminUserResponse;
import group.g.graduation.backend.admin.dto.CreateAdminRequest;
import group.g.graduation.backend.admin.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin User Controller - تحكم إدارة صلاحيات المسؤولين
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Management", description = "APIs لإدارة صلاحيات المسؤولين - Admin Role Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    
    private final AdminUserService adminUserService;
    
    // ===== Admin Creation (Initial Setup Only) =====
    
    /**
     * إنشاء حساب مسؤول جديد
     * هذا الـ endpoint يجب تعطيله بعد الإعداد الأولي
     */
    @PostMapping("/create-admin")
    @Operation(
        summary = "إنشاء حساب مسؤول",
        description = "إنشاء حساب مسؤول جديد - للإعداد الأولي فقط"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "تم إنشاء المسؤول بنجاح"),
        @ApiResponse(responseCode = "400", description = "بيانات غير صالحة أو البريد موجود")
    })
    @PreAuthorize("hasRole('ADMIN') or @adminUserService.hasAdminUser() == false")
    public ResponseEntity<AdminUserResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        log.info("Creating admin account: {}", request.getEmail());
        
        User admin = adminUserService.createAdminUser(
            request.getEmail(),
            request.getPassword(),
            request.getFullName()
        );
        
        AdminUserResponse response = mapToResponse(admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // ===== Role Management =====
    
    /**
     * ترقية مستخدم إلى مسؤول
     */
    @PutMapping("/{userId}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "ترقية مستخدم إلى مسؤول",
        description = "إضافة صلاحية ROLE_ADMIN لمستخدم"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "تمت الترقية بنجاح"),
        @ApiResponse(responseCode = "400", description = "المستخدم لديه الصلاحية بالفعل"),
        @ApiResponse(responseCode = "404", description = "المستخدم غير موجود")
    })
    public ResponseEntity<Map<String, String>> promoteToAdmin(@PathVariable Long userId) {
        log.info("Promoting user {} to admin", userId);
        
        adminUserService.promoteToAdmin(userId);
        
        return ResponseEntity.ok(Map.of(
            "message", "تمت ترقية المستخدم إلى مسؤول بنجاح - User promoted to admin successfully",
            "userId", userId.toString()
        ));
    }
    
    /**
     * إزالة صلاحية المسؤول من مستخدم
     */
    @PutMapping("/{userId}/demote")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "إزالة صلاحية المسؤول",
        description = "إزالة صلاحية ROLE_ADMIN من مستخدم"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "تمت الإزالة بنجاح"),
        @ApiResponse(responseCode = "400", description = "المستخدم ليس مسؤولاً أو هو آخر مسؤول"),
        @ApiResponse(responseCode = "404", description = "المستخدم غير موجود")
    })
    public ResponseEntity<Map<String, String>> demoteFromAdmin(@PathVariable Long userId) {
        log.info("Demoting user {} from admin", userId);
        
        adminUserService.demoteFromAdmin(userId);
        
        return ResponseEntity.ok(Map.of(
            "message", "تمت إزالة صلاحية المسؤول بنجاح - Admin role removed successfully",
            "userId", userId.toString()
        ));
    }
    
    // ===== System Check =====
    
    /**
     * التحقق من وجود مسؤول في النظام
     */
    @GetMapping("/has-admin")
    @Operation(
        summary = "التحقق من وجود مسؤول",
        description = "التحقق من وجود حساب مسؤول نشط في النظام"
    )
    public ResponseEntity<Map<String, Boolean>> hasAdmin() {
        boolean hasAdmin = adminUserService.hasAdminUser();
        return ResponseEntity.ok(Map.of("hasAdmin", hasAdmin));
    }
    
    // ===== Helper Methods =====
    
    private AdminUserResponse mapToResponse(User user) {
        return AdminUserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .active(user.isActive())
            .roles(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet()))
            .createdAt(user.getCreatedAt())
            .build();
    }
}
