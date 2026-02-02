package group.g.graduation.backend.admin.controller;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.admin.service.AdminMonthPlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Controller for MonthPlant Management
 * تحكم إدارة علاقة الأشهر بالنباتات للأدمن
 */
@RestController
@RequestMapping("/api/admin/month-plants")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin MonthPlant Management", description = "إدارة علاقة الأشهر بالنباتات")
@SecurityRequirement(name = "bearerAuth")
public class AdminMonthPlantController {
    
    private final AdminMonthPlantService monthPlantService;
    
    // ===================== Create Operations =====================
    
    @PostMapping("/month/{monthId}")
    @Operation(summary = "إضافة نبتة لشهر", description = "ربط نبتة بشهر زراعة معين")
    public ResponseEntity<MonthPlantResponse> addPlantToMonth(
            @PathVariable Long monthId,
            @Valid @RequestBody MonthPlantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(monthPlantService.addPlantToMonth(monthId, request));
    }
    
    @PostMapping("/month/{monthId}/bulk")
    @Operation(summary = "إضافة عدة نباتات لشهر", description = "ربط عدة نباتات بشهر زراعة واحد")
    public ResponseEntity<List<MonthPlantResponse>> addPlantsToMonth(
            @PathVariable Long monthId,
            @Valid @RequestBody BulkPlantsToMonthRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(monthPlantService.addPlantsToMonth(monthId, request));
    }
    
    @PostMapping("/plant/{plantId}/months")
    @Operation(summary = "إضافة نبتة لعدة أشهر", description = "ربط نبتة واحدة بعدة أشهر زراعة")
    public ResponseEntity<List<MonthPlantResponse>> addPlantToMonths(
            @PathVariable Long plantId,
            @Valid @RequestBody BulkMonthsToPlantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(monthPlantService.addPlantToMonths(plantId, request));
    }
    
    // ===================== Read Operations =====================
    
    @GetMapping
    @Operation(summary = "جلب كل العلاقات", description = "جلب كل علاقات الأشهر بالنباتات")
    public ResponseEntity<List<MonthPlantResponse>> getAllMonthPlants() {
        return ResponseEntity.ok(monthPlantService.getAllMonthPlants());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "جلب علاقة", description = "جلب علاقة شهر-نبتة بمعرفها")
    public ResponseEntity<?> getMonthPlantById(@PathVariable Long id) {
        try {
            log.info("Getting month-plant with id: {}", id);
            MonthPlantResponse response = monthPlantService.getMonthPlantById(id);
            log.info("Successfully retrieved month-plant with id: {}", id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.warn("Month-plant not found with id: {}: {}", id, e.getMessage());
            Map<String, Object> error = Map.of(
                "status", 404,
                "error", "Not Found",
                "message", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error getting month-plant with id: {}", id, e);
            Map<String, Object> error = Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "Error retrieving month plant: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now(),
                "details", e.getClass().getSimpleName()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/month/{monthId}")
    @Operation(summary = "نباتات شهر معين", description = "جلب النباتات المناسبة للزراعة في شهر معين")
    public ResponseEntity<List<MonthPlantResponse>> getPlantsByMonth(@PathVariable Long monthId) {
        return ResponseEntity.ok(monthPlantService.getPlantsByMonth(monthId));
    }
    
    @GetMapping("/plant/{plantId}")
    @Operation(summary = "أشهر نبتة معينة", description = "جلب الأشهر المناسبة لزراعة نبتة معينة")
    public ResponseEntity<List<MonthPlantResponse>> getMonthsByPlant(@PathVariable Long plantId) {
        return ResponseEntity.ok(monthPlantService.getMonthsByPlant(plantId));
    }
    
    @GetMapping("/current-month")
    @Operation(summary = "نباتات الشهر الحالي", description = "جلب النباتات المناسبة للزراعة في الشهر الحالي")
    public ResponseEntity<List<MonthPlantResponse>> getPlantsForCurrentMonth() {
        return ResponseEntity.ok(monthPlantService.getPlantsForCurrentMonth());
    }
    
    @GetMapping("/calendar")
    @Operation(summary = "تقويم الزراعة", description = "جلب تقويم كامل للزراعة (كل الأشهر مع نباتاتها)")
    public ResponseEntity<List<Map<String, Object>>> getPlantingCalendar() {
        return ResponseEntity.ok(monthPlantService.getPlantingCalendar());
    }
    
    @GetMapping("/stats")
    @Operation(summary = "إحصائيات", description = "جلب إحصائيات علاقات الأشهر بالنباتات")
    public ResponseEntity<Map<String, Object>> getMonthPlantStats() {
        return ResponseEntity.ok(monthPlantService.getMonthPlantStats());
    }
    
    // ===================== Update Operations =====================
    
    @PutMapping("/{id}")
    @Operation(summary = "تعديل علاقة", description = "تعديل ملاحظات الزراعة لعلاقة موجودة")
    public ResponseEntity<?> updateMonthPlant(
            @PathVariable Long id,
            @Valid @RequestBody MonthPlantUpdateRequest request) {
        try {
            log.info("Updating month-plant with id: {} with data: {}", id, request);
            MonthPlantResponse response = monthPlantService.updateMonthPlant(id, request);
            log.info("Successfully updated month-plant with id: {}", id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.warn("Month-plant not found for update with id: {}: {}", id, e.getMessage());
            Map<String, Object> error = Map.of(
                "status", 404,
                "error", "Not Found", 
                "message", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error updating month-plant with id: {}", id, e);
            Map<String, Object> error = Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "Error updating month plant: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now(),
                "details", e.getClass().getSimpleName()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    // ===================== Delete Operations =====================
    
    @DeleteMapping("/{id}")
    @Operation(summary = "حذف علاقة", description = "حذف علاقة شهر-نبتة بمعرفها")
    public ResponseEntity<Void> deleteMonthPlant(@PathVariable Long id) {
        monthPlantService.deleteMonthPlant(id);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/month/{monthId}/plant/{plantId}")
    @Operation(summary = "إزالة نبتة من شهر", description = "إزالة ربط نبتة من شهر معين")
    public ResponseEntity<Void> removePlantFromMonth(
            @PathVariable Long monthId,
            @PathVariable Long plantId) {
        monthPlantService.removePlantFromMonth(monthId, plantId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/month/{monthId}/all")
    @Operation(summary = "إزالة كل نباتات شهر", description = "إزالة كل النباتات المرتبطة بشهر معين")
    public ResponseEntity<Void> removeAllPlantsFromMonth(@PathVariable Long monthId) {
        monthPlantService.removeAllPlantsFromMonth(monthId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/plant/{plantId}/all")
    @Operation(summary = "إزالة نبتة من كل الأشهر", description = "إزالة نبتة من كل الأشهر المرتبطة بها")
    public ResponseEntity<Void> removePlantFromAllMonths(@PathVariable Long plantId) {
        monthPlantService.removePlantFromAllMonths(plantId);
        return ResponseEntity.noContent().build();
    }
}
