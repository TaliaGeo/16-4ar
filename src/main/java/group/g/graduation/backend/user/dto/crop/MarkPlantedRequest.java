package group.g.graduation.backend.user.dto.crop;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * طلب تغيير حالة النبتة (زرعت / حصدت)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkPlantedRequest {
    @NotNull(message = "معرّف نبتة المستخدم مطلوب")
    private Long userPlantId;
    private String nickname;
}
