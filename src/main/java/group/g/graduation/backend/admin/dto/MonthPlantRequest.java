package group.g.graduation.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for adding a plant to a month - طلب إضافة نبتة لشهر
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب إضافة نبتة لشهر زراعة")
public class MonthPlantRequest {
    
    @NotNull(message = "معرف النبتة مطلوب")
    @Schema(description = "معرف النبتة", example = "1", required = true)
    private Long plantId;
    
    @Size(max = 500, message = "ملاحظة الزراعة يجب أن لا تتجاوز 500 حرف")
    @Schema(description = "ملاحظة خاصة للزراعة في هذا الشهر بالعربي", example = "أفضل وقت للزراعة في بداية الشهر")
    private String plantingNoteAr;
    
    @Size(max = 500, message = "Planting note must not exceed 500 characters")
    @Schema(description = "ملاحظة خاصة للزراعة في هذا الشهر بالإنجليزي", example = "Best time to plant is at the beginning of the month")
    private String plantingNoteEn;
}
