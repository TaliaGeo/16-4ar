package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.common.enums.PlantCategory;
import org.springframework.data.domain.Pageable;

/**
 * Admin Plant Service Interface - خدمة إدارة النباتات للأدمن
 */
public interface AdminPlantService {
    
    // ===== CRUD Operations =====
    
    /**
     * Create a new plant - إنشاء نبتة جديدة
     * @param request Plant creation request
     * @return Created plant response
     */
    PlantResponse createPlant(PlantCreateRequest request);
    
    /**
     * Update an existing plant - تعديل نبتة موجودة
     * @param id Plant ID
     * @param request Plant update request
     * @return Updated plant response
     */
    PlantResponse updatePlant(Long id, PlantUpdateRequest request);
    
    /**
     * Delete a plant - حذف نبتة
     * @param id Plant ID
     */
    void deletePlant(Long id);
    
    /**
     * Get plant by ID - جلب نبتة بالمعرف
     * @param id Plant ID
     * @return Plant response
     */
    PlantResponse getPlantById(Long id);
    
    // ===== List & Search Operations =====
    
    /**
     * Get all plants with pagination - جلب كل النباتات مع الترقيم
     * @param pageable Pagination info
     * @return Paginated plant list
     */
    PlantListResponse getAllPlants(Pageable pageable);
    
    /**
     * Search plants by keyword - البحث عن نباتات بكلمة مفتاحية
     * @param keyword Search keyword
     * @param pageable Pagination info
     * @return Paginated plant list
     */
    PlantListResponse searchPlants(String keyword, Pageable pageable);
    
    /**
     * Get plants by category - جلب النباتات حسب التصنيف
     * @param category Plant category
     * @param pageable Pagination info
     * @return Paginated plant list
     */
    PlantListResponse getPlantsByCategory(PlantCategory category, Pageable pageable);
    
    // ===== Image Operations =====
    
    /**
     * Add image to plant - إضافة صورة للنبتة
     * @param plantId Plant ID
     * @param request Image request
     * @return Added image response
     */
    PlantImageResponse addImage(Long plantId, PlantImageRequest request);
    
    /**
     * Delete image from plant - حذف صورة من النبتة
     * @param plantId Plant ID
     * @param imageId Image ID
     */
    void deleteImage(Long plantId, Long imageId);
    
    /**
     * Set primary image - تعيين الصورة الرئيسية
     * @param plantId Plant ID
     * @param imageId Image ID
     * @return Updated image response
     */
    PlantImageResponse setPrimaryImage(Long plantId, Long imageId);
    
    /**
     * Reorder images - إعادة ترتيب الصور
     * @param plantId Plant ID
     * @param request Reorder request with image IDs in new order
     */
    void reorderImages(Long plantId, ImageReorderRequest request);
    
    // ===== Video Operations =====
    
    /**
     * Update planting video URL - تحديث رابط فيديو الزراعة
     * @param plantId Plant ID
     * @param request Video update request
     * @return Updated plant response
     */
    PlantResponse updatePlantingVideo(Long plantId, PlantVideoUpdateRequest request);
}
