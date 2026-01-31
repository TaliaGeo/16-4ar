package group.g.graduation.backend.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for plant recommendation request - طلب توصية النباتات
 * إجابات المستخدم على الأسئلة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantRecommendationRequest {
    
    /**
     * إجابات المستخدم
     * Key: questionKey (site, light, container, water, soil)
     * Value: optionKey أو List<optionKey> للأسئلة متعددة الاختيارات
     * 
     * مثال:
     * {
     *   "site": "balcony",
     *   "light": "partial_sun",
     *   "container": "medium_pot",
     *   "water": "every_2_3",
     *   "soil": "potting_mix",
     *   "prefs": ["beginner", "tea", "aromatic"]
     * }
     */
    @NotEmpty(message = "إجابات الأسئلة مطلوبة")
    private Map<String, Object> answers;
    
    /**
     * عدد التوصيات المطلوبة (اختياري، افتراضي 5)
     */
    private Integer limit;
    
    /**
     * الحد الأدنى لنسبة التوافق (اختياري، افتراضي 30%)
     */
    private Double minMatchPercentage;
}
