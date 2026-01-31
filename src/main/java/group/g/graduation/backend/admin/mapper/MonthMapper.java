package group.g.graduation.backend.admin.mapper;

import group.g.graduation.backend.admin.dto.MonthPlantRequest;
import group.g.graduation.backend.admin.dto.MonthPlantResponse;
import group.g.graduation.backend.admin.dto.MonthRequest;
import group.g.graduation.backend.admin.dto.MonthResponse;
import group.g.graduation.backend.common.enums.Season;
import group.g.graduation.backend.common.model.Month;
import group.g.graduation.backend.common.model.MonthPlant;
import group.g.graduation.backend.common.model.Plant;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Month and MonthPlant entities
 * محوّل كيانات الشهر وعلاقة الشهر بالنبتة
 */
@Component
public class MonthMapper {
    
    /**
     * تحويل طلب الشهر إلى كيان جديد
     */
    public Month toEntity(MonthRequest request) {
        if (request == null) return null;
        
        return Month.builder()
                .monthNumber(request.getMonthNumber())
                .nameAr(request.getNameAr())
                .nameEn(request.getNameEn())
                .season(request.getSeason())
                .weatherDescriptionAr(request.getWeatherDescriptionAr())
                .weatherDescriptionEn(request.getWeatherDescriptionEn())
                .imageUrl(request.getImageUrl())
                .build();
    }
    
    /**
     * تحديث كيان الشهر من الطلب
     */
    public void updateEntity(Month month, MonthRequest request) {
        if (month == null || request == null) return;
        
        month.setMonthNumber(request.getMonthNumber());
        month.setNameAr(request.getNameAr());
        month.setNameEn(request.getNameEn());
        month.setSeason(request.getSeason());
        month.setWeatherDescriptionAr(request.getWeatherDescriptionAr());
        month.setWeatherDescriptionEn(request.getWeatherDescriptionEn());
        month.setImageUrl(request.getImageUrl());
    }
    
    /**
     * تحويل كيان الشهر إلى استجابة (بدون تفاصيل النباتات)
     */
    public MonthResponse toResponse(Month month) {
        return toResponse(month, false);
    }
    
    /**
     * تحويل كيان الشهر إلى استجابة
     */
    public MonthResponse toResponse(Month month, boolean includePlants) {
        if (month == null) return null;
        
        MonthResponse.MonthResponseBuilder builder = MonthResponse.builder()
                .id(month.getId())
                .monthNumber(month.getMonthNumber())
                .nameAr(month.getNameAr())
                .nameEn(month.getNameEn())
                .season(month.getSeason())
                .seasonNameAr(getSeasonNameAr(month.getSeason()))
                .weatherDescriptionAr(month.getWeatherDescriptionAr())
                .weatherDescriptionEn(month.getWeatherDescriptionEn())
                .imageUrl(month.getImageUrl());
        
        if (month.getMonthPlants() != null) {
            builder.plantsCount(month.getMonthPlants().size());
            
            if (includePlants) {
                builder.plants(month.getMonthPlants().stream()
                        .map(mp -> MonthResponse.MonthPlantSummary.builder()
                                .plantId(mp.getPlant().getId())
                                .plantNameAr(mp.getPlant().getNameAr())
                                .plantNameEn(mp.getPlant().getNameEn())
                                .plantingNoteAr(mp.getPlantingNoteAr())
                                .build())
                        .collect(Collectors.toList()));
            }
        } else {
            builder.plantsCount(0);
            builder.plants(Collections.emptyList());
        }
        
        return builder.build();
    }
    
    /**
     * إنشاء كيان علاقة شهر-نبتة
     */
    public MonthPlant toEntity(MonthPlantRequest request, Month month, Plant plant) {
        if (request == null || month == null || plant == null) return null;
        
        return MonthPlant.builder()
                .month(month)
                .plant(plant)
                .plantingNoteAr(request.getPlantingNoteAr())
                .plantingNoteEn(request.getPlantingNoteEn())
                .build();
    }
    
    /**
     * تحويل كيان علاقة شهر-نبتة إلى استجابة
     */
    public MonthPlantResponse toMonthPlantResponse(MonthPlant monthPlant) {
        if (monthPlant == null) return null;
        
        return MonthPlantResponse.builder()
                .id(monthPlant.getId())
                .monthId(monthPlant.getMonth().getId())
                .monthNumber(monthPlant.getMonth().getMonthNumber())
                .monthNameAr(monthPlant.getMonth().getNameAr())
                .monthNameEn(monthPlant.getMonth().getNameEn())
                .plantId(monthPlant.getPlant().getId())
                .plantNameAr(monthPlant.getPlant().getNameAr())
                .plantNameEn(monthPlant.getPlant().getNameEn())
                .plantingNoteAr(monthPlant.getPlantingNoteAr())
                .plantingNoteEn(monthPlant.getPlantingNoteEn())
                .build();
    }
    
    /**
     * تحويل قائمة علاقات شهر-نبتة إلى استجابات
     */
    public List<MonthPlantResponse> toMonthPlantResponseList(List<MonthPlant> monthPlants) {
        if (monthPlants == null) return Collections.emptyList();
        
        return monthPlants.stream()
                .map(this::toMonthPlantResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * تحويل قائمة الشهور إلى استجابات
     */
    public List<MonthResponse> toResponseList(List<Month> months) {
        if (months == null) return Collections.emptyList();
        
        return months.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * الحصول على اسم الفصل بالعربي
     */
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
