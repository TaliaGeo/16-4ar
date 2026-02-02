package group.g.graduation.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating month-plant relationship - طلب تحديث علاقة شهر-نبتة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب تحديث ملاحظات علاقة شهر-نبتة")
public class MonthPlantUpdateRequest {
    
    @Size(max = 500, message = "ملاحظة الزراعة يجب أن لا تتجاوز 500 حرف")
    @Schema(description = "ملاحظة خاصة للزراعة في هذا الشهر بالعربي", example = "أفضل وقت للزراعة في بداية الشهر")
    private String plantingNoteAr;
    
    @Size(max = 500, message = "Planting note must not exceed 500 characters")
    @Schema(description = "ملاحظة خاصة للزراعة في هذا الشهر بالإنجليزي", example = "Best time to plant is at the beginning of the month")
    private String plantingNoteEn;
}