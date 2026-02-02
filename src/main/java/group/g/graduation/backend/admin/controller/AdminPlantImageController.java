package group.g.graduation.backend.admin.controller;

import group.g.graduation.backend.admin.dto.PlantImageResponse;
import group.g.graduation.backend.admin.service.PlantImageUploadService;
import group.g.graduation.backend.common.model.PlantImage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Plant Image Controller - إدارة صور النباتات
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/plants/{plantId}/images")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Plant Images", description = "إدارة صور النباتات")
@SecurityRequirement(name = "bearerAuth")
public class AdminPlantImageController {
    
    private final PlantImageUploadService imageUploadService;
    
    /**
     * رفع صورة واحدة لنبتة
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "رفع صورة لنبتة")
    public ResponseEntity<PlantImageResponse> uploadImage(
            @PathVariable Long plantId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", defaultValue = "false") Boolean isPrimary) {
        
        log.info("Uploading image for plant {}, isPrimary: {}", plantId, isPrimary);
        PlantImage image = imageUploadService.uploadPlantImage(plantId, file, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(image));
    }
    
    /**
     * رفع عدة صور لنبتة
     */
    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "رفع عدة صور لنبتة")
    public ResponseEntity<List<PlantImageResponse>> uploadMultipleImages(
            @PathVariable Long plantId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "primaryIndex", required = false) Long primaryIndex) {
        
        log.info("Uploading {} images for plant {}", files.size(), plantId);
        List<PlantImage> images = imageUploadService.uploadPlantImages(plantId, files, primaryIndex);
        List<PlantImageResponse> responses = images.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
    
    /**
     * جلب كل صور نبتة
     */
    @GetMapping
    @Operation(summary = "جلب كل صور نبتة")
    public ResponseEntity<List<PlantImageResponse>> getPlantImages(@PathVariable Long plantId) {
        List<PlantImage> images = imageUploadService.getPlantImages(plantId);
        List<PlantImageResponse> responses = images.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    /**
     * حذف صورة
     */
    @DeleteMapping("/{imageId}")
    @Operation(summary = "حذف صورة نبتة")
    public ResponseEntity<Map<String, String>> deleteImage(
            @PathVariable Long plantId,
            @PathVariable Long imageId) {
        
        log.info("Deleting image {} from plant {}", imageId, plantId);
        imageUploadService.deletePlantImage(imageId);
        return ResponseEntity.ok(Map.of(
                "message", "Image deleted successfully",
                "messageAr", "تم حذف الصورة بنجاح"
        ));
    }
    
    /**
     * تعيين صورة كرئيسية
     */
    @PatchMapping("/{imageId}/set-primary")
    @Operation(summary = "تعيين صورة كرئيسية")
    public ResponseEntity<Map<String, String>> setPrimaryImage(
            @PathVariable Long plantId,
            @PathVariable Long imageId) {
        
        log.info("Setting image {} as primary for plant {}", imageId, plantId);
        imageUploadService.setPrimaryImage(plantId, imageId);
        return ResponseEntity.ok(Map.of(
                "message", "Image set as primary successfully",
                "messageAr", "تم تعيين الصورة كرئيسية بنجاح"
        ));
    }
    
    /**
     * إعادة ترتيب الصور
     */
    @PutMapping("/reorder")
    @Operation(summary = "إعادة ترتيب صور نبتة")
    public ResponseEntity<Map<String, String>> reorderImages(
            @PathVariable Long plantId,
            @RequestBody List<Long> imageIds) {
        
        log.info("Reordering {} images for plant {}", imageIds.size(), plantId);
        imageUploadService.reorderImages(plantId, imageIds);
        return ResponseEntity.ok(Map.of(
                "message", "Images reordered successfully",
                "messageAr", "تم إعادة ترتيب الصور بنجاح"
        ));
    }
    
    /**
     * تحديث النص البديل للصورة
     */
    @PatchMapping("/{imageId}/alt-text")
    @Operation(summary = "تحديث النص البديل للصورة")
    public ResponseEntity<PlantImageResponse> updateAltText(
            @PathVariable Long plantId,
            @PathVariable Long imageId,
            @RequestBody Map<String, String> altTexts) {
        
        log.info("Updating alt text for image {} of plant {}", imageId, plantId);
        PlantImage image = imageUploadService.updateImageAltText(
                plantId, 
                imageId, 
                altTexts.get("altTextAr"), 
                altTexts.get("altTextEn")
        );
        return ResponseEntity.ok(toResponse(image));
    }
    
    // ===== Helper Methods =====
    
    private PlantImageResponse toResponse(PlantImage image) {
        return PlantImageResponse.builder()
                .id(image.getId())
                .plantId(image.getPlant().getId())
                .plantNameAr(image.getPlant().getNameAr())
                .imageUrl(image.getImageUrl())
                .altTextAr(image.getAltTextAr())
                .altTextEn(image.getAltTextEn())
                .isPrimary(image.getIsPrimary())
                .displayOrder(image.getDisplayOrder())
                .build();
    }
}
