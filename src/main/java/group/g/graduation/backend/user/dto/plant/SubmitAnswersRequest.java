package group.g.graduation.backend.user.dto.plant;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SubmitAnswersRequest - طلب تقديم إجابات أسئلة إضافة المحصول
 * اليوزر بيبعث إجاباته على الأسئلة الإجبارية والاختيارية
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswersRequest {

    @NotEmpty(message = "يجب الإجابة على الأسئلة الإجبارية على الأقل")
    private List<QuestionAnswer> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAnswer {
        @NotNull(message = "معرّف السؤال مطلوب")
        private Long questionId;

        @NotEmpty(message = "يجب اختيار خيار واحد على الأقل")
        private List<Long> selectedOptionIds;  // قائمة الخيارات المختارة (ممكن أكثر من واحد)
    }
}
