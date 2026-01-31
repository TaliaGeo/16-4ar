package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.MonthPlantRequest;
import group.g.graduation.backend.admin.dto.MonthPlantResponse;
import group.g.graduation.backend.admin.dto.MonthRequest;
import group.g.graduation.backend.admin.dto.MonthResponse;
import group.g.graduation.backend.common.enums.Season;

import java.util.List;

/**
 * Admin service for Month and MonthPlant management
 * خدمة إدارة الشهور وعلاقاتها بالنباتات للأدمن
 */
public interface AdminMonthService {
    
    // ===================== Month Operations =====================
    
    /**
     * إنشاء شهر جديد
     */
    MonthResponse createMonth(MonthRequest request);
    
    /**
     * تحديث شهر
     */
    MonthResponse updateMonth(Long id, MonthRequest request);
    
    /**
     * الحصول على شهر بالمعرّف
     */
    MonthResponse getMonthById(Long id);
    
    /**
     * الحصول على شهر برقمه
     */
    MonthResponse getMonthByNumber(Integer monthNumber);
    
    /**
     * الحصول على شهر مع قائمة النباتات
     */
    MonthResponse getMonthWithPlants(Integer monthNumber);
    
    /**
     * الحصول على جميع الشهور
     */
    List<MonthResponse> getAllMonths();
    
    /**
     * الحصول على شهور فصل معين
     */
    List<MonthResponse> getMonthsBySeason(Season season);
    
    /**
     * حذف شهر
     */
    void deleteMonth(Long id);
    
    /**
     * التحقق من وجود شهر
     */
    boolean existsByMonthNumber(Integer monthNumber);
    
    // ===================== MonthPlant Operations =====================
    
    /**
     * إضافة نبتة لشهر زراعة
     */
    MonthPlantResponse addPlantToMonth(Integer monthNumber, MonthPlantRequest request);
    
    /**
     * تحديث ملاحظات زراعة نبتة في شهر
     */
    MonthPlantResponse updateMonthPlant(Long id, MonthPlantRequest request);
    
    /**
     * الحصول على علاقة شهر-نبتة بالمعرّف
     */
    MonthPlantResponse getMonthPlantById(Long id);
    
    /**
     * الحصول على جميع النباتات لشهر معين
     */
    List<MonthPlantResponse> getPlantsByMonth(Integer monthNumber);
    
    /**
     * الحصول على أشهر زراعة نبتة معينة
     */
    List<MonthPlantResponse> getMonthsByPlant(Long plantId);
    
    /**
     * حذف علاقة شهر-نبتة
     */
    void removeMonthPlant(Long id);
    
    /**
     * حذف نبتة من شهر
     */
    void removePlantFromMonth(Integer monthNumber, Long plantId);
    
    /**
     * التحقق من وجود علاقة شهر-نبتة
     */
    boolean existsMonthPlant(Long monthId, Long plantId);
    
    // ===================== Bulk Operations =====================
    
    /**
     * تهيئة الشهور الـ 12 بالبيانات الافتراضية
     */
    List<MonthResponse> initializeDefaultMonths();
    
    /**
     * إضافة نبتة لعدة أشهر دفعة واحدة
     */
    List<MonthPlantResponse> addPlantToMultipleMonths(Long plantId, List<Integer> monthNumbers, String plantingNoteAr, String plantingNoteEn);
}
