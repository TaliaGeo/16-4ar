package group.g.graduation.backend.user.dto.crop;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * مهمة خاصة بنبتة المستخدم
 * مطابق لتصميم الشاشة: اسم + أيقونة + وصف + وقت + حالة + أزرار
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPlantTaskResponse {
    private Long taskId;
    private String taskNameAr;          // "ري"
    private String taskNameEn;          // "Watering"
    private String icon;                // "💧"
    private String descriptionAr;       // "اسقِ التربة حتى تصبح رطبة بدون إغراق..."
    private String descriptionEn;
    private LocalDate dueDate;
    private String dueTimeDisplay;      // "الساعة 6:00 م" أو "بعد 5 أيام"
    private String dueDateRelativeAr;   // "مستو فوم" / "متأخر" / "بعد 5 أيام" / "قادمة"
    private String dueDateRelativeEn;   // "Due today" / "Overdue" / "In 5 days" / "Upcoming"
    private String status;              // PENDING, DUE_TODAY, OVERDUE, COMPLETED, SNOOZED
    private String completedAt;
    private boolean isDueToday;
    private boolean isOverdue;
}
