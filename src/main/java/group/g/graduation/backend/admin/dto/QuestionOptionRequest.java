package group.g.graduation.backend.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for question option request - طلب إضافة/تعديل خيار
 * ملاحظة: optionTextAr و optionKey مطلوبين فقط عند الإنشاء، يتم التحقق في الـ Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionRequest {
    
    @Size(max = 255, message = "نص الخيار يجب أن لا يتجاوز 255 حرف")
    private String optionTextAr;
    
    @Size(max = 255, message = "نص الخيار بالإنجليزي يجب أن لا يتجاوز 255 حرف")
    private String optionTextEn;
    
    @Size(max = 50, message = "مفتاح الخيار يجب أن لا يتجاوز 50 حرف")
    private String optionKey;  // indoor, balcony, full_sun, etc.
    
    private Integer displayOrder;
    
    private Boolean isActive = true;
}
