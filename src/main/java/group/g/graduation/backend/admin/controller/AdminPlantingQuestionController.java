package group.g.graduation.backend.admin.controller;

import group.g.graduation.backend.admin.dto.PlantingQuestionRequest;
import group.g.graduation.backend.admin.dto.PlantingQuestionResponse;
import group.g.graduation.backend.admin.dto.QuestionOptionRequest;
import group.g.graduation.backend.admin.dto.QuestionOptionResponse;
import group.g.graduation.backend.admin.service.AdminPlantingQuestionService;
import group.g.graduation.backend.admin.service.AdminPlantingQuestionService.PlantingQuestionStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Controller for managing Planting Questions and Options
 * إدارة أسئلة إضافة المحصول وخياراتها
 */
@RestController
@RequestMapping("/api/admin/planting-questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Planting Questions", description = "APIs for managing planting questions and options for plant recommendation")
public class AdminPlantingQuestionController {
    
    private final AdminPlantingQuestionService questionService;
    
    // ============ PlantingQuestion Endpoints ============
    
    @GetMapping
    @Operation(summary = "Get all questions", description = "Get all planting questions without options")
    public ResponseEntity<List<PlantingQuestionResponse>> getAllQuestions() {
        return ResponseEntity.ok(questionService.getAllQuestions());
    }
    
    @GetMapping("/with-options")
    @Operation(summary = "Get all questions with options", description = "Get all planting questions with their options")
    public ResponseEntity<List<PlantingQuestionResponse>> getAllQuestionsWithOptions() {
        return ResponseEntity.ok(questionService.getAllQuestionsWithOptions());
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active questions", description = "Get only active planting questions")
    public ResponseEntity<List<PlantingQuestionResponse>> getActiveQuestions() {
        return ResponseEntity.ok(questionService.getActiveQuestions());
    }
    
    @GetMapping("/active/with-options")
    @Operation(summary = "Get active questions with options", description = "Get active questions with their options (for mobile app)")
    public ResponseEntity<List<PlantingQuestionResponse>> getActiveQuestionsWithOptions() {
        return ResponseEntity.ok(questionService.getActiveQuestionsWithOptions());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get question by ID", description = "Get a specific question with its options")
    public ResponseEntity<PlantingQuestionResponse> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getQuestionById(id));
    }
    
    @GetMapping("/key/{key}")
    @Operation(summary = "Get question by key", description = "Get a question by its key (site, light, container, water, soil)")
    public ResponseEntity<PlantingQuestionResponse> getQuestionByKey(@PathVariable String key) {
        return ResponseEntity.ok(questionService.getQuestionByKey(key));
    }
    
