package group.g.graduation.backend.user.dto.crop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * صفحة المهام لنبتة مزروعة - التبويب الثاني
 * مطابق لتصميم الشاشة: فلاتر (مهام العناية، الكل، اليوم، متأخرة) + قائمة المهام
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantedTasksResponse {
    private Long userPlantId;
    private String plantNameAr;
    private String plantNameEn;
    private String status; // PLANTED
    private int totalTasks;
    private int pendingCount;
    private int dueTodayCount;
    private int overdueCount;
    private int completedCount;
    private int snoozedCount;
    private List<UserPlantTaskResponse> tasks;
}
