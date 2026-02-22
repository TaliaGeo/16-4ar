package group.g.graduation.backend.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import group.g.graduation.backend.admin.dto.MonthPlantRequest;
import group.g.graduation.backend.admin.dto.MonthPlantResponse;
import group.g.graduation.backend.admin.dto.MonthRequest;
import group.g.graduation.backend.admin.dto.MonthResponse;
import group.g.graduation.backend.admin.service.AdminMonthService;
import group.g.graduation.backend.common.enums.Season;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin controller for Month and MonthPlant management
 * تحكم إدارة الشهور وعلاقاتها بالنباتات
 */
@RestController
@RequestMapping("/api/admin/months")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Months & Planting Calendar", description = "إدارة الشهور وتقويم الزراعة")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMonthController {
    
    private final AdminMonthService monthService;
    
    // ===================== Month Endpoints =====================
    
    @PostMapping
    @Operation(summary = "إنشاء شهر جديد", description = "إضافة شهر جديد للتقويم")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "تم إنشاء الشهر بنجاح"),
            @ApiResponse(responseCode = "400", description = "بيانات غير صالحة"),
            @ApiResponse(responseCode = "409", description = "الشهر موجود مسبقاً")
    })
    public ResponseEntity<MonthResponse> createMonth(
            @Valid @RequestBody MonthRequest request) {
        log.info("REST: إنشاء شهر جديد - رقم: {}", request.getMonthNumber());
        MonthResponse response = monthService.createMonth(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "تحديث شهر", description = "تعديل بيانات شهر موجود")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم تحديث الشهر بنجاح"),
            @ApiResponse(responseCode = "404", description = "الشهر غير موجود"),
            @ApiResponse(responseCode = "400", description = "بيانات غير صالحة")
    })
    public ResponseEntity<MonthResponse> updateMonth(
            @Parameter(description = "معرّف الشهر") @PathVariable Long id,
            @Valid @RequestBody MonthRequest request) {
        log.info("REST: تحديث الشهر - ID: {}", id);
        MonthResponse response = monthService.updateMonth(id, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "الحصول على شهر بالمعرّف", description = "عرض تفاصيل شهر محدد")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم العثور على الشهر"),
            @ApiResponse(responseCode = "404", description = "الشهر غير موجود")
    })
    public ResponseEntity<MonthResponse> getMonthById(
            @Parameter(description = "معرّف الشهر") @PathVariable Long id) {
        log.info("REST: الحصول على الشهر - ID: {}", id);
        MonthResponse response = monthService.getMonthById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{monthNumber}")
    @Operation(summary = "الحصول على شهر برقمه", description = "عرض تفاصيل شهر برقمه (1-12)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم العثور على الشهر"),
            @ApiResponse(responseCode = "404", description = "الشهر غير موجود")
    })
    public ResponseEntity<MonthResponse> getMonthByNumber(
            @Parameter(description = "رقم الشهر (1-12)") @PathVariable Integer monthNumber) {
        log.info("REST: الحصول على الشهر رقم: {}", monthNumber);
        MonthResponse response = monthService.getMonthByNumber(monthNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{monthNumber}/with-plants")
    @Operation(summary = "الحصول على شهر مع قائمة النباتات", description = "عرض تفاصيل شهر مع جميع النباتات المناسبة للزراعة فيه")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم العثور على الشهر"),
            @ApiResponse(responseCode = "404", description = "الشهر غير موجود")
    })
    public ResponseEntity<MonthResponse> getMonthWithPlants(
            @Parameter(description = "رقم الشهر (1-12)") @PathVariable Integer monthNumber) {
        log.info("REST: الحصول على الشهر مع النباتات - رقم: {}", monthNumber);
        MonthResponse response = monthService.getMonthWithPlants(monthNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "الحصول على جميع الشهور", description = "عرض قائمة جميع الشهور مرتبة")
    @ApiResponse(responseCode = "200", description = "تم جلب الشهور بنجاح")
    public ResponseEntity<List<MonthResponse>> getAllMonths() {
        log.info("REST: الحصول على جميع الشهور");
        List<MonthResponse> months = monthService.getAllMonths();
        return ResponseEntity.ok(months);
    }
    
    @GetMapping("/season/{season}")
    @Operation(summary = "الحصول على شهور فصل معين", description = "عرض شهور فصل محدد")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب الشهور بنجاح"),
            @ApiResponse(responseCode = "400", description = "فصل غير صالح")
    })
    public ResponseEntity<List<MonthResponse>> getMonthsBySeason(
            @Parameter(description = "الفصل (WINTER, SPRING, SUMMER, AUTUMN)") @PathVariable Season season) {
        log.info("REST: الحصول على شهور فصل: {}", season);
        List<MonthResponse> months = monthService.getMonthsBySeason(season);
        return ResponseEntity.ok(months);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "حذف شهر", description = "حذف شهر من التقويم")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "تم حذف الشهر بنجاح"),
            @ApiResponse(responseCode = "404", description = "الشهر غير موجود")
    })
    public ResponseEntity<Void> deleteMonth(
            @Parameter(description = "معرّف الشهر") @PathVariable Long id) {
        log.info("REST: حذف الشهر - ID: {}", id);
        monthService.deleteMonth(id);
        return ResponseEntity.noContent().build();
    }
    
    // ===================== MonthPlant Endpoints =====================
    
    @PostMapping("/number/{monthNumber}/plants")
    @Operation(summary = "إضافة نبتة لشهر", description = "ربط نبتة بشهر زراعة مناسب")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "تم إضافة النبتة للشهر بنجاح"),
            @ApiResponse(responseCode = "404", description = "الشهر أو النبتة غير موجودة"),
            @ApiResponse(responseCode = "409", description = "النبتة مضافة مسبقاً لهذا الشهر")
    })
    public ResponseEntity<MonthPlantResponse> addPlantToMonth(
            @Parameter(description = "رقم الشهر (1-12)") @PathVariable Integer monthNumber,
            @Valid @RequestBody MonthPlantRequest request) {
        log.info("REST: إضافة نبتة {} للشهر {}", request.getPlantId(), monthNumber);
        MonthPlantResponse response = monthService.addPlantToMonth(monthNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/month-plants/{id}")
    @Operation(summary = "تحديث ملاحظات زراعة", description = "تعديل ملاحظات زراعة نبتة في شهر معين")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم التحديث بنجاح"),
            @ApiResponse(responseCode = "404", description = "العلاقة غير موجودة")
    })
    public ResponseEntity<MonthPlantResponse> updateMonthPlant(
            @Parameter(description = "معرّف علاقة الشهر-النبتة") @PathVariable Long id,
            @Valid @RequestBody MonthPlantRequest request) {
        log.info("REST: تحديث علاقة شهر-نبتة - ID: {}", id);
        MonthPlantResponse response = monthService.updateMonthPlant(id, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/month-plants/{id}")
    @Operation(summary = "الحصول على علاقة شهر-نبتة", description = "عرض تفاصيل علاقة شهر بنبتة")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم العثور على العلاقة"),
            @ApiResponse(responseCode = "404", description = "العلاقة غير موجودة")
    })
    public ResponseEntity<MonthPlantResponse> getMonthPlantById(
            @Parameter(description = "معرّف العلاقة") @PathVariable Long id) {
        log.info("REST: الحصول على علاقة شهر-نبتة - ID: {}", id);
        MonthPlantResponse response = monthService.getMonthPlantById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{monthNumber}/plants")
    @Operation(summary = "الحصول على نباتات شهر", description = "عرض جميع النباتات المناسبة للزراعة في شهر معين")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب النباتات بنجاح"),
            @ApiResponse(responseCode = "404", description = "الشهر غير موجود")
    })
    public ResponseEntity<List<MonthPlantResponse>> getPlantsByMonth(
            @Parameter(description = "رقم الشهر (1-12)") @PathVariable Integer monthNumber) {
        log.info("REST: الحصول على نباتات الشهر: {}", monthNumber);
        List<MonthPlantResponse> plants = monthService.getPlantsByMonth(monthNumber);
        return ResponseEntity.ok(plants);
    }
    
    @GetMapping("/plants/{plantId}/months")
    @Operation(summary = "الحصول على أشهر زراعة نبتة", description = "عرض جميع الأشهر المناسبة لزراعة نبتة معينة")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم جلب الأشهر بنجاح"),
            @ApiResponse(responseCode = "404", description = "النبتة غير موجودة")
    })
    public ResponseEntity<List<MonthPlantResponse>> getMonthsByPlant(
            @Parameter(description = "معرّف النبتة") @PathVariable Long plantId) {
        log.info("REST: الحصول على أشهر زراعة النبتة - ID: {}", plantId);
        List<MonthPlantResponse> months = monthService.getMonthsByPlant(plantId);
        return ResponseEntity.ok(months);
    }
    
    @DeleteMapping("/month-plants/{id}")
    @Operation(summary = "حذف علاقة شهر-نبتة", description = "إزالة ربط نبتة بشهر")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "تم الحذف بنجاح"),
            @ApiResponse(responseCode = "404", description = "العلاقة غير موجودة")
    })
    public ResponseEntity<Void> removeMonthPlant(
            @Parameter(description = "معرّف العلاقة") @PathVariable Long id) {
        log.info("REST: حذف علاقة شهر-نبتة - ID: {}", id);
        monthService.removeMonthPlant(id);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/number/{monthNumber}/plants/{plantId}")
    @Operation(summary = "إزالة نبتة من شهر", description = "إلغاء ربط نبتة بشهر زراعة")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "تم الإزالة بنجاح"),
            @ApiResponse(responseCode = "404", description = "الشهر أو النبتة غير موجودة")
    })
    public ResponseEntity<Void> removePlantFromMonth(
            @Parameter(description = "رقم الشهر (1-12)") @PathVariable Integer monthNumber,
            @Parameter(description = "معرّف النبتة") @PathVariable Long plantId) {
        log.info("REST: إزالة النبتة {} من الشهر {}", plantId, monthNumber);
        monthService.removePlantFromMonth(monthNumber, plantId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/plants/{plantId}/bulk-months")
    @Operation(summary = "إضافة نبتة لعدة أشهر", description = "ربط نبتة بعدة أشهر زراعة دفعة واحدة")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "تم الإضافة بنجاح"),
            @ApiResponse(responseCode = "404", description = "النبتة أو أحد الأشهر غير موجود")
    })
    public ResponseEntity<List<MonthPlantResponse>> addPlantToMultipleMonths(
            @Parameter(description = "معرّف النبتة") @PathVariable Long plantId,
            @RequestBody Map<String, Object> request) {
        
        @SuppressWarnings("unchecked")
        List<Integer> monthNumbers = (List<Integer>) request.get("monthNumbers");
        String plantingNoteAr = (String) request.getOrDefault("plantingNoteAr", null);
        String plantingNoteEn = (String) request.getOrDefault("plantingNoteEn", null);
        
        log.info("REST: إضافة النبتة {} لأشهر: {}", plantId, monthNumbers);
        List<MonthPlantResponse> responses = monthService.addPlantToMultipleMonths(
                plantId, monthNumbers, plantingNoteAr, plantingNoteEn);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
}
