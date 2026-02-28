package group.g.graduation.backend.user.dto.plant;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SubmitAnswersRequest - طلب تقديم إجابات أسئلة إضافة المحصول
 * اليوزر بيبعث إجاباته على الأسئلة الإجبارية والاختيارية
 * مع إمكانية إرسال موقع GPS الحالي لاقتراحات أذكى
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswersRequest {

    @NotEmpty(message = "يجب الإجابة على الأسئلة الإجبارية على الأقل")
    private List<QuestionAnswer> answers;

    @Schema(description = "خط العرض من GPS (اختياري – لاقتراحات حسب الطقس والموقع)", example = "32.2211")
    private Double latitude;

    @Schema(description = "خط الطول من GPS (اختياري – لاقتراحات حسب الطقس والموقع)", example = "35.2544")
    private Double longitude;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAnswer {
        @NotNull(message = "معرّف السؤال مطلوب")
        private Long questionId;

        @Schema(description = "قائمة الخيارات المختارة (ممكن أكثر من واحد)")
        private List<Long> selectedOptionIds;  // قائمة الخيارات المختارة (ممكن أكثر من واحد)

        /**
         * دعم إرسال خيار واحد بدل قائمة - selectedOptionId (مفرد)
         * بيحوّله لقائمة تلقائياً
         */
        @JsonSetter("selectedOptionId")
        public void setSelectedOptionId(Long optionId) {
            if (optionId != null) {
                this.selectedOptionIds = new ArrayList<>(List.of(optionId));
            }
        }

        /**
         * بيرجع القائمة أو قائمة فاضية لو ما في خيارات
         */
        public List<Long> getSelectedOptionIds() {
            return selectedOptionIds != null ? selectedOptionIds : new ArrayList<>();
        }
    }
}
