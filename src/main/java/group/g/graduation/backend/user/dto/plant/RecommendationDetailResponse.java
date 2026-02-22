package group.g.graduation.backend.user.dto.plant;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RecommendationDetailResponse - تفاصيل اقتراح نبتة واحدة
 * لما اليوزر يضغط على اقتراح معين بيشوف كل التفاصيل
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDetailResponse {

    // ===== معلومات النبتة الأساسية =====
    private Long plantId;
    private String nameAr;
    private String nameEn;
    private String nameScientific;
    private String shortDescriptionAr;
    private String shortDescriptionEn;
    private String imageUrl;
    private String category;
    private String difficultyLevel;

    // ===== نتيجة التطابق =====
    private Double matchPercentage;
    private String matchLevel;
    private String matchLevelAr;
    private Boolean needsAdjustment;

    // ===== تفاصيل كل سؤال =====
    private List<QuestionMatchInfo> questionDetails;

    // ===== ليش اقترحناه =====
    private List<String> recommendationReasonsAr;
    private List<String> recommendationReasonsEn;

    // ===== تعديلات بسيطة لنجاح أفضل =====
    private List<String> adjustmentTipsAr;
    private List<String> adjustmentTipsEn;

    // ===== معلومات العناية =====
    private CareInfo careInfo;

    // ===== تاجات =====
    private List<String> quickTagsAr;
    private List<String> quickTagsEn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionMatchInfo {
        private Long questionId;
        private String questionKey;
        private String questionTextAr;
        private String questionTextEn;
        private String selectedOptionTextAr;     // الخيار اللي اختارو اليوزر
        private String selectedOptionTextEn;
        private Integer score;                    // نقاط (0-100)
        private String scoreLevel;                // excellent, good, fair, poor
        private String scoreLevelAr;              // ممتاز، جيد، مقبول، ضعيف
        private String adjustmentTipAr;           // نصيحة تعديل إن وجدت
        private String adjustmentTipEn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CareInfo {
        private String lightInfoAr;              // معلومات الضوء
        private String lightInfoEn;
        private String soilInfoAr;               // معلومات التربة
        private String soilInfoEn;
        private String wateringInfoAr;           // معلومات الري
        private String wateringInfoEn;
        private Integer wateringIntervalDays;    // كم يوم بين كل ري
        private String careInfoAr;               // نصائح عامة
        private String careInfoEn;
    }
}
