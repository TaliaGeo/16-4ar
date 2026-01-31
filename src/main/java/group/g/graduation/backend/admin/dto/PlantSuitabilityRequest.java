package group.g.graduation.backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for plant suitability request - طلب إضافة/تعديل ملاءمة نبات
 * Note: Validation is done manually in service for create operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantSuitabilityRequest {
    
    private Long plantId;  // Required for create, optional for update
    
    private Long optionId;  // Required for create, optional for update
    
    @Min(value = 0, message = "نقاط الملاءمة يجب أن تكون 0 على الأقل")
    @Max(value = 100, message = "نقاط الملاءمة يجب أن لا تتجاوز 100")
    private Integer score;  // 0-100: 0=غير مناسب، 50=متوسط، 100=مثالي
    
    @Size(max = 1000, message = "نصيحة التعديل يجب أن لا تتجاوز 1000 حرف")
    private String adjustmentTipAr;  // نصيحة للمستخدم إذا الظروف مش مثالية
    
    @Size(max = 1000, message = "نصيحة التعديل يجب أن لا تتجاوز 1000 حرف")
    private String adjustmentTipEn;
}
