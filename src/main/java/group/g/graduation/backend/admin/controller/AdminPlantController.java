package group.g.graduation.backend.admin.controller;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.admin.service.AdminPlantService;
import group.g.graduation.backend.common.enums.PlantCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Plant Controller - واجهة API لإدارة النباتات
 */
@RestController
@RequestMapping("/api/admin/plants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Plants", description = "Plant management APIs for administrators")
public class AdminPlantController {
    
    private final AdminPlantService plantService;
    
    // ===== CRUD Operations =====
    
    @PostMapping
    @Operation(summary = "Create a new plant", description = "إنشاء نبتة جديدة")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Plant created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin only")
    })
    public ResponseEntity<PlantResponse> createPlant(
            @Valid @RequestBody PlantCreateRequest request) {
        PlantResponse response = plantService.createPlant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update a plant", description = "تعديل نبتة موجودة")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plant updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Plant not found")
    })
    public ResponseEntity<PlantResponse> updatePlant(
            @Parameter(description = "Plant ID") @PathVariable Long id,
            @Valid @RequestBody PlantUpdateRequest request) {
        PlantResponse response = plantService.updatePlant(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a plant", description = "حذف نبتة")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Plant deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Plant not found")
    })
    public ResponseEntity<Void> deletePlant(
            @Parameter(description = "Plant ID") @PathVariable Long id) {
        plantService.deletePlant(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get plant by ID", description = "جلب نبتة بالمعرف")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plant found"),
        @ApiResponse(responseCode = "404", description = "Plant not found")
    })
    public ResponseEntity<PlantResponse> getPlantById(
            @Parameter(description = "Plant ID") @PathVariable Long id) {
        PlantResponse response = plantService.getPlantById(id);
        return ResponseEntity.ok(response);
    }
    
    // ===== List & Search Operations =====
    
    @GetMapping
    @Operation(summary = "Get all plants", description = "جلب كل النباتات مع الترقيم")
    @ApiResponse(responseCode = "200", description = "Plants retrieved successfully")
    public ResponseEntity<PlantListResponse> getAllPlants(
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") 
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") 
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PlantListResponse response = plantService.getAllPlants(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search plants", description = "البحث عن نباتات بكلمة مفتاحية")
    @ApiResponse(responseCode = "200", description = "Search results")
    public ResponseEntity<PlantListResponse> searchPlants(
            @Parameter(description = "Search keyword") 
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        PlantListResponse response = plantService.searchPlants(q, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/category/{category}")
    @Operation(summary = "Get plants by category", description = "جلب النباتات حسب التصنيف")
    @ApiResponse(responseCode = "200", description = "Plants retrieved by category")
    public ResponseEntity<PlantListResponse> getPlantsByCategory(
            @Parameter(description = "Plant category") 
            @PathVariable PlantCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        PlantListResponse response = plantService.getPlantsByCategory(category, pageable);
        return ResponseEntity.ok(response);
    }
    
    // ===== Image Operations =====
    
    @PostMapping("/{plantId}/images")
    @Operation(summary = "Add image to plant", description = "إضافة صورة للنبتة")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Image added successfully"),
        @ApiResponse(responseCode = "404", description = "Plant not found")
    })
    public ResponseEntity<PlantImageResponse> addImage(
            @Parameter(description = "Plant ID") @PathVariable Long plantId,
            @Valid @RequestBody PlantImageRequest request) {
        PlantImageResponse response = plantService.addImage(plantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @DeleteMapping("/{plantId}/images/{imageId}")
    @Operation(summary = "Delete image from plant", description = "حذف صورة من النبتة")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Image deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Plant or image not found")
    })
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "Plant ID") @PathVariable Long plantId,
            @Parameter(description = "Image ID") @PathVariable Long imageId) {
        plantService.deleteImage(plantId, imageId);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{plantId}/images/{imageId}/primary")
    @Operation(summary = "Set primary image", description = "تعيين الصورة الرئيسية")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Primary image set successfully"),
        @ApiResponse(responseCode = "404", description = "Plant or image not found")
    })
    public ResponseEntity<PlantImageResponse> setPrimaryImage(
            @Parameter(description = "Plant ID") @PathVariable Long plantId,
            @Parameter(description = "Image ID") @PathVariable Long imageId) {
        PlantImageResponse response = plantService.setPrimaryImage(plantId, imageId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{plantId}/images/reorder")
    @Operation(summary = "Reorder images", description = "إعادة ترتيب صور النبتة")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Images reordered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid image IDs"),
        @ApiResponse(responseCode = "404", description = "Plant not found")
    })
    public ResponseEntity<Void> reorderImages(
            @Parameter(description = "Plant ID") @PathVariable Long plantId,
            @Valid @RequestBody ImageReorderRequest request) {
        plantService.reorderImages(plantId, request);
        return ResponseEntity.ok().build();
    }
    
    // ===== Video Operations =====
    
    @PutMapping("/{plantId}/video")
    @Operation(summary = "Update planting video", description = "تحديث رابط فيديو الزراعة")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Video URL updated successfully"),
        @ApiResponse(responseCode = "404", description = "Plant not found")
    })
    public ResponseEntity<PlantResponse> updatePlantingVideo(
            @Parameter(description = "Plant ID") @PathVariable Long plantId,
            @Valid @RequestBody PlantVideoUpdateRequest request) {
        PlantResponse response = plantService.updatePlantingVideo(plantId, request);
        return ResponseEntity.ok(response);
    }
}
