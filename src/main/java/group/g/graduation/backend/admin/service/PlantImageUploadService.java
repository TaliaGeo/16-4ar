package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.common.exception.BadRequestException;
import group.g.graduation.backend.common.exception.ResourceNotFoundException;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantImage;
import group.g.graduation.backend.common.repository.PlantImageRepository;
import group.g.graduation.backend.common.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Plant Image Upload Service - خدمة رفع صور النباتات
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlantImageUploadService {
    
    private final PlantRepository plantRepository;
    private final PlantImageRepository plantImageRepository;
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    /**
     * رفع صورة لنبتة
     */
    @Transactional
    public PlantImage uploadPlantImage(Long plantId, MultipartFile file, Boolean isPrimary) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new ResourceNotFoundException("Plant", "id", plantId));
        
        validateFile(file);
        
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        String filePath = saveFile(file, fileName, plantId);
        
        // إذا كانت الصورة رئيسية، نلغي الرئيسية السابقة
        if (Boolean.TRUE.equals(isPrimary)) {
            plantImageRepository.findByPlantIdAndIsPrimaryTrue(plantId)
                    .ifPresent(img -> {
                        img.setIsPrimary(false);
                        plantImageRepository.save(img);
                    });
        }
        
        // الحصول على أعلى ترتيب حالي
        Integer maxOrder = plantImageRepository.findMaxDisplayOrderByPlantId(plantId);
        int nextOrder = (maxOrder != null ? maxOrder : 0) + 1;
        
        PlantImage plantImage = new PlantImage();
        plantImage.setPlant(plant);
        plantImage.setImageUrl("/uploads/plants/" + plantId + "/" + fileName);
        plantImage.setAltTextAr(plant.getNameAr());
        plantImage.setAltTextEn(plant.getNameEn());
        plantImage.setIsPrimary(Boolean.TRUE.equals(isPrimary));
        plantImage.setDisplayOrder(nextOrder);
        
        PlantImage saved = plantImageRepository.save(plantImage);
        log.info("Uploaded image for plant {}: {}", plantId, fileName);
        
        return saved;
    }
    
    /**
     * رفع عدة صور لنبتة
     */
    @Transactional
    public List<PlantImage> uploadPlantImages(Long plantId, List<MultipartFile> files, Long primaryImageIndex) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new ResourceNotFoundException("Plant", "id", plantId));
        
        List<PlantImage> uploadedImages = new ArrayList<>();
        
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            boolean isPrimary = primaryImageIndex != null && primaryImageIndex == i;
            PlantImage image = uploadPlantImage(plantId, file, isPrimary);
            uploadedImages.add(image);
        }
        
        return uploadedImages;
    }
    
    /**
     * حذف صورة نبتة
     */
    @Transactional
    public void deletePlantImage(Long imageId) {
        PlantImage image = plantImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("PlantImage", "id", imageId));
        
        // حذف الملف من النظام فقط إذا كان ملف محلي
        String imageUrl = image.getImageUrl();
        if (imageUrl != null && !imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            try {
                Path filePath = Paths.get(uploadDir + imageUrl.replace("/uploads", ""));
                Files.deleteIfExists(filePath);
                log.info("Deleted file from disk: {}", filePath);
            } catch (IOException e) {
                log.warn("Could not delete file: {}", imageUrl);
            }
        } else {
            log.info("Skipping file deletion for external URL: {}", imageUrl);
        }
        
        plantImageRepository.delete(image);
        log.info("Deleted plant image: {}", imageId);
    }
    
    /**
     * تعيين صورة كرئيسية
     */
    @Transactional
    public void setPrimaryImage(Long plantId, Long imageId) {
        PlantImage image = plantImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("PlantImage", "id", imageId));
        
        if (!image.getPlant().getId().equals(plantId)) {
            throw new BadRequestException("Image does not belong to the specified plant");
        }
        
        // إلغاء الصورة الرئيسية الحالية
        plantImageRepository.findByPlantIdAndIsPrimaryTrue(plantId)
                .ifPresent(img -> {
                    img.setIsPrimary(false);
                    plantImageRepository.save(img);
                });
        
        // تعيين الصورة الجديدة كرئيسية
        image.setIsPrimary(true);
        plantImageRepository.save(image);
        
        log.info("Set primary image {} for plant {}", imageId, plantId);
    }
    
    /**
     * جلب صور نبتة
     */
    public List<PlantImage> getPlantImages(Long plantId) {
        if (!plantRepository.existsById(plantId)) {
            throw new ResourceNotFoundException("Plant", "id", plantId);
        }
        return plantImageRepository.findByPlantIdOrderByIsPrimaryDescDisplayOrderAsc(plantId);
    }
    
    /**
     * إعادة ترتيب الصور
     */
    @Transactional
    public void reorderImages(Long plantId, List<Long> imageIds) {
        if (!plantRepository.existsById(plantId)) {
            throw new ResourceNotFoundException("Plant", "id", plantId);
        }
        
        for (int i = 0; i < imageIds.size(); i++) {
            Long imageId = imageIds.get(i);
            PlantImage image = plantImageRepository.findById(imageId)
                    .orElseThrow(() -> new ResourceNotFoundException("PlantImage", "id", imageId));
            
            if (!image.getPlant().getId().equals(plantId)) {
                throw new BadRequestException("Image " + imageId + " does not belong to plant " + plantId);
            }
            
            image.setDisplayOrder(i + 1);
            plantImageRepository.save(image);
        }
        
        log.info("Reordered {} images for plant {}", imageIds.size(), plantId);
    }
    
    /**
     * تحديث النص البديل للصورة
     */
    @Transactional
    public PlantImage updateImageAltText(Long plantId, Long imageId, String altTextAr, String altTextEn) {
        PlantImage image = plantImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("PlantImage", "id", imageId));
        
        if (!image.getPlant().getId().equals(plantId)) {
            throw new BadRequestException("Image does not belong to the specified plant");
        }
        
        if (altTextAr != null) {
            image.setAltTextAr(altTextAr);
        }
        if (altTextEn != null) {
            image.setAltTextEn(altTextEn);
        }
        
        return plantImageRepository.save(image);
    }
    
    // ===== Helper Methods =====
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed (10MB)");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BadRequestException("Invalid file name");
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("File type not allowed. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }
    
    private String generateUniqueFileName(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }
    
    private String saveFile(MultipartFile file, String fileName, Long plantId) {
        try {
            Path uploadPath = Paths.get(uploadDir, "plants", String.valueOf(plantId));
            Files.createDirectories(uploadPath);
            
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to save file: {}", e.getMessage());
            throw new BadRequestException("Failed to save file: " + e.getMessage());
        }
    }
}
