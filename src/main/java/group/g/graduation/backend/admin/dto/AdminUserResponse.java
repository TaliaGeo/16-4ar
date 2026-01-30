package group.g.graduation.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * Admin User Response DTO - استجابة بيانات المسؤول
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "بيانات المسؤول - Admin User Data")
public class AdminUserResponse {
    
    @Schema(description = "معرف المستخدم", example = "1")
    private Long id;
    
    @Schema(description = "البريد الإلكتروني", example = "admin@example.com")
    private String email;
    
    @Schema(description = "الاسم الكامل", example = "أحمد محمد")
    private String fullName;
    
    @Schema(description = "حالة الحساب", example = "true")
    private boolean active;
    
    @Schema(description = "الصلاحيات", example = "[\"ROLE_ADMIN\", \"ROLE_USER\"]")
    private Set<String> roles;
    
    @Schema(description = "تاريخ الإنشاء")
    private Instant createdAt;
}
