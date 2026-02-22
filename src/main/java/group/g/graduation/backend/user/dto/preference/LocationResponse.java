package group.g.graduation.backend.user.dto.preference;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * استجابة بيانات موقع المستخدم
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "بيانات موقع المستخدم")
public class LocationResponse {

    @Schema(description = "اسم المدينة بالعربي", example = "نابلس")
    private String cityAr;

    @Schema(description = "اسم المدينة بالإنجليزي", example = "Nablus")
    private String cityEn;

    @Schema(description = "خط العرض", example = "32.2211")
    private Double latitude;

    @Schema(description = "خط الطول", example = "35.2544")
    private Double longitude;

    @Schema(description = "المنطقة", example = "فلسطين")
    private String region;
}
