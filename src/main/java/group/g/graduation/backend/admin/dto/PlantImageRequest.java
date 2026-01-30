package group.g.graduation.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for adding plant image - طلب إضافة صورة للنبتة
 * يستخدم عند إضافة صورة برابط مباشر (ليس رفع ملف)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantImageRequest {
    
    @NotBlank(message = "رابط الصورة مطلوب")
    @Size(max = 500, message = "رابط الصورة يجب أن لا يتجاوز 500 حرف")
    private String imageUrl;
    
    @Size(max = 200, message = "النص البديل يجب أن لا يتجاوز 200 حرف")
    private String altTextAr;
    
    @Size(max = 200, message = "Alt text must not exceed 200 characters")
    private String altTextEn;
    
    private Boolean isPrimary;  // هل هي الصورة الرئيسية
    
    private Integer displayOrder;  // ترتيب العرض
}
