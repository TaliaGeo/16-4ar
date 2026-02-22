package group.g.graduation.backend.user.dto.plant;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SelectPlantResponse - استجابة بعد اختيار النبتة
 * تأكيد أنو النبتة انضافت لمحاصيل المستخدم
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectPlantResponse {

    private Long userPlantId;                // معرّف نبتة المستخدم
    private Long plantId;                    // معرّف النبتة
    private String plantNameAr;              // اسم النبتة بالعربي
    private String plantNameEn;              // اسم النبتة بالإنجليزي
    private String plantNameScientific;      // الاسم العلمي
    private String imageUrl;                 // الصورة
    private String nickname;                 // الاسم المخصص
    private String status;                   // الحالة (PLANNED)
    private LocalDate plannedDate;           // تاريخ التخطيط
    private String messageAr;               // رسالة نجاح بالعربي
    private String messageEn;               // رسالة نجاح بالإنجليزي
}
