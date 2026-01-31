package group.g.graduation.backend.admin.mapper;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantImage;
import group.g.graduation.backend.common.model.PlantTask;
import group.g.graduation.backend.common.model.TaskType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for PlantImage, TaskType, and PlantTask entities
 * محوّل كيانات صور النباتات وأنواع المهام ومهام النباتات
 */
@Component
public class PlantTaskMapper {
    
    // ===================== PlantImage Mapping =====================
    
    /**
     * تحويل طلب صورة إلى كيان جديد
     */
    public PlantImage toEntity(PlantImageRequest request, Plant plant) {
        if (request == null || plant == null) return null;
        
        PlantImage image = new PlantImage();
        image.setPlant(plant);
        image.setImageUrl(request.getImageUrl());
        image.setAltTextAr(request.getAltTextAr());
        image.setAltTextEn(request.getAltTextEn());
        image.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false);
        image.setDisplayOrder(request.getDisplayOrder());
        return image;
    }
    
    /**
     * تحديث كيان صورة من الطلب
     */
    public void updateEntity(PlantImage image, PlantImageRequest request) {
        if (image == null || request == null) return;
        
        if (request.getImageUrl() != null) image.setImageUrl(request.getImageUrl());
        if (request.getAltTextAr() != null) image.setAltTextAr(request.getAltTextAr());
        if (request.getAltTextEn() != null) image.setAltTextEn(request.getAltTextEn());
        if (request.getIsPrimary() != null) image.setIsPrimary(request.getIsPrimary());
        if (request.getDisplayOrder() != null) image.setDisplayOrder(request.getDisplayOrder());
    }
    
    /**
     * تحويل كيان صورة إلى استجابة
     */
    public PlantImageResponse toImageResponse(PlantImage image) {
        if (image == null) return null;
        
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
    
    /**
     * تحويل قائمة صور إلى استجابات
     */
    public List<PlantImageResponse> toImageResponseList(List<PlantImage> images) {
        if (images == null) return Collections.emptyList();
        return images.stream().map(this::toImageResponse).collect(Collectors.toList());
    }
    
    // ===================== TaskType Mapping =====================
    
    /**
     * تحويل طلب نوع مهمة إلى كيان جديد
     */
    public TaskType toEntity(TaskTypeRequest request) {
        if (request == null) return null;
        
        TaskType taskType = new TaskType();
        taskType.setNameAr(request.getNameAr());
        taskType.setNameEn(request.getNameEn());
        taskType.setIcon(request.getIcon());
        taskType.setDescriptionAr(request.getDescriptionAr());
        taskType.setDescriptionEn(request.getDescriptionEn());
        return taskType;
    }
    
    /**
     * تحديث كيان نوع مهمة من الطلب
     */
    public void updateEntity(TaskType taskType, TaskTypeRequest request) {
        if (taskType == null || request == null) return;
        
        if (request.getNameAr() != null) taskType.setNameAr(request.getNameAr());
        if (request.getNameEn() != null) taskType.setNameEn(request.getNameEn());
        if (request.getIcon() != null) taskType.setIcon(request.getIcon());
        if (request.getDescriptionAr() != null) taskType.setDescriptionAr(request.getDescriptionAr());
        if (request.getDescriptionEn() != null) taskType.setDescriptionEn(request.getDescriptionEn());
    }
    
    /**
     * تحويل كيان نوع مهمة إلى استجابة
     */
    public TaskTypeResponse toTaskTypeResponse(TaskType taskType) {
        if (taskType == null) return null;
        
        return TaskTypeResponse.builder()
                .id(taskType.getId())
                .nameAr(taskType.getNameAr())
                .nameEn(taskType.getNameEn())
                .icon(taskType.getIcon())
                .descriptionAr(taskType.getDescriptionAr())
                .descriptionEn(taskType.getDescriptionEn())
                .plantsCount(taskType.getPlantTasks() != null ? taskType.getPlantTasks().size() : 0)
                .build();
    }
    
    /**
     * تحويل قائمة أنواع المهام إلى استجابات
     */
    public List<TaskTypeResponse> toTaskTypeResponseList(List<TaskType> taskTypes) {
        if (taskTypes == null) return Collections.emptyList();
        return taskTypes.stream().map(this::toTaskTypeResponse).collect(Collectors.toList());
    }
    
    // ===================== PlantTask Mapping =====================
    
    /**
     * تحويل طلب مهمة نبتة إلى كيان جديد
     */
    public PlantTask toEntity(PlantTaskRequest request, Plant plant, TaskType taskType) {
        if (request == null || plant == null || taskType == null) return null;
        
        PlantTask task = new PlantTask();
        task.setPlant(plant);
        task.setTaskType(taskType);
        task.setIntervalDays(request.getIntervalDays());
        task.setDescriptionAr(request.getDescriptionAr());
        task.setDescriptionEn(request.getDescriptionEn());
        task.setStartDayAfterPlanting(request.getStartDayAfterPlanting() != null ? request.getStartDayAfterPlanting() : 0);
        task.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : true);
        return task;
    }
    
    /**
     * تحديث كيان مهمة نبتة من الطلب
     */
    public void updateEntity(PlantTask task, PlantTaskRequest request, TaskType taskType) {
        if (task == null || request == null) return;
        
        if (taskType != null) task.setTaskType(taskType);
        if (request.getIntervalDays() != null) task.setIntervalDays(request.getIntervalDays());
        if (request.getDescriptionAr() != null) task.setDescriptionAr(request.getDescriptionAr());
        if (request.getDescriptionEn() != null) task.setDescriptionEn(request.getDescriptionEn());
        if (request.getStartDayAfterPlanting() != null) task.setStartDayAfterPlanting(request.getStartDayAfterPlanting());
        if (request.getIsRecurring() != null) task.setIsRecurring(request.getIsRecurring());
    }
    
    /**
     * تحويل كيان مهمة نبتة إلى استجابة
     */
    public PlantTaskResponse toPlantTaskResponse(PlantTask task) {
        if (task == null) return null;
        
        return PlantTaskResponse.builder()
                .id(task.getId())
                .plantId(task.getPlant().getId())
                .plantNameAr(task.getPlant().getNameAr())
                .taskTypeId(task.getTaskType().getId())
                .taskTypeNameAr(task.getTaskType().getNameAr())
                .taskTypeNameEn(task.getTaskType().getNameEn())
                .taskTypeIcon(task.getTaskType().getIcon())
                .intervalDays(task.getIntervalDays())
                .descriptionAr(task.getDescriptionAr())
                .descriptionEn(task.getDescriptionEn())
                .startDayAfterPlanting(task.getStartDayAfterPlanting())
                .isRecurring(task.getIsRecurring())
                .build();
    }
    
    /**
     * تحويل قائمة مهام النباتات إلى استجابات
     */
    public List<PlantTaskResponse> toPlantTaskResponseList(List<PlantTask> tasks) {
        if (tasks == null) return Collections.emptyList();
        return tasks.stream().map(this::toPlantTaskResponse).collect(Collectors.toList());
    }
}
