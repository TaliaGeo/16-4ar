package group.g.graduation.backend.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import group.g.graduation.backend.admin.dto.PlantImageRequest;
import group.g.graduation.backend.admin.dto.PlantImageResponse;
import group.g.graduation.backend.admin.dto.PlantTaskRequest;
import group.g.graduation.backend.admin.dto.PlantTaskResponse;
import group.g.graduation.backend.admin.dto.TaskTypeRequest;
import group.g.graduation.backend.admin.dto.TaskTypeResponse;
import group.g.graduation.backend.admin.mapper.PlantTaskMapper;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantImage;
import group.g.graduation.backend.common.model.PlantTask;
import group.g.graduation.backend.common.model.TaskType;
import group.g.graduation.backend.common.repository.PlantImageRepository;
import group.g.graduation.backend.common.repository.PlantRepository;
import group.g.graduation.backend.common.repository.PlantTaskRepository;
import group.g.graduation.backend.common.repository.TaskTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of AdminPlantTaskService
 * تنفيذ خدمة إدارة صور النباتات وأنواع المهام ومهام النباتات للأدمن
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminPlantTaskServiceImpl implements AdminPlantTaskService {
    
    private final PlantRepository plantRepository;
    private final PlantImageRepository plantImageRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final PlantTaskRepository plantTaskRepository;
    private final PlantTaskMapper mapper;
    
    // ===================== PlantImage Operations =====================
    
    @Override
    public PlantImageResponse addImageToPlant(Long plantId, PlantImageRequest request) {
        log.info("إضافة صورة للنبتة - ID: {}", plantId);
        
        Plant plant = findPlantById(plantId);
        PlantImage image = mapper.toEntity(request, plant);
        
        // إذا كانت الصورة الأولى، اجعلها رئيسية
        List<PlantImage> existingImages = plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(plantId);
        if (existingImages.isEmpty()) {
            image.setIsPrimary(true);
        }
        
        // إذا كانت رئيسية، أزل الرئيسية من الصور الأخرى
        if (Boolean.TRUE.equals(image.getIsPrimary())) {
            existingImages.forEach(img -> img.setIsPrimary(false));
            plantImageRepository.saveAll(existingImages);
        }
        
        PlantImage saved = plantImageRepository.save(image);
        log.info("تم إضافة الصورة بنجاح - ID: {}", saved.getId());
        return mapper.toImageResponse(saved);
    }
    
    @Override
    public PlantImageResponse updateImage(Long imageId, PlantImageRequest request) {
        log.info("تحديث الصورة - ID: {}", imageId);
        
        PlantImage image = findImageById(imageId);
        
        // إذا تم تعيينها كرئيسية
        if (Boolean.TRUE.equals(request.getIsPrimary()) && !Boolean.TRUE.equals(image.getIsPrimary())) {
            List<PlantImage> otherImages = plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(image.getPlant().getId());
            otherImages.forEach(img -> img.setIsPrimary(false));
            plantImageRepository.saveAll(otherImages);
        }
        
        mapper.updateEntity(image, request);
        PlantImage updated = plantImageRepository.save(image);
        
        log.info("تم تحديث الصورة بنجاح - ID: {}", updated.getId());
        return mapper.toImageResponse(updated);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlantImageResponse getImageById(Long imageId) {
        return mapper.toImageResponse(findImageById(imageId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantImageResponse> getImagesByPlant(Long plantId) {
        // التحقق من وجود النبتة
        if (!plantRepository.existsById(plantId)) {
            throw new EntityNotFoundException("لم يتم العثور على النبتة - ID: " + plantId);
        }
        
        List<PlantImage> images = plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(plantId);
        return mapper.toImageResponseList(images);
    }
    
    @Override
    public PlantImageResponse setAsPrimaryImage(Long imageId) {
        log.info("تعيين صورة كرئيسية - ID: {}", imageId);
        
        PlantImage image = findImageById(imageId);
        
        // أزل الرئيسية من الصور الأخرى
        List<PlantImage> otherImages = plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(image.getPlant().getId());
        otherImages.forEach(img -> img.setIsPrimary(false));
        plantImageRepository.saveAll(otherImages);
        
        // عيّن هذه كرئيسية
        image.setIsPrimary(true);
        PlantImage updated = plantImageRepository.save(image);
        
        log.info("تم تعيين الصورة كرئيسية بنجاح - ID: {}", updated.getId());
        return mapper.toImageResponse(updated);
    }
    
    @Override
    public void deleteImage(Long imageId) {
        log.info("حذف الصورة - ID: {}", imageId);
        
        PlantImage image = findImageById(imageId);
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        Long plantId = image.getPlant().getId();
        
        plantImageRepository.delete(image);
        
        // إذا كانت رئيسية، عيّن أول صورة متبقية كرئيسية
        if (wasPrimary) {
            List<PlantImage> remainingImages = plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(plantId);
            if (!remainingImages.isEmpty()) {
                remainingImages.get(0).setIsPrimary(true);
                plantImageRepository.save(remainingImages.get(0));
            }
        }
        
        log.info("تم حذف الصورة بنجاح - ID: {}", imageId);
    }
    
    @Override
    public void deleteAllPlantImages(Long plantId) {
        log.info("حذف جميع صور النبتة - ID: {}", plantId);
        
        if (!plantRepository.existsById(plantId)) {
            throw new EntityNotFoundException("لم يتم العثور على النبتة - ID: " + plantId);
        }
        
        plantImageRepository.deleteByPlantId(plantId);
        log.info("تم حذف جميع صور النبتة بنجاح - ID: {}", plantId);
    }
    
    // ===================== TaskType Operations =====================
    
    @Override
    public TaskTypeResponse createTaskType(TaskTypeRequest request) {
        log.info("إنشاء نوع مهمة جديد: {}", request.getNameAr());
        
        // التحقق من عدم وجود نوع مهمة بنفس الاسم
        if (taskTypeRepository.findByNameAr(request.getNameAr()).isPresent()) {
            throw new IllegalArgumentException("يوجد نوع مهمة بنفس الاسم: " + request.getNameAr());
        }
        
        TaskType taskType = mapper.toEntity(request);
        TaskType saved = taskTypeRepository.save(taskType);
        
        log.info("تم إنشاء نوع المهمة بنجاح - ID: {}", saved.getId());
        return mapper.toTaskTypeResponse(saved);
    }
    
    @Override
    public TaskTypeResponse updateTaskType(Long id, TaskTypeRequest request) {
        log.info("تحديث نوع المهمة - ID: {}", id);
        
        TaskType taskType = findTaskTypeById(id);
        
        // التحقق من عدم وجود نوع مهمة آخر بنفس الاسم
        if (!taskType.getNameAr().equals(request.getNameAr())) {
            taskTypeRepository.findByNameAr(request.getNameAr())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("يوجد نوع مهمة آخر بنفس الاسم: " + request.getNameAr());
                    });
        }
        
        mapper.updateEntity(taskType, request);
        TaskType updated = taskTypeRepository.save(taskType);
        
        log.info("تم تحديث نوع المهمة بنجاح - ID: {}", updated.getId());
        return mapper.toTaskTypeResponse(updated);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TaskTypeResponse getTaskTypeById(Long id) {
        return mapper.toTaskTypeResponse(findTaskTypeById(id));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TaskTypeResponse> getAllTaskTypes() {
        List<TaskType> taskTypes = taskTypeRepository.findAll();
        return mapper.toTaskTypeResponseList(taskTypes);
    }
    
    @Override
    public void deleteTaskType(Long id) {
        log.info("حذف نوع المهمة - ID: {}", id);
        
        TaskType taskType = findTaskTypeById(id);
        
        // التحقق من عدم وجود مهام مرتبطة
        List<PlantTask> linkedTasks = plantTaskRepository.findByTaskType(taskType);
        if (!linkedTasks.isEmpty()) {
            throw new IllegalStateException("لا يمكن حذف نوع المهمة لأنه مرتبط بـ " + linkedTasks.size() + " مهمة");
        }
        
        taskTypeRepository.delete(taskType);
        log.info("تم حذف نوع المهمة بنجاح - ID: {}", id);
    }
    
    // ===================== PlantTask Operations =====================
    
    @Override
    public PlantTaskResponse addTaskToPlant(Long plantId, PlantTaskRequest request) {
        log.info("إضافة مهمة للنبتة {} - نوع المهمة: {}", plantId, request.getTaskTypeId());
        
        Plant plant = findPlantById(plantId);
        TaskType taskType = findTaskTypeById(request.getTaskTypeId());
        
        PlantTask task = mapper.toEntity(request, plant, taskType);
        PlantTask saved = plantTaskRepository.save(task);
        
        log.info("تم إضافة المهمة بنجاح - ID: {}", saved.getId());
        return mapper.toPlantTaskResponse(saved);
    }
    
    @Override
    public PlantTaskResponse updatePlantTask(Long taskId, PlantTaskRequest request) {
        log.info("تحديث مهمة النبتة - ID: {}", taskId);
        
        PlantTask task = findPlantTaskById(taskId);
        TaskType taskType = null;
        
        if (request.getTaskTypeId() != null && !request.getTaskTypeId().equals(task.getTaskType().getId())) {
            taskType = findTaskTypeById(request.getTaskTypeId());
        }
        
        mapper.updateEntity(task, request, taskType);
        PlantTask updated = plantTaskRepository.save(task);
        
        log.info("تم تحديث مهمة النبتة بنجاح - ID: {}", updated.getId());
        return mapper.toPlantTaskResponse(updated);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlantTaskResponse getPlantTaskById(Long taskId) {
        return mapper.toPlantTaskResponse(findPlantTaskById(taskId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantTaskResponse> getTasksByPlant(Long plantId) {
        if (!plantRepository.existsById(plantId)) {
            throw new EntityNotFoundException("لم يتم العثور على النبتة - ID: " + plantId);
        }
        
        List<PlantTask> tasks = plantTaskRepository.findByPlantIdWithTaskType(plantId);
        return mapper.toPlantTaskResponseList(tasks);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantTaskResponse> getTasksByType(Long taskTypeId) {
        TaskType taskType = findTaskTypeById(taskTypeId);
        List<PlantTask> tasks = plantTaskRepository.findByTaskType(taskType);
        return mapper.toPlantTaskResponseList(tasks);
    }
    
    @Override
    public void deletePlantTask(Long taskId) {
        log.info("حذف مهمة النبتة - ID: {}", taskId);
        
        PlantTask task = findPlantTaskById(taskId);
        plantTaskRepository.delete(task);
        
        log.info("تم حذف مهمة النبتة بنجاح - ID: {}", taskId);
    }
    
    @Override
    public void deleteAllPlantTasks(Long plantId) {
        log.info("حذف جميع مهام النبتة - ID: {}", plantId);
        
        if (!plantRepository.existsById(plantId)) {
            throw new EntityNotFoundException("لم يتم العثور على النبتة - ID: " + plantId);
        }
        
        List<PlantTask> tasks = plantTaskRepository.findByPlantId(plantId);
        plantTaskRepository.deleteAll(tasks);
        
        log.info("تم حذف {} مهمة للنبتة - ID: {}", tasks.size(), plantId);
    }
    
    // ===================== Private Helper Methods =====================
    
    private Plant findPlantById(Long id) {
        return plantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على النبتة - ID: " + id));
    }
    
    private PlantImage findImageById(Long id) {
        return plantImageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على الصورة - ID: " + id));
    }
    
    private TaskType findTaskTypeById(Long id) {
        return taskTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على نوع المهمة - ID: " + id));
    }
    
    private PlantTask findPlantTaskById(Long id) {
        return plantTaskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("لم يتم العثور على مهمة النبتة - ID: " + id));
    }
}
