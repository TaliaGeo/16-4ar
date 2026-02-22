package group.g.graduation.backend.user.dto.plant;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PlantingQuestionsResponse - استجابة أسئلة إضافة المحصول
 * ترجع كل الأسئلة (إجبارية + اختيارية) مع خياراتها
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantingQuestionsResponse {

    private List<QuestionGroup> requiredQuestions;   // الأسئلة الإجبارية
    private List<QuestionGroup> optionalQuestions;   // الأسئلة الاختيارية
    private int totalRequired;                       // عدد الأسئلة الإجبارية
    private int totalOptional;                       // عدد الأسئلة الاختيارية

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionGroup {
        private Long questionId;
        private String questionKey;          // مفتاح السؤال: location, sunlight, etc.
        private String questionTextAr;       // نص السؤال بالعربي
        private String questionTextEn;       // نص السؤال بالإنجليزي
        private Boolean isRequired;          // إجباري أم لا
        private Boolean allowMultiple;       // يسمح باختيار أكثر من خيار
        private Integer displayOrder;        // ترتيب العرض
        private String helpTextAr;           // نص مساعد (تلميحة) بالعربي
        private String helpTextEn;           // نص مساعد بالإنجليزي
        private List<OptionItem> options;    // خيارات السؤال
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItem {
        private Long optionId;
        private String optionKey;            // مفتاح الخيار: indoor, full_sun, etc.
        private String optionTextAr;         // نص الخيار بالعربي
        private String optionTextEn;         // نص الخيار بالإنجليزي
        private Integer displayOrder;        // ترتيب العرض
    }
}
