package group.g.graduation.backend.user.dto.crop;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * طلب تنفيذ إجراء على مهمة (نفذ / تذكير بعد ساعة / تذكير غد / تذكير لاحقا)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskActionRequest {
    @NotNull(message = "معرّف المهمة مطلوب")
    private Long taskId;
    
    @NotNull(message = "نوع الإجراء مطلوب")
    private Action action;

    public enum Action {
        COMPLETE,           // نفذ / تمت
        SNOOZE_1_HOUR,      // تذكير بعد ساعة
        SNOOZE_TOMORROW,    // تذكير غد
        SNOOZE_LATER        // تذكير لاحقا (بعد 3 أيام)
    }
}
