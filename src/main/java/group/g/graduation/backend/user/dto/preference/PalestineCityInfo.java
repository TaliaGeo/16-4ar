package group.g.graduation.backend.user.dto.preference;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * معلومات مدينة فلسطينية
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "معلومات مدينة في فلسطين")
public class PalestineCityInfo {

    @Schema(description = "اسم المدينة بالعربي", example = "نابلس")
    private String nameAr;

    @Schema(description = "اسم المدينة بالإنجليزي", example = "Nablus")
    private String nameEn;

    @Schema(description = "خط العرض", example = "32.2211")
    private Double latitude;

    @Schema(description = "خط الطول", example = "35.2544")
    private Double longitude;
}
