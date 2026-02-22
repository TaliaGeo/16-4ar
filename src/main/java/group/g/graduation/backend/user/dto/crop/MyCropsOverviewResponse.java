package group.g.graduation.backend.user.dto.crop;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * نظرة عامة على صفحة محاصيلي - الأقسام الثلاثة
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyCropsOverviewResponse {
    private long plannedCount;
    private long plantedCount;
    private long harvestedCount;
    private List<UserPlantCardResponse> planned;
    private List<UserPlantCardResponse> planted;
    private List<UserPlantCardResponse> harvested;
}
