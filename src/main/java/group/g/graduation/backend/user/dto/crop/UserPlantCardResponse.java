package group.g.graduation.backend.user.dto.crop;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * كارد نبتة المستخدم - يظهر في القوائم
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPlantCardResponse {
    private Long userPlantId;
    private Long plantId;
    private String plantNameAr;
    private String plantNameEn;
    private String imageUrl;
    private String nickname;
    private String status;
    private LocalDate plannedDate;
    private LocalDate plantedDate;
    private int pendingTasksCount;
}
