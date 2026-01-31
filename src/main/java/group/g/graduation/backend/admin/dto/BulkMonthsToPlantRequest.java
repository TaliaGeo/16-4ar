package group.g.graduation.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk adding months to a plant
 * طلب إضافة نبتة لعدة أشهر
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب إضافة نبتة واحدة لعدة أشهر")
public class BulkMonthsToPlantRequest {
    
    @NotEmpty(message = "Months list is required")
    @Schema(description = "قائمة الأشهر المراد إضافة النبتة لها")
    private List<MonthEntry> months;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthEntry {
        @NotNull(message = "Month ID is required")
        private Long monthId;
        private String plantingNoteAr;
        private String plantingNoteEn;
    }
}
