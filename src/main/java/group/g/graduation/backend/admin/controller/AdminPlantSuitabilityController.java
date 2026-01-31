package group.g.graduation.backend.admin.controller;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.admin.service.AdminPlantSuitabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Controller for PlantSuitability & PlantRecommendation
 * واجهة برمجة التطبيقات لإدارة ملاءمة النباتات والتوصيات
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/plant-suitability")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Plant Suitability", description = "APIs for managing plant suitability and recommendations")
public class AdminPlantSuitabilityController {
    
    private final AdminPlantSuitabilityService suitabilityService;
    
    // ===================== CRUD Operations =====================
    
    @PostMapping
    @Operation(summary = "Create plant suitability", description = "إنشاء سجل ملاءمة جديد لنبات وخيار")
    public ResponseEntity<PlantSuitabilityResponse> createSuitability(
            @Valid @RequestBody PlantSuitabilityRequest request) {
        log.info("Creating plant suitability: plantId={}, optionId={}", request.getPlantId(), request.getOptionId());
        return new ResponseEntity<>(suitabilityService.createSuitability(request), HttpStatus.CREATED);
    }
    
    @PostMapping("/bulk")
    @Operation(summary = "Create bulk suitabilities", description = "إنشاء عدة سجلات ملاءمة دفعة واحدة")
    public ResponseEntity<List<PlantSuitabilityResponse>> createBulkSuitabilities(
            @Valid @RequestBody BulkPlantSuitabilityRequest request) {
        log.info("Creating {} bulk suitabilities", request.getSuitabilities().size());
        return new ResponseEntity<>(suitabilityService.createBulkSuitabilities(request), HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get suitability by ID", description = "جلب سجل ملاءمة بالمعرّف")
    public ResponseEntity<PlantSuitabilityResponse> getSuitabilityById(
            @Parameter(description = "Suitability ID") @PathVariable Long id) {
        return ResponseEntity.ok(suitabilityService.getSuitabilityById(id));
    }
    
    @GetMapping
    @Operation(summary = "Get all suitabilities", description = "جلب جميع سجلات الملاءمة")
    public ResponseEntity<List<PlantSuitabilityResponse>> getAllSuitabilities() {
        return ResponseEntity.ok(suitabilityService.getAllSuitabilities());
    }
    
    @GetMapping("/plant/{plantId}")
    @Operation(summary = "Get suitabilities by plant", description = "جلب جميع سجلات ملاءمة نبات معين")
    public ResponseEntity<List<PlantSuitabilityResponse>> getSuitabilitiesByPlant(
            @Parameter(description = "Plant ID") @PathVariable Long plantId) {
        return ResponseEntity.ok(suitabilityService.getSuitabilitiesByPlantId(plantId));
    }
    
    @GetMapping("/option/{optionId}")
    @Operation(summary = "Get suitabilities by option", description = "جلب جميع سجلات ملاءمة خيار معين")
    public ResponseEntity<List<PlantSuitabilityResponse>> getSuitabilitiesByOption(
            @Parameter(description = "Option ID") @PathVariable Long optionId) {
        return ResponseEntity.ok(suitabilityService.getSuitabilitiesByOptionId(optionId));
    }
    
    @GetMapping("/plant/{plantId}/option/{optionId}")
    @Operation(summary = "Get suitability by plant and option", description = "جلب سجل ملاءمة لنبات وخيار معينين")
    public ResponseEntity<PlantSuitabilityResponse> getSuitabilityByPlantAndOption(
            @Parameter(description = "Plant ID") @PathVariable Long plantId,
            @Parameter(description = "Option ID") @PathVariable Long optionId) {
        return ResponseEntity.ok(suitabilityService.getSuitabilityByPlantAndOption(plantId, optionId));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update suitability", description = "تحديث سجل ملاءمة")
    public ResponseEntity<PlantSuitabilityResponse> updateSuitability(
            @Parameter(description = "Suitability ID") @PathVariable Long id,
            @Valid @RequestBody PlantSuitabilityRequest request) {
        log.info("Updating suitability with ID: {}", id);
        return ResponseEntity.ok(suitabilityService.updateSuitability(id, request));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete suitability", description = "حذف سجل ملاءمة")
    public ResponseEntity<Map<String, String>> deleteSuitability(
            @Parameter(description = "Suitability ID") @PathVariable Long id) {
        suitabilityService.deleteSuitability(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Suitability deleted successfully");
        response.put("messageAr", "تم حذف سجل الملاءمة بنجاح");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/plant/{plantId}")
    @Operation(summary = "Delete all suitabilities for a plant", description = "حذف جميع سجلات ملاءمة نبات")
    public ResponseEntity<Map<String, String>> deleteSuitabilitiesByPlant(
            @Parameter(description = "Plant ID") @PathVariable Long plantId) {
        suitabilityService.deleteSuitabilitiesByPlantId(plantId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "All suitabilities for plant deleted successfully");
        response.put("messageAr", "تم حذف جميع سجلات ملاءمة النبات بنجاح");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/plant/{plantId}/option/{optionId}")
    @Operation(summary = "Delete suitability by plant and option", description = "حذف سجل ملاءمة لنبات وخيار")
    public ResponseEntity<Map<String, String>> deleteSuitabilityByPlantAndOption(
            @Parameter(description = "Plant ID") @PathVariable Long plantId,
            @Parameter(description = "Option ID") @PathVariable Long optionId) {
        suitabilityService.deleteSuitabilityByPlantAndOption(plantId, optionId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Suitability deleted successfully");
        response.put("messageAr", "تم حذف سجل الملاءمة بنجاح");
        return ResponseEntity.ok(response);
    }
    
    // ===================== Statistics & Analytics =====================
    
    @GetMapping("/stats/plant/{plantId}")
    @Operation(summary = "Get plant suitability statistics", description = "إحصائيات ملاءمة نبات")
    public ResponseEntity<Map<String, Object>> getPlantSuitabilityStats(
            @Parameter(description = "Plant ID") @PathVariable Long plantId) {
        return ResponseEntity.ok(suitabilityService.getPlantSuitabilityStats(plantId));
    }
    
    @GetMapping("/stats/plants-average")
    @Operation(summary = "Get all plants with average scores", description = "جميع النباتات مع متوسط الدرجات")
    public ResponseEntity<List<Map<String, Object>>> getPlantsWithAverageScores() {
        return ResponseEntity.ok(suitabilityService.getPlantsWithAverageScores());
    }
    
    @GetMapping("/stats/plant/{plantId}/by-level")
    @Operation(summary = "Count suitabilities by score level", description = "عدد الملاءمات حسب مستوى الدرجة")
    public ResponseEntity<Map<String, Long>> countSuitabilitiesByScoreLevel(
            @Parameter(description = "Plant ID") @PathVariable Long plantId) {
        return ResponseEntity.ok(suitabilityService.countSuitabilitiesByScoreLevel(plantId));
    }
    
    // ===================== Plant Recommendations =====================
    
    @PostMapping("/recommendations")
    @Operation(summary = "Get plant recommendations", 
               description = "الحصول على توصيات النباتات بناءً على إجابات المستخدم")
    public ResponseEntity<List<PlantRecommendationResponse>> getRecommendations(
            @Valid @RequestBody PlantRecommendationRequest request) {
        log.info("Getting recommendations for {} answers", request.getAnswers().size());
        return ResponseEntity.ok(suitabilityService.getRecommendations(request));
    }
    
    @PostMapping("/recommendations/top/{limit}")
    @Operation(summary = "Get top N recommendations", 
               description = "الحصول على أفضل N توصيات")
    public ResponseEntity<List<PlantRecommendationResponse>> getTopRecommendations(
            @Parameter(description = "Number of recommendations") @PathVariable int limit,
            @Valid @RequestBody PlantRecommendationRequest request) {
        log.info("Getting top {} recommendations", limit);
        return ResponseEntity.ok(suitabilityService.getTopRecommendations(request, limit));
    }
    
    @PostMapping("/recommendations/plant/{plantId}")
    @Operation(summary = "Calculate match for specific plant", 
               description = "حساب نسبة التطابق لنبات معين")
    public ResponseEntity<PlantRecommendationResponse> calculatePlantMatch(
            @Parameter(description = "Plant ID") @PathVariable Long plantId,
            @RequestBody Map<String, Object> answers) {
        log.info("Calculating match for plantId: {}", plantId);
        return ResponseEntity.ok(suitabilityService.calculatePlantMatch(plantId, answers));
    }
    
    // ===================== Setup & Initialization =====================
    
    @PostMapping("/initialize/plant/{plantId}")
    @Operation(summary = "Initialize suitabilities for a plant", 
               description = "تهيئة سجلات الملاءمة لنبات (بدرجة افتراضية 50)")
    public ResponseEntity<List<PlantSuitabilityResponse>> initializePlantSuitabilities(
            @Parameter(description = "Plant ID") @PathVariable Long plantId) {
        log.info("Initializing suitabilities for plantId: {}", plantId);
        return new ResponseEntity<>(suitabilityService.initializePlantSuitabilities(plantId), HttpStatus.CREATED);
    }
    
    @PostMapping("/copy/{sourcePlantId}/to/{targetPlantId}")
    @Operation(summary = "Copy suitabilities from one plant to another", 
               description = "نسخ سجلات الملاءمة من نبات إلى آخر")
    public ResponseEntity<List<PlantSuitabilityResponse>> copySuitabilities(
            @Parameter(description = "Source Plant ID") @PathVariable Long sourcePlantId,
            @Parameter(description = "Target Plant ID") @PathVariable Long targetPlantId) {
        log.info("Copying suitabilities from plantId {} to plantId {}", sourcePlantId, targetPlantId);
        return new ResponseEntity<>(
                suitabilityService.copySuitabilitiesFromPlant(sourcePlantId, targetPlantId), 
                HttpStatus.CREATED);
    }
    
    @PutMapping("/plant/{plantId}/batch")
    @Operation(summary = "Update multiple suitabilities for a plant", 
               description = "تحديث عدة سجلات ملاءمة لنبات دفعة واحدة")
    public ResponseEntity<List<PlantSuitabilityResponse>> updatePlantSuitabilities(
            @Parameter(description = "Plant ID") @PathVariable Long plantId,
            @Valid @RequestBody List<PlantSuitabilityRequest> requests) {
        log.info("Updating {} suitabilities for plantId: {}", requests.size(), plantId);
        return ResponseEntity.ok(suitabilityService.updatePlantSuitabilities(plantId, requests));
    }
}
