package group.g.graduation.backend.admin.dto;

import group.g.graduation.backend.common.enums.Season;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating a month - طلب إنشاء/تعديل شهر
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب إنشاء/تعديل شهر")
public class MonthRequest {
    
    @NotNull(message = "رقم الشهر مطلوب")
    @Min(value = 1, message = "رقم الشهر يجب أن يكون بين 1 و 12")
    @Max(value = 12, message = "رقم الشهر يجب أن يكون بين 1 و 12")
    @Schema(description = "رقم الشهر (1-12)", example = "3", required = true)
    private Integer monthNumber;
    
    @NotBlank(message = "اسم الشهر بالعربي مطلوب")
    @Size(max = 50, message = "اسم الشهر يجب أن لا يتجاوز 50 حرف")
    @Schema(description = "اسم الشهر بالعربي", example = "آذار", required = true)
    private String nameAr;
    
    @Size(max = 50, message = "Month name must not exceed 50 characters")
    @Schema(description = "اسم الشهر بالإنجليزي", example = "March")
    private String nameEn;
    
    @NotNull(message = "الفصل مطلوب")
    @Schema(description = "الفصل", example = "SPRING", required = true)
    private Season season;
    
    @Size(max = 500, message = "وصف الطقس يجب أن لا يتجاوز 500 حرف")
    @Schema(description = "وصف حالة الطقس في فلسطين بالعربي", example = "بداية الربيع، الطقس معتدل ومناسب للزراعة")
    private String weatherDescriptionAr;
    
    @Size(max = 500, message = "Weather description must not exceed 500 characters")
    @Schema(description = "وصف حالة الطقس في فلسطين بالإنجليزي", example = "Beginning of spring, moderate weather suitable for planting")
    private String weatherDescriptionEn;
    
    @Size(max = 500, message = "رابط الصورة يجب أن لا يتجاوز 500 حرف")
    @Schema(description = "رابط صورة تعبر عن الشهر", example = "https://example.com/images/march.jpg")
    private String imageUrl;
}
