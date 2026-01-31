package group.g.graduation.backend.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk plant suitability request - طلب إضافة ملاءمات متعددة
 * لإضافة ملاءمة نبات واحد لعدة خيارات دفعة واحدة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPlantSuitabilityRequest {
    
    @NotEmpty(message = "قائمة الملاءمات مطلوبة")
    private List<PlantSuitabilityRequest> suitabilities;
}
