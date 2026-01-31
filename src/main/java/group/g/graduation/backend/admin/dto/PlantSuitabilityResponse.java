package group.g.graduation.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for plant suitability response - استجابة ملاءمة نبات
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantSuitabilityResponse {
    
    private Long id;
    
    // معلومات النبات
    private Long plantId;
    private String plantNameAr;
    private String plantNameEn;
    
    // معلومات الخيار
    private Long optionId;
    private String optionKey;
    private String optionTextAr;
    private String optionTextEn;
    
    // معلومات السؤال (للسياق)
    private Long questionId;
    private String questionKey;
    private String questionTextAr;
    
    // نقاط الملاءمة
    private Integer score;
    private String scoreLevel;  // "excellent", "good", "fair", "poor"
    
    // نصيحة التعديل
    private String adjustmentTipAr;
    private String adjustmentTipEn;
}
