package group.g.graduation.backend.admin.controller;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.admin.service.AdminPlantTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for Task Types and Plant Tasks management
 * تحكم إدارة أنواع المهام ومهام النباتات
 * 
 * Note: Plant Image endpoints are already in AdminPlantController
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlantTaskController {
    
    private final AdminPlantTaskService plantTaskService;
    
    // ===================== Task Type Endpoints =====================
    
    @PostMapping("/task-types")
    @Tag(name = "Admin - Task Types", description = "إدارة أنواع المهام")
    @Operation(summary = "إنشاء نوع مهمة", description = "إضافة نوع مهمة جديد (ري، تسميد، تقليم...)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "تم الإنشاء بنجاح"),
            @ApiResponse(responseCode = "400", description = "بيانات غير صالحة")
    })
    public ResponseEntity<TaskTypeResponse> createTaskType(
            @Valid @RequestBody TaskTypeRequest request) {
        log.info("REST: إنشاء نوع مهمة جديد: {}", request.getNameAr());
        TaskTypeResponse response = plantTaskService.createTaskType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/task-types/{id}")
    @Tag(name = "Admin - Task Types")
    @Operation(summary = "تحديث نوع مهمة", description = "تعديل بيانات نوع مهمة")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم التحديث بنجاح"),
            @ApiResponse(responseCode = "404", description = "نوع المهمة غير موجود")
    })
    public ResponseEntity<TaskTypeResponse> updateTaskType(
            @Parameter(description = "معرّف نوع المهمة") @PathVariable Long id,
            @Valid @RequestBody TaskTypeRequest request) {
        log.info("REST: تحديث نوع المهمة - ID: {}", id);
        TaskTypeResponse response = plantTaskService.updateTaskType(id, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/task-types/{id}")
    @Tag(name = "Admin - Task Types")
    @Operation(summary = "الحصول على نوع مهمة", description = "عرض تفاصيل نوع مهمة")
    @ApiResponse(responseCode = "200", description = "تم العثور على نوع المهمة")
    public ResponseEntity<TaskTypeResponse> getTaskTypeById(
            @Parameter(description = "معرّف نوع المهمة") @PathVariable Long id) {
        log.info("REST: الحصول على نوع المهمة - ID: {}", id);
        TaskTypeResponse response = plantTaskService.getTaskTypeById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/task-types")
    @Tag(name = "Admin - Task Types")
    @Operation(summary = "جميع أنواع المهام", description = "عرض قائمة جميع أنواع المهام")
    @ApiResponse(responseCode = "200", description = "تم جلب أنواع المهام بنجاح")
    public ResponseEntity<List<TaskTypeResponse>> getAllTaskTypes() {
        log.info("REST: الحصول على جميع أنواع المهام");
        List<TaskTypeResponse> taskTypes = plantTaskService.getAllTaskTypes();
        return ResponseEntity.ok(taskTypes);
    }
    
    @DeleteMapping("/task-types/{id}")
    @Tag(name = "Admin - Task Types")
    @Operation(summary = "حذف نوع مهمة", description = "حذف نوع مهمة (يجب ألا يكون مرتبطاً بمهام)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "تم الحذف بنجاح"),
            @ApiResponse(responseCode = "400", description = "لا يمكن الحذف - مرتبط بمهام")
    })
    public ResponseEntity<Void> deleteTaskType(
            @Parameter(description = "معرّف نوع المهمة") @PathVariable Long id) {
        log.info("REST: حذف نوع المهمة - ID: {}", id);
        plantTaskService.deleteTaskType(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/task-types/initialize")
    @Tag(name = "Admin - Task Types")
    @Operation(summary = "تهيئة أنواع المهام", description = "إنشاء أنواع المهام الافتراضية (ري، تسميد، تقليم...)")
    @ApiResponse(responseCode = "201", description = "تم التهيئة بنجاح")
    public ResponseEntity<List<TaskTypeResponse>> initializeTaskTypes() {
        log.info("REST: تهيئة أنواع المهام الافتراضية");
        List<TaskTypeResponse> taskTypes = plantTaskService.initializeDefaultTaskTypes();
        return ResponseEntity.status(HttpStatus.CREATED).body(taskTypes);
    }
    
    // ===================== Plant Task Endpoints =====================
    
    @PostMapping("/plants/{plantId}/tasks")
    @Tag(name = "Admin - Plant Tasks", description = "إدارة مهام النباتات")
    @Operation(summary = "إضافة مهمة لنبتة", description = "ربط مهمة بنبتة مع تحديد الفترة والوصف")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "تم الإضافة بنجاح"),
            @ApiResponse(responseCode = "404", description = "النبتة أو نوع المهمة غير موجود")
    })
    public ResponseEntity<PlantTaskResponse> addTaskToPlant(
            @Parameter(description = "معرّف النبتة") @PathVariable Long plantId,
            @Valid @RequestBody PlantTaskRequest request) {
        log.info("REST: إضافة مهمة للنبتة - ID: {}", plantId);
        PlantTaskResponse response = plantTaskService.addTaskToPlant(plantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/plant-tasks/{taskId}")
    @Tag(name = "Admin - Plant Tasks")
    @Operation(summary = "تحديث مهمة نبتة", description = "تعديل بيانات مهمة نبتة")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم التحديث بنجاح"),
            @ApiResponse(responseCode = "404", description = "المهمة غير موجودة")
    })
    public ResponseEntity<PlantTaskResponse> updatePlantTask(
            @Parameter(description = "معرّف المهمة") @PathVariable Long taskId,
            @Valid @RequestBody PlantTaskRequest request) {
        log.info("REST: تحديث مهمة النبتة - ID: {}", taskId);
        PlantTaskResponse response = plantTaskService.updatePlantTask(taskId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/plant-tasks/{taskId}")
    @Tag(name = "Admin - Plant Tasks")
    @Operation(summary = "الحصول على مهمة", description = "عرض تفاصيل مهمة نبتة")
    @ApiResponse(responseCode = "200", description = "تم العثور على المهمة")
    public ResponseEntity<PlantTaskResponse> getPlantTaskById(
            @Parameter(description = "معرّف المهمة") @PathVariable Long taskId) {
        log.info("REST: الحصول على مهمة النبتة - ID: {}", taskId);
        PlantTaskResponse response = plantTaskService.getPlantTaskById(taskId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/plants/{plantId}/tasks")
    @Tag(name = "Admin - Plant Tasks")
    @Operation(summary = "مهام نبتة", description = "عرض جميع مهام نبتة معينة")
    @ApiResponse(responseCode = "200", description = "تم جلب المهام بنجاح")
    public ResponseEntity<List<PlantTaskResponse>> getTasksByPlant(
            @Parameter(description = "معرّف النبتة") @PathVariable Long plantId) {
        log.info("REST: الحصول على مهام النبتة - ID: {}", plantId);
        List<PlantTaskResponse> tasks = plantTaskService.getTasksByPlant(plantId);
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/task-types/{taskTypeId}/plant-tasks")
    @Tag(name = "Admin - Plant Tasks")
    @Operation(summary = "مهام حسب النوع", description = "عرض جميع مهام النباتات من نوع معين")
    @ApiResponse(responseCode = "200", description = "تم جلب المهام بنجاح")
    public ResponseEntity<List<PlantTaskResponse>> getTasksByType(
            @Parameter(description = "معرّف نوع المهمة") @PathVariable Long taskTypeId) {
        log.info("REST: الحصول على مهام النوع - ID: {}", taskTypeId);
        List<PlantTaskResponse> tasks = plantTaskService.getTasksByType(taskTypeId);
        return ResponseEntity.ok(tasks);
    }
    
    @DeleteMapping("/plant-tasks/{taskId}")
    @Tag(name = "Admin - Plant Tasks")
    @Operation(summary = "حذف مهمة", description = "حذف مهمة من نبتة")
    @ApiResponse(responseCode = "204", description = "تم الحذف بنجاح")
    public ResponseEntity<Void> deletePlantTask(
            @Parameter(description = "معرّف المهمة") @PathVariable Long taskId) {
        log.info("REST: حذف مهمة النبتة - ID: {}", taskId);
        plantTaskService.deletePlantTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
