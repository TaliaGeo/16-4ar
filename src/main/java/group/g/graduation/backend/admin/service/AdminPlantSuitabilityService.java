package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Service Interface for PlantSuitability & PlantRecommendation - خدمة إدارة ملاءمة النباتات
 */
public interface AdminPlantSuitabilityService {
    
    // ===================== CRUD Operations for PlantSuitability =====================
    
    /**
     * Create a new plant suitability record
     */
    PlantSuitabilityResponse createSuitability(PlantSuitabilityRequest request);
    
    /**
     * Create multiple plant suitability records in bulk
     */
    List<PlantSuitabilityResponse> createBulkSuitabilities(BulkPlantSuitabilityRequest request);
    
    /**
     * Get plant suitability by ID
     */
    PlantSuitabilityResponse getSuitabilityById(Long id);
    
    /**
     * Get all suitabilities for a specific plant
     */
    List<PlantSuitabilityResponse> getSuitabilitiesByPlantId(Long plantId);
    
    /**
     * Get all suitabilities for a specific option
     */
    List<PlantSuitabilityResponse> getSuitabilitiesByOptionId(Long optionId);
    
    /**
     * Get suitability for a specific plant-option combination
     */
    PlantSuitabilityResponse getSuitabilityByPlantAndOption(Long plantId, Long optionId);
    
    /**
     * Get all suitabilities
     */
    List<PlantSuitabilityResponse> getAllSuitabilities();
    
    /**
     * Update plant suitability by ID
     */
    PlantSuitabilityResponse updateSuitability(Long id, PlantSuitabilityRequest request);
    
    /**
     * Delete plant suitability by ID
     */
    void deleteSuitability(Long id);
    
    /**
     * Delete all suitabilities for a specific plant
     */
    void deleteSuitabilitiesByPlantId(Long plantId);
    
    /**
     * Delete suitability by plant and option IDs
     */
    void deleteSuitabilityByPlantAndOption(Long plantId, Long optionId);
    
    // ===================== Statistics & Analytics =====================
    
    /**
     * Get suitability statistics for a plant
     */
    Map<String, Object> getPlantSuitabilityStats(Long plantId);
    
    /**
     * Get all plants with their average suitability scores
     */
    List<Map<String, Object>> getPlantsWithAverageScores();
    
    /**
     * Count suitabilities by score level for a plant
     */
    Map<String, Long> countSuitabilitiesByScoreLevel(Long plantId);
    
    // ===================== Plant Recommendation =====================
    
    /**
     * Get plant recommendations based on user answers to planting questions
     * This is the main recommendation algorithm
     */
    List<PlantRecommendationResponse> getRecommendations(PlantRecommendationRequest request);
    
    /**
     * Get top N recommended plants based on user answers
     */
    List<PlantRecommendationResponse> getTopRecommendations(PlantRecommendationRequest request, int limit);
    
    /**
     * Calculate match percentage for a specific plant based on user answers
     */
    PlantRecommendationResponse calculatePlantMatch(Long plantId, Map<String, Object> answers);
    
    // ===================== Setup & Initialization =====================
    
    /**
     * Initialize suitabilities for a plant with default scores
     * This creates suitability records for all options with default score of 50
     */
    List<PlantSuitabilityResponse> initializePlantSuitabilities(Long plantId);
    
    /**
     * Copy suitabilities from one plant to another
     */
    List<PlantSuitabilityResponse> copySuitabilitiesFromPlant(Long sourcePlantId, Long targetPlantId);
    
    /**
     * Update multiple suitabilities for a plant at once
     */
    List<PlantSuitabilityResponse> updatePlantSuitabilities(Long plantId, List<PlantSuitabilityRequest> requests);
}
