package group.g.graduation.backend.admin.service;

import java.util.List;

import group.g.graduation.backend.admin.dto.PlantImageRequest;
import group.g.graduation.backend.admin.dto.PlantImageResponse;
import group.g.graduation.backend.admin.dto.PlantTaskRequest;
import group.g.graduation.backend.admin.dto.PlantTaskResponse;
import group.g.graduation.backend.admin.dto.TaskTypeRequest;
import group.g.graduation.backend.admin.dto.TaskTypeResponse;

/**
 * Admin service for PlantImage, TaskType, and PlantTask management
 * خدمة إدارة صور النباتات وأنواع المهام ومهام النباتات للأدمن
 */
public interface AdminPlantTaskService {
    
    // ===================== PlantImage Operations =====================
    
    /**
     * إضافة صورة لنبتة
     */
    PlantImageResponse addImageToPlant(Long plantId, PlantImageRequest request);
    
    /**
     * تحديث صورة
     */
    PlantImageResponse updateImage(Long imageId, PlantImageRequest request);
    
    /**
     * الحصول على صورة بالمعرّف
     */
    PlantImageResponse getImageById(Long imageId);
    
    /**
     * الحصول على صور نبتة
     */
    List<PlantImageResponse> getImagesByPlant(Long plantId);
    
    /**
     * تعيين صورة كصورة رئيسية
     */
    PlantImageResponse setAsPrimaryImage(Long imageId);
    
    /**
     * حذف صورة
     */
    void deleteImage(Long imageId);
    
    /**
     * حذف جميع صور نبتة
     */
    void deleteAllPlantImages(Long plantId);
    
    // ===================== TaskType Operations =====================
    
    /**
     * إنشاء نوع مهمة جديد
     */
    TaskTypeResponse createTaskType(TaskTypeRequest request);
    
    /**
     * تحديث نوع مهمة
     */
    TaskTypeResponse updateTaskType(Long id, TaskTypeRequest request);
    
    /**
     * الحصول على نوع مهمة بالمعرّف
     */
    TaskTypeResponse getTaskTypeById(Long id);
    
    /**
     * الحصول على جميع أنواع المهام
     */
    List<TaskTypeResponse> getAllTaskTypes();
    
    /**
     * حذف نوع مهمة
     */
    void deleteTaskType(Long id);
    
    // ===================== PlantTask Operations =====================
    
    /**
     * إضافة مهمة لنبتة
     */
    PlantTaskResponse addTaskToPlant(Long plantId, PlantTaskRequest request);
    
    /**
     * تحديث مهمة نبتة
     */
    PlantTaskResponse updatePlantTask(Long taskId, PlantTaskRequest request);
    
    /**
     * الحصول على مهمة بالمعرّف
     */
    PlantTaskResponse getPlantTaskById(Long taskId);
    
    /**
     * الحصول على مهام نبتة
     */
    List<PlantTaskResponse> getTasksByPlant(Long plantId);
    
    /**
     * الحصول على جميع مهام نوع معين
     */
    List<PlantTaskResponse> getTasksByType(Long taskTypeId);
    
    /**
     * حذف مهمة
     */
    void deletePlantTask(Long taskId);
    
    /**
     * حذف جميع مهام نبتة
     */
    void deleteAllPlantTasks(Long plantId);
}
