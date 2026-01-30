package group.g.graduation.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create Admin Request DTO - طلب إنشاء حساب مسؤول
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب إنشاء حساب مسؤول - Create Admin Account Request")
public class CreateAdminRequest {
    
    @NotBlank(message = "البريد الإلكتروني مطلوب - Email is required")
    @Email(message = "صيغة البريد الإلكتروني غير صحيحة - Invalid email format")
    @Schema(description = "البريد الإلكتروني", example = "admin@example.com")
    private String email;
    
    @NotBlank(message = "كلمة المرور مطلوبة - Password is required")
    @Size(min = 8, message = "كلمة المرور يجب أن تكون 8 أحرف على الأقل - Password must be at least 8 characters")
    @Schema(description = "كلمة المرور", example = "Admin@123")
    private String password;
    
    @NotBlank(message = "الاسم الكامل مطلوب - Full name is required")
    @Size(min = 2, max = 100, message = "الاسم يجب أن يكون بين 2 و 100 حرف - Name must be between 2 and 100 characters")
    @Schema(description = "الاسم الكامل", example = "أحمد محمد")
    private String fullName;
}
