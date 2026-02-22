package group.g.graduation.backend.user.dto.plant;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RecommendationListResponse - استجابة قائمة الاقتراحات
 * النباتات المقترحة بعد إجابة اليوزر على الأسئلة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationListResponse {

    private String sessionId;                            // معرّف الجلسة
    private int totalRecommendations;                    // عدد الاقتراحات
    private String summaryAr;                            // ملخص عربي
    private String summaryEn;                            // ملخص إنجليزي
    private List<String> userConditionsSummary;           // ملخص ظروف اليوزر
    private List<RecommendedPlant> recommendations;      // قائمة الاقتراحات

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedPlant {
        private Long plantId;
        private String nameAr;                           // اسم النبتة بالعربي
        private String nameEn;                           // اسم النبتة بالإنجليزي
        private String nameScientific;                   // الاسم العلمي
        private String imageUrl;                         // صورة النبتة الرئيسية
        private Double matchPercentage;                  // نسبة التطابق (0-100)
        private String matchLevel;                       // excellent, good, fair, poor
        private String matchLevelAr;                     // مناسب, مناسب مع تعديل, الخ
        private List<String> quickTagsAr;                // تاجات سريعة (ضوء، تربة، ري)
        private List<String> quickTagsEn;                // Quick tags (light, soil, water)
        private Boolean needsAdjustment;                 // هل يحتاج تعديل
        private String conditionsSummaryAr;              // ملخص الظروف بالعربي
        private String conditionsSummaryEn;              // ملخص الظروف بالإنجليزي
    }
}
