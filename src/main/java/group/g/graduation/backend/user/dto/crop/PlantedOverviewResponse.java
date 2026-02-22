package group.g.graduation.backend.user.dto.crop;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * نظرة عامة على نبتة مزروعة - التبويب الأول
 * مطابق لتصميم الشاشة: اسم + صورة + رسالة ترحيب + أيام الزراعة + آخر ري + الري القادم
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantedOverviewResponse {
    private Long userPlantId;
    private Long plantId;
    private String plantNameAr;
    private String plantNameEn;
    private String nameScientific;
    private String imageUrl;
    private List<String> allImageUrls;
    private String nickname;
    private String status; // PLANTED

    // رسالة ترحيب
    private String welcomeMessageAr;
    private String welcomeMessageEn;

    // تاريخ الزراعة وعدد الأيام
    private LocalDate plantedDate;
    private int daysSincePlanting;
    private String daysSincePlantingTextAr;  // "اليوم 12 من الزراعة"
    private String daysSincePlantingTextEn;  // "Day 12 since planting"

    // آخر ري
    private String lastWateringDate;
    private String lastWateringRelativeAr;   // "قبل يومين"
    private String lastWateringRelativeEn;   // "2 days ago"

    // الري القادم
    private String nextWateringDate;
    private String nextWateringRelativeAr;   // "بعد 6 ساعات"
    private String nextWateringRelativeEn;   // "In 6 hours"

    private Integer wateringIntervalDays;
    private String difficultyLevel;
    private String category;
}
