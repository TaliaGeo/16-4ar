package group.g.graduation.backend.user.service;

import group.g.graduation.backend.common.enums.Season;
import group.g.graduation.backend.common.exception.ResourceNotFoundException;
import group.g.graduation.backend.common.model.Month;
import group.g.graduation.backend.common.model.MonthPlant;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantImage;
import group.g.graduation.backend.common.repository.MonthPlantRepository;
import group.g.graduation.backend.common.repository.MonthRepository;
import group.g.graduation.backend.common.repository.PlantImageRepository;
import group.g.graduation.backend.user.dto.home.MonthDetailResponse;
import group.g.graduation.backend.user.dto.home.MonthlyCalendarResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Monthly Calendar Service - خدمة الكالندر الزراعية
 * تعرض الأشهر مع حالة الطقس ونباتات كل شهر
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MonthlyCalendarService {

    private final MonthRepository monthRepository;
    private final MonthPlantRepository monthPlantRepository;
    private final PlantImageRepository plantImageRepository;

    /**
     * جلب الكالندر الزراعية الكاملة (12 شهر)
     * كل شهر يعرض: الاسم، الفصل، وصف الطقس، عدد النباتات
     */
    public MonthlyCalendarResponse getCalendar() {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();

        List<Month> months = monthRepository.findAllByOrderByMonthNumberAsc();

        List<MonthlyCalendarResponse.CalendarMonth> calendarMonths = months.stream()
                .map(month -> toCalendarMonth(month, currentMonth))
                .collect(Collectors.toList());

        // لو ما في أشهر محفوظة بقاعدة البيانات، نرجع الأشهر الافتراضية
        if (calendarMonths.isEmpty()) {
            log.info("📅 No months found in database, using defaults");
            calendarMonths = buildDefaultCalendarMonths(currentMonth);
        }

        return MonthlyCalendarResponse.builder()
                .months(calendarMonths)
                .currentMonth(currentMonth)
                .currentYear(today.getYear())
                .build();
    }

    /**
     * جلب تفاصيل شهر محدد مع نباتاته
     * عند الضغط على الشهر في الكالندر
     */
    public MonthDetailResponse getMonthDetail(Integer monthNumber) {
        Month month = monthRepository.findByMonthNumberWithPlants(monthNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Month", "monthNumber", monthNumber));

        List<MonthPlant> monthPlants = month.getMonthPlants();

        List<MonthDetailResponse.MonthPlantSuggestion> plantSuggestions = monthPlants.stream()
                .map(this::toPlantSuggestion)
                .collect(Collectors.toList());

        return MonthDetailResponse.builder()
                .monthNumber(month.getMonthNumber())
                .monthNameAr(month.getNameAr())
                .monthNameEn(month.getNameEn())
                .seasonNameAr(getSeasonNameAr(month.getSeason()))
                .weatherDescriptionAr(month.getWeatherDescriptionAr())
                .regionAr("فلسطين - مناخ البحر المتوسط")
                .regionEn("Palestine - Mediterranean climate")
                .plantsCount(plantSuggestions.size())
                .plants(plantSuggestions)
                .build();
    }

    // ===== Mappers =====

    private MonthlyCalendarResponse.CalendarMonth toCalendarMonth(Month month, int currentMonth) {
        return MonthlyCalendarResponse.CalendarMonth.builder()
                .id(month.getId())
                .monthNumber(month.getMonthNumber())
                .nameAr(month.getNameAr())
                .nameEn(month.getNameEn())
                .season(month.getSeason())
                .seasonNameAr(getSeasonNameAr(month.getSeason()))
                .weatherDescriptionAr(month.getWeatherDescriptionAr())
                .weatherDescriptionEn(month.getWeatherDescriptionEn())
                .imageUrl(month.getImageUrl())
                .plantsCount(month.getMonthPlants() != null ? month.getMonthPlants().size() : 0)
                .isCurrent(month.getMonthNumber() != null && month.getMonthNumber() == currentMonth)
                .build();
    }

    private MonthDetailResponse.MonthPlantSuggestion toPlantSuggestion(MonthPlant monthPlant) {
        Plant plant = monthPlant.getPlant();

        // جلب الصورة الرئيسية للنبتة
        String primaryImage = null;
        try {
            Optional<PlantImage> image = plantImageRepository.findByPlantIdAndIsPrimaryTrue(plant.getId());
            primaryImage = image.map(PlantImage::getImageUrl).orElse(null);
        } catch (Exception e) {
            log.debug("Could not fetch primary image for plant {}", plant.getId());
        }

        // بناء ملخص من معلومات العناية
        String summaryAr = buildPlantSummaryAr(plant);
        String summaryEn = buildPlantSummaryEn(plant);

        return MonthDetailResponse.MonthPlantSuggestion.builder()
                .plantId(plant.getId())
                .nameAr(plant.getNameAr())
                .nameEn(plant.getNameEn())
                .shortDescriptionAr(plant.getShortDescriptionAr())
                .shortDescriptionEn(plant.getShortDescriptionEn())
                .summaryAr(summaryAr)
                .summaryEn(summaryEn)
                .primaryImageUrl(primaryImage)
                .plantingNoteAr(monthPlant.getPlantingNoteAr())
                .plantingNoteEn(monthPlant.getPlantingNoteEn())
                .difficultyLevel(plant.getDifficultyLevel() != null ?
                        plant.getDifficultyLevel().name() : null)
                .category(plant.getCategory() != null ? plant.getCategory().name() : null)
                .build();
    }

    /**
     * بناء ملخص عن النبتة بالعربي من بيانات النبتة
     */
    private String buildPlantSummaryAr(Plant plant) {
        StringBuilder sb = new StringBuilder();

        if (plant.getCareInfoAr() != null && !plant.getCareInfoAr().isEmpty()) {
            // أخذ أول 100 حرف من معلومات العناية
            String care = plant.getCareInfoAr();
            sb.append(care.length() > 100 ? care.substring(0, 100) + "..." : care);
        } else if (plant.getLightInfoAr() != null) {
            sb.append("الإضاءة: ").append(truncate(plant.getLightInfoAr(), 50));
            if (plant.getWateringInfoAr() != null) {
                sb.append(" | الري: ").append(truncate(plant.getWateringInfoAr(), 50));
            }
        }

        return sb.length() > 0 ? sb.toString() : plant.getShortDescriptionAr();
    }

    private String buildPlantSummaryEn(Plant plant) {
        StringBuilder sb = new StringBuilder();

        if (plant.getCareInfoEn() != null && !plant.getCareInfoEn().isEmpty()) {
            String care = plant.getCareInfoEn();
            sb.append(care.length() > 100 ? care.substring(0, 100) + "..." : care);
        } else if (plant.getLightInfoEn() != null) {
            sb.append("Light: ").append(truncate(plant.getLightInfoEn(), 50));
            if (plant.getWateringInfoEn() != null) {
                sb.append(" | Watering: ").append(truncate(plant.getWateringInfoEn(), 50));
            }
        }

        return sb.length() > 0 ? sb.toString() : plant.getShortDescriptionEn();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    // ===== Default Calendar =====

    /**
     * أشهر افتراضية في حال عدم وجود بيانات
     */
    private List<MonthlyCalendarResponse.CalendarMonth> buildDefaultCalendarMonths(int currentMonth) {
        String[][] monthData = {
                {"يناير", "January", "WINTER", "بارد ماطر", "Cold rainy"},
                {"فبراير", "February", "WINTER", "بارد", "Cold"},
                {"مارس", "March", "SPRING", "معتدل", "Moderate"},
                {"أبريل", "April", "SPRING", "ربيعي دافئ", "Warm spring"},
                {"مايو", "May", "SPRING", "دافئ", "Warm"},
                {"يونيو", "June", "SUMMER", "حار", "Hot"},
                {"يوليو", "July", "SUMMER", "حار جداً", "Very hot"},
                {"أغسطس", "August", "SUMMER", "حار جاف", "Hot dry"},
                {"سبتمبر", "September", "AUTUMN", "معتدل", "Moderate"},
                {"أكتوبر", "October", "AUTUMN", "خريفي لطيف", "Nice autumn"},
                {"نوفمبر", "November", "AUTUMN", "بارد لطيف", "Cool nice"},
                {"ديسمبر", "December", "WINTER", "بارد ماطر", "Cold rainy"}
        };

        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(i -> {
                    String[] data = monthData[i - 1];
                    return MonthlyCalendarResponse.CalendarMonth.builder()
                            .id((long) i)
                            .monthNumber(i)
                            .nameAr(data[0])
                            .nameEn(data[1])
                            .season(Season.valueOf(data[2]))
                            .seasonNameAr(getSeasonNameAr(Season.valueOf(data[2])))
                            .weatherDescriptionAr(data[3])
                            .weatherDescriptionEn(data[4])
                            .plantsCount(0)
                            .isCurrent(i == currentMonth)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String getSeasonNameAr(Season season) {
        if (season == null) return null;
        return switch (season) {
            case WINTER -> "شتاء";
            case SPRING -> "ربيع";
            case SUMMER -> "صيف";
            case AUTUMN -> "خريف";
        };
    }
}
