package group.g.graduation.backend.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for planting question request - طلب إضافة/تعديل سؤال
 * ملاحظة: questionTextAr و questionKey مطلوبين فقط عند الإنشاء، يتم التحقق في الـ Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantingQuestionRequest {
    
    @Size(max = 500, message = "نص السؤال يجب أن لا يتجاوز 500 حرف")
    private String questionTextAr;
    
    @Size(max = 500, message = "نص السؤال بالإنجليزي يجب أن لا يتجاوز 500 حرف")
    private String questionTextEn;
    
    @Size(max = 50, message = "مفتاح السؤال يجب أن لا يتجاوز 50 حرف")
    private String questionKey;  // site, light, container, water, soil
    
    private Boolean isRequired = true;
    
    private Boolean allowMultiple = false;  // للتفضيلات فقط
    
    private Integer displayOrder;
    
    private Boolean isActive = true;
    
    // خيارات السؤال (اختياري - يمكن إضافتها لاحقاً)
    private List<QuestionOptionRequest> options;
}
