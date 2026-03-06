package group.g.graduation.backend.user.dto.preference;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * طلب تحديد موقع المستخدم
 * يمكن إرسال اسم المدينة أو إحداثيات GPS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "طلب تحديد موقع المستخدم - يمكن إرسال اسم المدينة أو إحداثيات GPS")
public class SetLocationRequest {

    @Schema(description = "اسم المدينة بالعربي أو الإنجليزي (مطلوب إذا لم يتم إرسال إحداثيات)", example = "نابلس")
    private String city;

    @Schema(description = "خط العرض (مطلوب إذا لم يتم إرسال اسم المدينة)", example = "32.2211")
    private Double latitude;

    @Schema(description = "خط الطول (مطلوب إذا لم يتم إرسال اسم المدينة)", example = "35.2544")
    private Double longitude;

    /**
     * Validates that either city or (latitude + longitude) is provided.
     */
    public boolean isValid() {
        boolean hasCity = city != null && !city.isBlank();
        boolean hasCoordinates = latitude != null && longitude != null;
        return hasCity || hasCoordinates;
    }
}
