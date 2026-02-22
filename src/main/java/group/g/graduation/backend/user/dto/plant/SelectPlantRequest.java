package group.g.graduation.backend.user.dto.plant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SelectPlantRequest - طلب اختيار نبتة من الاقتراحات
 * اليوزر بيختار نبتة واحدة من الاقتراحات عشان يزرعها
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectPlantRequest {

    @NotNull(message = "معرّف الجلسة مطلوب")
    private String sessionId;

    @NotNull(message = "معرّف النبتة مطلوب")
    private Long plantId;

    private String nickname;  // اسم مخصص للنبتة (اختياري)
    private String notes;     // ملاحظات (اختياري)
}
