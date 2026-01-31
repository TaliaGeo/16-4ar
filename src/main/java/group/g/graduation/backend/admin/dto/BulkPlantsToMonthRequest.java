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
 * DTO for bulk adding plants to a month
 * طلب إضافة عدة نباتات لشهر واحد
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب إضافة عدة نباتات لشهر واحد")
public class BulkPlantsToMonthRequest {
    
    @NotEmpty(message = "Plants list is required")
    @Schema(description = "قائمة النباتات المراد إضافتها")
    private List<PlantEntry> plants;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlantEntry {
        @NotNull(message = "Plant ID is required")
        private Long plantId;
        private String plantingNoteAr;
        private String plantingNoteEn;
    }
}
