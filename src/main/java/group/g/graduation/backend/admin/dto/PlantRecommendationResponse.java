package group.g.graduation.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for plant recommendation response - استجابة توصية النبات
 * النتيجة النهائية بعد تحليل إجابات المستخدم
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantRecommendationResponse {
    
    // معلومات النبات
    private Long plantId;
    private String nameAr;
    private String nameEn;
    private String nameScientific;
    private String shortDescriptionAr;
    private String shortDescriptionEn;
    private String imageUrl;
    
    // درجة التوافق
    private Integer totalScore;          // مجموع النقاط
    private Integer maxPossibleScore;    // أقصى نقاط ممكنة
    private Double matchPercentage;      // نسبة التوافق (0-100%)
    private String matchLevel;           // "excellent", "good", "fair", "poor"
    private String matchLevelAr;         // "ممتاز", "جيد", "مقبول", "ضعيف"
    
    // تفاصيل التوافق لكل سؤال
    private List<QuestionMatchDetail> questionMatches;
    
    // نصائح للتحسين
    private List<String> adjustmentTips;
    
    /**
     * تفاصيل توافق سؤال واحد
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionMatchDetail {
        private Long questionId;
        private String questionKey;
        private String questionTextAr;
        private String questionTextEn;
        private String selectedOptionText;
        private Integer score;
        private String scoreLevel;
    }
}
