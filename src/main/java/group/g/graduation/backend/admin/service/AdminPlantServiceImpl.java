package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.admin.mapper.PlantMapper;
import group.g.graduation.backend.common.enums.PlantCategory;
import group.g.graduation.backend.common.exception.BadRequestException;
import group.g.graduation.backend.common.exception.DuplicateResourceException;
import group.g.graduation.backend.common.exception.ResourceNotFoundException;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantImage;
import group.g.graduation.backend.common.repository.PlantImageRepository;
import group.g.graduation.backend.common.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin Plant Service Implementation - تنفيذ خدمة إدارة النباتات للأدمن
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminPlantServiceImpl implements AdminPlantService {
    
    private final PlantRepository plantRepository;
    private final PlantImageRepository plantImageRepository;
    private final PlantMapper plantMapper;
    
    // ===== CRUD Operations =====
    
    @Override
    public PlantResponse createPlant(PlantCreateRequest request) {
        log.info("Creating new plant: {}", request.getNameAr());
        
        // ===== 1) التحقق من عدم وجود تكرار =====
        if (request.getNameAr() != null && plantRepository.existsByNameAr(request.getNameAr())) {
            log.warn("Plant with nameAr '{}' already exists", request.getNameAr());
            throw new DuplicateResourceException("Plant", "nameAr", request.getNameAr());
        }
        
        if (request.getNameScientific() != null && 
            !request.getNameScientific().trim().isEmpty() && 
            plantRepository.existsByNameScientific(request.getNameScientific())) {
            log.warn("Plant with nameScientific '{}' already exists", request.getNameScientific());
            throw new DuplicateResourceException("Plant", "nameScientific", request.getNameScientific());
        }
        
        // ===== 2) إنشاء Entity جديدة =====
        Plant plant = plantMapper.toEntity(request);
        
        // ===== 3) حفظ في قاعدة البيانات =====
        Plant savedPlant = plantRepository.save(plant);
        
        log.info("Plant created successfully with ID: {}", savedPlant.getId());
        
        // ===== 4) إرجاع Response =====
        return plantMapper.toResponse(savedPlant);
    }
    
    @Override
    public PlantResponse updatePlant(Long id, PlantUpdateRequest request) {
        log.info("Updating plant with ID: {}", id);
        
        Plant plant = findPlantById(id);
        plantMapper.updateEntity(plant, request);
        Plant updatedPlant = plantRepository.save(plant);
        
        log.info("Plant updated successfully: {}", id);
        return plantMapper.toResponse(updatedPlant);
    }
    
    @Override
    public void deletePlant(Long id) {
        log.info("Deleting plant with ID: {}", id);
        
        Plant plant = findPlantById(id);
        plantRepository.delete(plant);
        
        log.info("Plant deleted successfully: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlantResponse getPlantById(Long id) {
        log.debug("Getting plant by ID: {}", id);
        
        Plant plant = plantRepository.findByIdWithImages(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plant", "id", id));
        
        return plantMapper.toResponse(plant);
    }
    
    // ===== List & Search Operations =====
    
    @Override
    @Transactional(readOnly = true)
    public PlantListResponse getAllPlants(Pageable pageable) {
        log.debug("Getting all plants, page: {}", pageable.getPageNumber());
        
        Page<Plant> plantsPage = plantRepository.findAll(pageable);
        return plantMapper.toListResponse(plantsPage);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlantListResponse searchPlants(String keyword, Pageable pageable) {
        log.debug("Searching plants with keyword: {}", keyword);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllPlants(pageable);
        }
        
        // البحث في الاسم العربي أو الإنجليزي
        List<Plant> plantsAr = plantRepository.findByNameArContainingIgnoreCase(keyword.trim());
        List<Plant> plantsEn = plantRepository.findByNameEnContainingIgnoreCase(keyword.trim());
        
        // دمج النتائج وإزالة التكرار
        plantsAr.addAll(plantsEn);
        List<Plant> uniquePlants = plantsAr.stream()
                .distinct()
                .toList();
        
        // تحويل لـ Page يدوياً
        int start = Math.min((int) pageable.getOffset(), uniquePlants.size());
        int end = Math.min((start + pageable.getPageSize()), uniquePlants.size());
        
        List<Plant> pageContent = start < uniquePlants.size() ? uniquePlants.subList(start, end) : List.of();
        Page<Plant> page = new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, uniquePlants.size());
        
        return plantMapper.toListResponse(page);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlantListResponse getPlantsByCategory(PlantCategory category, Pageable pageable) {
        log.debug("Getting plants by category: {}", category);
        
        List<Plant> plants = plantRepository.findByCategory(category);
        
        int start = Math.min((int) pageable.getOffset(), plants.size());
        int end = Math.min((start + pageable.getPageSize()), plants.size());
        
        List<Plant> pageContent = start < plants.size() ? plants.subList(start, end) : List.of();
        Page<Plant> page = new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, plants.size());
        
        return plantMapper.toListResponse(page);
    }
    
    // ===== Image Operations =====
    
    @Override
    public PlantImageResponse addImage(Long plantId, PlantImageRequest request) {
        log.info("Adding image to plant ID: {}", plantId);
        
        Plant plant = findPlantById(plantId);
        
        // Defensive: ensure images list is not null
        if (plant.getImages() == null) {
            plant.setImages(new java.util.ArrayList<>());
        }
        
        // إذا كانت هذه الصورة الرئيسية، نزيل الـ primary من الصور الأخرى
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            plant.getImages().forEach(img -> img.setIsPrimary(false));
        }
        
        PlantImage image = plantMapper.toImageEntity(request, plant);
        
        // تعيين ترتيب العرض إذا لم يُحدد
        if (image.getDisplayOrder() == null) {
            image.setDisplayOrder(plant.getImages().size() + 1);
        }
        
        plant.getImages().add(image);
        plantRepository.save(plant);
        
        // جلب الصورة المحفوظة
        PlantImage savedImage = plant.getImages().get(plant.getImages().size() - 1);
        
        log.info("Image added successfully to plant: {}", plantId);
        return plantMapper.toImageResponse(savedImage);
    }
    
    @Override
    public void deleteImage(Long plantId, Long imageId) {
        log.info("Deleting image {} from plant {}", imageId, plantId);
        
        Plant plant = findPlantById(plantId);
        
        // Defensive: ensure images list is not null
        if (plant.getImages() == null || plant.getImages().isEmpty()) {
            throw new ResourceNotFoundException("Image", "id", imageId);
        }
        
        PlantImage image = plant.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));
        
        plant.getImages().remove(image);
        plantRepository.save(plant);
        
        log.info("Image deleted successfully: {}", imageId);
    }
    
    @Override
    public PlantImageResponse setPrimaryImage(Long plantId, Long imageId) {
        log.info("Setting primary image {} for plant {}", imageId, plantId);
        
        Plant plant = findPlantById(plantId);
        
        // Defensive: ensure images list is not null
        if (plant.getImages() == null || plant.getImages().isEmpty()) {
            throw new ResourceNotFoundException("Image", "id", imageId);
        }
        
        PlantImage targetImage = null;
        
        for (PlantImage img : plant.getImages()) {
            if (img.getId().equals(imageId)) {
                img.setIsPrimary(true);
                targetImage = img;
            } else {
                img.setIsPrimary(false);
            }
        }
        
        if (targetImage == null) {
            throw new ResourceNotFoundException("Image", "id", imageId);
        }
        
        plantRepository.save(plant);
        
        log.info("Primary image set successfully: {}", imageId);
        return plantMapper.toImageResponse(targetImage);
    }
    
    @Override
    public void reorderImages(Long plantId, ImageReorderRequest request) {
        log.info("Reordering images for plant {}", plantId);
        
        Plant plant = findPlantById(plantId);
        List<Long> newOrder = request.getImageIds();
        
        // Defensive: ensure images list is not null
        if (plant.getImages() == null) {
            plant.setImages(new java.util.ArrayList<>());
        }
        
        // التحقق من أن كل الصور موجودة
        if (newOrder.size() != plant.getImages().size()) {
            throw new BadRequestException("عدد الصور في الطلب لا يتطابق مع عدد صور النبتة");
        }
        
        // تحديث ترتيب العرض
        for (int i = 0; i < newOrder.size(); i++) {
            Long imageId = newOrder.get(i);
            int finalOrder = i + 1;
            
            plant.getImages().stream()
                    .filter(img -> img.getId().equals(imageId))
                    .findFirst()
                    .ifPresent(img -> img.setDisplayOrder(finalOrder));
        }
        
        plantRepository.save(plant);
        
        log.info("Images reordered successfully for plant: {}", plantId);
    }
    
    // ===== Video Operations =====
    
    @Override
    public PlantResponse updatePlantingVideo(Long plantId, PlantVideoUpdateRequest request) {
        log.info("Updating planting video for plant {}", plantId);
        
        Plant plant = findPlantById(plantId);
        plant.setPlantingVideoUrl(request.getPlantingVideoUrl());
        Plant updatedPlant = plantRepository.save(plant);
        
        log.info("Planting video updated successfully for plant: {}", plantId);
        return plantMapper.toResponse(updatedPlant);
    }
    
    // ===== Helper Methods =====
    
    /**
     * Find plant by ID or throw exception
     */
    private Plant findPlantById(Long id) {
        return plantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plant", "id", id));
    }
}