package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.MonthPlantRequest;
import group.g.graduation.backend.admin.dto.MonthPlantResponse;
import group.g.graduation.backend.admin.dto.MonthRequest;
import group.g.graduation.backend.admin.dto.MonthResponse;
import group.g.graduation.backend.admin.mapper.MonthMapper;
import group.g.graduation.backend.common.enums.Season;
import group.g.graduation.backend.common.model.Month;
import group.g.graduation.backend.common.model.MonthPlant;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.repository.MonthPlantRepository;
import group.g.graduation.backend.common.repository.MonthRepository;
import group.g.graduation.backend.common.repository.PlantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of AdminMonthService
 * تنفيذ خدمة إدارة الشهور للأدمن
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminMonthServiceImpl implements AdminMonthService {
    
    private final MonthRepository monthRepository;
    private final MonthPlantRepository monthPlantRepository;
    private final PlantRepository plantRepository;
    private final MonthMapper monthMapper;
    
    // ===================== Month Operations =====================
    
    @Override
    public MonthResponse createMonth(MonthRequest request) {
        log.info("إنشاء شهر جديد برقم: {}", request.getMonthNumber());
        
        // التحقق من عدم وجود شهر بنفس الرقم
        if (monthRepository.findByMonthNumber(request.getMonthNumber()).isPresent()) {
            throw new IllegalArgumentException("يوجد شهر بنفس الرقم: " + request.getMonthNumber());
        }
        
        Month month = monthMapper.toEntity(request);
        Month saved = monthRepository.save(month);
        
        log.info("تم إنشاء الشهر بنجاح - ID: {}", saved.getId());
        return monthMapper.toResponse(saved);
    }
    
    @Override
    public MonthResponse updateMonth(Long id, MonthRequest request) {
        log.info("تحديث الشهر - ID: {}", id);
        
        Month month = findMonthById(id);
        
        // التحقق من عدم وجود شهر آخر بنفس الرقم
        if (!month.getMonthNumber().equals(request.getMonthNumber())) {
            monthRepository.findByMonthNumber(request.getMonthNumber())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("يوجد شهر آخر بنفس الرقم: " + request.getMonthNumber());
                    });
        }
        
        monthMapper.updateEntity(month, request);
        Month updated = monthRepository.save(month);
        
        log.info("تم تحديث الشهر بنجاح - ID: {}", updated.getId());
        return monthMapper.toResponse(updated);
    }
    
    @Override
    @Transactional(readOnly = true)
    public MonthResponse getMonthById(Long id) {
        return monthMapper.toResponse(findMonthById(id));
    }
    
    @Override
    @Transactional(readOnly = true)
    public MonthResponse getMonthByNumber(Integer monthNumber) {
        Month month = monthRepository.findByMonthNumber(monthNumber)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على الشهر رقم: " + monthNumber));
        return monthMapper.toResponse(month);
    }
    
    @Override
    @Transactional(readOnly = true)
    public MonthResponse getMonthWithPlants(Integer monthNumber) {
        Month month = monthRepository.findByMonthNumberWithPlants(monthNumber)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على الشهر رقم: " + monthNumber));
        return monthMapper.toResponse(month, true);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MonthResponse> getAllMonths() {
        List<Month> months = monthRepository.findAllByOrderByMonthNumberAsc();
        return monthMapper.toResponseList(months);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MonthResponse> getMonthsBySeason(Season season) {
        List<Month> months = monthRepository.findBySeason(season);
        return monthMapper.toResponseList(months);
    }
    
    @Override
    public void deleteMonth(Long id) {
        log.info("حذف الشهر - ID: {}", id);
        
        Month month = findMonthById(id);
        
        // حذف جميع علاقات الشهر بالنباتات أولاً
        monthPlantRepository.deleteAll(month.getMonthPlants());
        
        monthRepository.delete(month);
        log.info("تم حذف الشهر بنجاح - ID: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByMonthNumber(Integer monthNumber) {
        return monthRepository.findByMonthNumber(monthNumber).isPresent();
    }
    
    // ===================== MonthPlant Operations =====================
    
    @Override
    public MonthPlantResponse addPlantToMonth(Integer monthNumber, MonthPlantRequest request) {
        log.info("إضافة نبتة {} للشهر {}", request.getPlantId(), monthNumber);
        
        Month month = monthRepository.findByMonthNumber(monthNumber)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على الشهر رقم: " + monthNumber));
        
        Plant plant = plantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على النبتة - ID: " + request.getPlantId()));
        
        // التحقق من عدم وجود العلاقة مسبقاً
        if (monthPlantRepository.existsByMonthIdAndPlantId(month.getId(), plant.getId())) {
            throw new IllegalArgumentException("النبتة مضافة مسبقاً لهذا الشهر");
        }
        
        MonthPlant monthPlant = monthMapper.toEntity(request, month, plant);
        MonthPlant saved = monthPlantRepository.save(monthPlant);
        
        log.info("تم إضافة النبتة للشهر بنجاح - ID: {}", saved.getId());
        return monthMapper.toMonthPlantResponse(saved);
    }
    
    @Override
    public MonthPlantResponse updateMonthPlant(Long id, MonthPlantRequest request) {
        log.info("تحديث علاقة شهر-نبتة - ID: {}", id);
        
        MonthPlant monthPlant = findMonthPlantById(id);
        
        // تحديث الملاحظات فقط
        monthPlant.setPlantingNoteAr(request.getPlantingNoteAr());
        monthPlant.setPlantingNoteEn(request.getPlantingNoteEn());
        
        MonthPlant updated = monthPlantRepository.save(monthPlant);
        
        log.info("تم تحديث علاقة شهر-نبتة بنجاح - ID: {}", updated.getId());
        return monthMapper.toMonthPlantResponse(updated);
    }
    
    @Override
    @Transactional(readOnly = true)
    public MonthPlantResponse getMonthPlantById(Long id) {
        return monthMapper.toMonthPlantResponse(findMonthPlantById(id));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MonthPlantResponse> getPlantsByMonth(Integer monthNumber) {
        Month month = monthRepository.findByMonthNumber(monthNumber)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على الشهر رقم: " + monthNumber));
        
        List<MonthPlant> monthPlants = monthPlantRepository.findByMonthIdWithPlantDetails(month.getId());
        return monthMapper.toMonthPlantResponseList(monthPlants);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MonthPlantResponse> getMonthsByPlant(Long plantId) {
        // التحقق من وجود النبتة
        if (!plantRepository.existsById(plantId)) {
            throw new EntityNotFoundException("لم يتم العثور على النبتة - ID: " + plantId);
        }
        
        List<MonthPlant> monthPlants = monthPlantRepository.findByPlantId(plantId);
        return monthMapper.toMonthPlantResponseList(monthPlants);
    }
    
    @Override
    public void removeMonthPlant(Long id) {
        log.info("حذف علاقة شهر-نبتة - ID: {}", id);
        
        MonthPlant monthPlant = findMonthPlantById(id);
        monthPlantRepository.delete(monthPlant);
        
        log.info("تم حذف علاقة شهر-نبتة بنجاح - ID: {}", id);
    }
    
    @Override
    public void removePlantFromMonth(Integer monthNumber, Long plantId) {
        log.info("حذف النبتة {} من الشهر {}", plantId, monthNumber);
        
        Month month = monthRepository.findByMonthNumber(monthNumber)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على الشهر رقم: " + monthNumber));
        
        MonthPlant monthPlant = monthPlantRepository.findByMonthId(month.getId()).stream()
                .filter(mp -> mp.getPlant().getId().equals(plantId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("النبتة غير موجودة في هذا الشهر"));
        
        monthPlantRepository.delete(monthPlant);
        log.info("تم حذف النبتة من الشهر بنجاح");
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsMonthPlant(Long monthId, Long plantId) {
        return monthPlantRepository.existsByMonthIdAndPlantId(monthId, plantId);
    }
    
    // ===================== Bulk Operations =====================
    
    @Override
    public List<MonthResponse> initializeDefaultMonths() {
        log.info("تهيئة الشهور الـ 12 بالبيانات الافتراضية");
        
        List<Month> months = new ArrayList<>();
        
        // تعريف الشهور مع الفصول والأوصاف
        String[][] monthsData = {
                {"1", "كانون الثاني", "January", "WINTER", "طقس بارد مع احتمال هطول الأمطار والثلوج في المرتفعات", "Cold weather with possible rain and snow in highlands"},
                {"2", "شباط", "February", "WINTER", "استمرار البرد مع أمطار متوقعة", "Continued cold with expected rainfall"},
                {"3", "آذار", "March", "SPRING", "بداية الربيع، الطقس معتدل ومناسب للزراعة", "Beginning of spring, moderate weather suitable for planting"},
                {"4", "نيسان", "April", "SPRING", "طقس ربيعي لطيف، موسم الزراعة الرئيسي", "Pleasant spring weather, main planting season"},
                {"5", "أيار", "May", "SPRING", "نهاية الربيع، ارتفاع تدريجي في درجات الحرارة", "End of spring, gradual temperature increase"},
                {"6", "حزيران", "June", "SUMMER", "بداية الصيف، طقس حار وجاف", "Beginning of summer, hot and dry weather"},
                {"7", "تموز", "July", "SUMMER", "ذروة الصيف، طقس حار جداً", "Peak summer, very hot weather"},
                {"8", "آب", "August", "SUMMER", "استمرار الحرارة العالية", "Continued high temperatures"},
                {"9", "أيلول", "September", "AUTUMN", "بداية الخريف، انخفاض تدريجي في الحرارة", "Beginning of autumn, gradual temperature decrease"},
                {"10", "تشرين الأول", "October", "AUTUMN", "طقس خريفي معتدل، موسم زراعة ثانوي", "Moderate autumn weather, secondary planting season"},
                {"11", "تشرين الثاني", "November", "AUTUMN", "نهاية الخريف، بداية موسم الأمطار", "End of autumn, beginning of rainy season"},
                {"12", "كانون الأول", "December", "WINTER", "بداية الشتاء، طقس بارد مع أمطار", "Beginning of winter, cold weather with rain"}
        };
        
        for (String[] data : monthsData) {
            int monthNumber = Integer.parseInt(data[0]);
            
            // تخطي إذا كان الشهر موجوداً مسبقاً
            if (monthRepository.findByMonthNumber(monthNumber).isPresent()) {
                log.info("الشهر {} موجود مسبقاً، تخطي...", monthNumber);
                continue;
            }
            
            Month month = Month.builder()
                    .monthNumber(monthNumber)
                    .nameAr(data[1])
                    .nameEn(data[2])
                    .season(Season.valueOf(data[3]))
                    .weatherDescriptionAr(data[4])
                    .weatherDescriptionEn(data[5])
                    .build();
            
            months.add(monthRepository.save(month));
        }
        
        log.info("تم تهيئة {} شهر بنجاح", months.size());
        return monthMapper.toResponseList(months);
    }
    
    @Override
    public List<MonthPlantResponse> addPlantToMultipleMonths(Long plantId, List<Integer> monthNumbers, 
                                                              String plantingNoteAr, String plantingNoteEn) {
        log.info("إضافة النبتة {} لأشهر: {}", plantId, monthNumbers);
        
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على النبتة - ID: " + plantId));
        
        List<MonthPlant> results = new ArrayList<>();
        
        for (Integer monthNumber : monthNumbers) {
            Month month = monthRepository.findByMonthNumber(monthNumber)
                    .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على الشهر رقم: " + monthNumber));
            
            // تخطي إذا كانت العلاقة موجودة مسبقاً
            if (monthPlantRepository.existsByMonthIdAndPlantId(month.getId(), plant.getId())) {
                log.info("النبتة مضافة مسبقاً للشهر {}، تخطي...", monthNumber);
                continue;
            }
            
            MonthPlant monthPlant = MonthPlant.builder()
                    .month(month)
                    .plant(plant)
                    .plantingNoteAr(plantingNoteAr)
                    .plantingNoteEn(plantingNoteEn)
                    .build();
            
            results.add(monthPlantRepository.save(monthPlant));
        }
        
        log.info("تم إضافة النبتة لـ {} شهر بنجاح", results.size());
        return monthMapper.toMonthPlantResponseList(results);
    }
    
    // ===================== Private Helper Methods =====================
    
    private Month findMonthById(Long id) {
        return monthRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على الشهر - ID: " + id));
    }
    
    private MonthPlant findMonthPlantById(Long id) {
        return monthPlantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على علاقة الشهر-النبتة - ID: " + id));
    }
}