    @PostMapping
    @Operation(summary = "Create question", description = "Create a new planting question")
    public ResponseEntity<PlantingQuestionResponse> createQuestion(
            @Valid @RequestBody PlantingQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.createQuestion(request));
    }
    
    @PostMapping("/with-options")
    @Operation(summary = "Create question with options", description = "Create a new question with its options")
    public ResponseEntity<PlantingQuestionResponse> createQuestionWithOptions(
            @Valid @RequestBody PlantingQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.createQuestionWithOptions(request));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update question", description = "Update an existing planting question")
    public ResponseEntity<PlantingQuestionResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody PlantingQuestionRequest request) {
        return ResponseEntity.ok(questionService.updateQuestion(id, request));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete question", description = "Delete a planting question and its options")
    public ResponseEntity<Map<String, String>> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok(Map.of("message", "تم حذف السؤال بنجاح"));
    }
    
    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle question active", description = "Toggle the active status of a question")
    public ResponseEntity<PlantingQuestionResponse> toggleQuestionActive(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.toggleQuestionActive(id));
    }
    
    @PutMapping("/reorder")
    @Operation(summary = "Reorder questions", description = "Reorder questions by providing list of IDs in new order")
    public ResponseEntity<Map<String, String>> reorderQuestions(@RequestBody List<Long> questionIds) {
        questionService.reorderQuestions(questionIds);
        return ResponseEntity.ok(Map.of("message", "تم إعادة ترتيب الأسئلة بنجاح"));
    }
    
    // ============ QuestionOption Endpoints ============
    
    @GetMapping("/{questionId}/options")
    @Operation(summary = "Get options for question", description = "Get all options for a specific question")
    public ResponseEntity<List<QuestionOptionResponse>> getOptionsByQuestionId(
            @PathVariable Long questionId) {
        return ResponseEntity.ok(questionService.getOptionsByQuestionId(questionId));
    }
    
    @GetMapping("/options/{optionId}")
    @Operation(summary = "Get option by ID", description = "Get a specific option by ID")
    public ResponseEntity<QuestionOptionResponse> getOptionById(@PathVariable Long optionId) {
        return ResponseEntity.ok(questionService.getOptionById(optionId));
    }
    
    @PostMapping("/{questionId}/options")
    @Operation(summary = "Create option", description = "Create a new option for a question")
    public ResponseEntity<QuestionOptionResponse> createOption(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionOptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.createOption(questionId, request));
    }
    
    @PostMapping("/{questionId}/options/batch")
    @Operation(summary = "Create multiple options", description = "Create multiple options for a question at once")
    public ResponseEntity<List<QuestionOptionResponse>> createOptions(
            @PathVariable Long questionId,
            @Valid @RequestBody List<QuestionOptionRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.createOptions(questionId, requests));
    }
    
    @PutMapping("/options/{optionId}")
    @Operation(summary = "Update option", description = "Update an existing option")
    public ResponseEntity<QuestionOptionResponse> updateOption(
            @PathVariable Long optionId,
            @Valid @RequestBody QuestionOptionRequest request) {
        return ResponseEntity.ok(questionService.updateOption(optionId, request));
    }
    
    @DeleteMapping("/options/{optionId}")
    @Operation(summary = "Delete option", description = "Delete an option")
    public ResponseEntity<Map<String, String>> deleteOption(@PathVariable Long optionId) {
        questionService.deleteOption(optionId);
        return ResponseEntity.ok(Map.of("message", "تم حذف الخيار بنجاح"));
    }
    
    @PatchMapping("/options/{optionId}/toggle-active")
    @Operation(summary = "Toggle option active", description = "Toggle the active status of an option")
    public ResponseEntity<QuestionOptionResponse> toggleOptionActive(@PathVariable Long optionId) {
        return ResponseEntity.ok(questionService.toggleOptionActive(optionId));
    }
    
    @PutMapping("/{questionId}/options/reorder")
    @Operation(summary = "Reorder options", description = "Reorder options within a question")
    public ResponseEntity<Map<String, String>> reorderOptions(
            @PathVariable Long questionId,
            @RequestBody List<Long> optionIds) {
        questionService.reorderOptions(questionId, optionIds);
        return ResponseEntity.ok(Map.of("message", "تم إعادة ترتيب الخيارات بنجاح"));
    }
    
    // ============ Initialization & Stats ============
    
    @PostMapping("/initialize")
    @Operation(summary = "Initialize default questions", 
               description = "Initialize the 5 required questions + optional questions with their options")
    public ResponseEntity<Map<String, Object>> initializeDefaultQuestions() {
        boolean existed = questionService.hasDefaultQuestions();
        questionService.initializeDefaultQuestions();
        
        return ResponseEntity.ok(Map.of(
                "message", existed ? "الأسئلة الافتراضية موجودة مسبقاً" : "تم إنشاء الأسئلة الافتراضية بنجاح",
                "wasNew", !existed,
                "stats", questionService.getQuestionStats()
        ));
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get question statistics", description = "Get statistics about questions and options")
    public ResponseEntity<PlantingQuestionStats> getQuestionStats() {
        return ResponseEntity.ok(questionService.getQuestionStats());
    }
    
    @GetMapping("/check-defaults")
    @Operation(summary = "Check if defaults exist", description = "Check if default questions have been initialized")
    public ResponseEntity<Map<String, Boolean>> checkDefaultsExist() {
        return ResponseEntity.ok(Map.of("exists", questionService.hasDefaultQuestions()));
    }
}
