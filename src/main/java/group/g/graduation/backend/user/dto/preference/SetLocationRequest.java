package group.g.graduation.backend.user.dto.preference;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * طلب تحديد موقع المستخدم
 * يستخدمه اليوزر لتحديد مدينته في فلسطين
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب تحديد موقع المستخدم")
public class SetLocationRequest {

    @NotBlank(message = "اسم المدينة مطلوب")
    @Schema(description = "اسم المدينة بالعربي أو الإنجليزي", example = "نابلس")
    private String city;

    @Schema(description = "خط العرض (اختياري - يتم تعبئته تلقائياً)", example = "32.2211")
    private Double latitude;

    @Schema(description = "خط الطول (اختياري - يتم تعبئته تلقائياً)", example = "35.2544")
    private Double longitude;
}
