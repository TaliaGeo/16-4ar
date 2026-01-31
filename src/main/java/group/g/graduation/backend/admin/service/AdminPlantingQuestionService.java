package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.PlantingQuestionRequest;
import group.g.graduation.backend.admin.dto.PlantingQuestionResponse;
import group.g.graduation.backend.admin.dto.QuestionOptionRequest;
import group.g.graduation.backend.admin.dto.QuestionOptionResponse;

import java.util.List;

/**
 * Service interface for Planting Question management - خدمة إدارة أسئلة الزراعة
 */
public interface AdminPlantingQuestionService {
    
    // ============ PlantingQuestion Operations ============
    
    /**
     * Get all planting questions
     * @return List of all questions
     */
    List<PlantingQuestionResponse> getAllQuestions();
    
    /**
     * Get all planting questions with their options
     * @return List of all questions with options
     */
    List<PlantingQuestionResponse> getAllQuestionsWithOptions();
    
    /**
     * Get only active planting questions
     * @return List of active questions
     */
    List<PlantingQuestionResponse> getActiveQuestions();
    
    /**
     * Get only active planting questions with their options (for mobile app)
     * @return List of active questions with options
     */
    List<PlantingQuestionResponse> getActiveQuestionsWithOptions();
    
    /**
     * Get a specific planting question by ID
     * @param id Question ID
     * @return Question details with options
     */
    PlantingQuestionResponse getQuestionById(Long id);
    
    /**
     * Get a specific planting question by key
     * @param key Question key (site, light, container, water, soil)
     * @return Question details with options
     */
    PlantingQuestionResponse getQuestionByKey(String key);
    
    /**
     * Create a new planting question
     * @param request Question data
     * @return Created question
     */
    PlantingQuestionResponse createQuestion(PlantingQuestionRequest request);
    
    /**
     * Create a new planting question with options
     * @param request Question data with options
     * @return Created question with options
     */
    PlantingQuestionResponse createQuestionWithOptions(PlantingQuestionRequest request);
    
    /**
     * Update an existing planting question
     * @param id Question ID
     * @param request Updated question data
     * @return Updated question
     */
    PlantingQuestionResponse updateQuestion(Long id, PlantingQuestionRequest request);
    
    /**
     * Delete a planting question
     * @param id Question ID
     */
    void deleteQuestion(Long id);
    
    /**
     * Toggle question active status
     * @param id Question ID
     * @return Updated question
     */
    PlantingQuestionResponse toggleQuestionActive(Long id);
    
    /**
     * Reorder questions
     * @param questionIds List of question IDs in new order
     */
    void reorderQuestions(List<Long> questionIds);
    
    // ============ QuestionOption Operations ============
    
    /**
     * Get all options for a specific question
     * @param questionId Question ID
     * @return List of options
     */
    List<QuestionOptionResponse> getOptionsByQuestionId(Long questionId);
    
    /**
     * Get a specific option by ID
     * @param optionId Option ID
     * @return Option details
     */
    QuestionOptionResponse getOptionById(Long optionId);
    
    /**
     * Create a new option for a question
     * @param questionId Question ID
     * @param request Option data
     * @return Created option
     */
    QuestionOptionResponse createOption(Long questionId, QuestionOptionRequest request);
    
    /**
     * Create multiple options for a question
     * @param questionId Question ID
     * @param requests List of option data
     * @return List of created options
     */
    List<QuestionOptionResponse> createOptions(Long questionId, List<QuestionOptionRequest> requests);
    
    /**
     * Update an existing option
     * @param optionId Option ID
     * @param request Updated option data
     * @return Updated option
     */
    QuestionOptionResponse updateOption(Long optionId, QuestionOptionRequest request);
    
    /**
     * Delete an option
     * @param optionId Option ID
     */
    void deleteOption(Long optionId);
    
    /**
     * Toggle option active status
     * @param optionId Option ID
     * @return Updated option
     */
    QuestionOptionResponse toggleOptionActive(Long optionId);
    
    /**
     * Reorder options within a question
     * @param questionId Question ID
     * @param optionIds List of option IDs in new order
     */
    void reorderOptions(Long questionId, List<Long> optionIds);
    
    // ============ Initialization ============
    
    /**
     * Initialize default questions and options (5 required + optional)
     * Based on the HTML mockup design
     */
    void initializeDefaultQuestions();
    
    /**
     * Check if default questions exist
     * @return true if default questions exist
     */
    boolean hasDefaultQuestions();
    
    /**
     * Get statistics about questions
     * @return Statistics map
     */
    PlantingQuestionStats getQuestionStats();
    
    // ============ Stats Inner Class ============
    
    record PlantingQuestionStats(
            long totalQuestions,
            long activeQuestions,
            long requiredQuestions,
            long totalOptions,
            long activeOptions
    ) {}
}
