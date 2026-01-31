package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.admin.mapper.PlantSuitabilityMapper;
import group.g.graduation.backend.common.model.*;
import group.g.graduation.backend.common.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Service Implementation for PlantSuitability & PlantRecommendation
 * خدمة إدارة ملاءمة النباتات والتوصيات
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminPlantSuitabilityServiceImpl implements AdminPlantSuitabilityService {
    
    private final PlantSuitabilityRepository suitabilityRepository;
    private final PlantRepository plantRepository;
    private final QuestionOptionRepository optionRepository;
    private final PlantingQuestionRepository questionRepository;
    private final PlantSuitabilityMapper mapper;
    
    // ===================== CRUD Operations =====================
    
    @Override
    public PlantSuitabilityResponse createSuitability(PlantSuitabilityRequest request) {
        log.info("Creating plant suitability: plantId={}, optionId={}", request.getPlantId(), request.getOptionId());
        
        validateRequest(request, true);
        
        // Check if already exists
        if (suitabilityRepository.findByPlantIdAndOptionId(request.getPlantId(), request.getOptionId()).isPresent()) {
            throw new IllegalArgumentException("Suitability already exists for this plant-option combination");
        }
        
        Plant plant = plantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new EntityNotFoundException("Plant not found with ID: " + request.getPlantId()));
        
        QuestionOption option = optionRepository.findByIdWithQuestion(request.getOptionId())
                .orElseThrow(() -> new EntityNotFoundException("Option not found with ID: " + request.getOptionId()));
        
        PlantSuitability suitability = mapper.toEntity(request);
        suitability.setPlant(plant);
        suitability.setOption(option);
        
        PlantSuitability saved = suitabilityRepository.save(suitability);
        log.info("Created plant suitability with ID: {}", saved.getId());
        
        return mapper.toResponse(saved);
    }
    
    @Override
    public List<PlantSuitabilityResponse> createBulkSuitabilities(BulkPlantSuitabilityRequest request) {
        log.info("Creating {} bulk suitabilities", request.getSuitabilities().size());
        
        List<PlantSuitabilityResponse> responses = new ArrayList<>();
        
        for (PlantSuitabilityRequest suitRequest : request.getSuitabilities()) {
            try {
                // Check if already exists - update instead of create
                Optional<PlantSuitability> existing = suitabilityRepository
                        .findByPlantIdAndOptionId(suitRequest.getPlantId(), suitRequest.getOptionId());
                
                if (existing.isPresent()) {
                    // Update existing
                    PlantSuitability entity = existing.get();
                    mapper.updateEntity(entity, suitRequest);
                    PlantSuitability saved = suitabilityRepository.save(entity);
                    responses.add(mapper.toResponse(saved));
                } else {
                    // Create new
                    responses.add(createSuitability(suitRequest));
                }
            } catch (Exception e) {
                log.warn("Failed to create/update suitability for plantId={}, optionId={}: {}", 
                        suitRequest.getPlantId(), suitRequest.getOptionId(), e.getMessage());
            }
        }
        
        log.info("Created/updated {} suitabilities", responses.size());
        return responses;
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlantSuitabilityResponse getSuitabilityById(Long id) {
        PlantSuitability suitability = suitabilityRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Suitability not found with ID: " + id));
        return mapper.toResponse(suitability);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantSuitabilityResponse> getSuitabilitiesByPlantId(Long plantId) {
        if (!plantRepository.existsById(plantId)) {
            throw new EntityNotFoundException("Plant not found with ID: " + plantId);
        }
        
        List<PlantSuitability> suitabilities = suitabilityRepository.findByPlantIdWithOptionDetails(plantId);
        return mapper.toResponseList(suitabilities);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantSuitabilityResponse> getSuitabilitiesByOptionId(Long optionId) {
        if (!optionRepository.existsById(optionId)) {
            throw new EntityNotFoundException("Option not found with ID: " + optionId);
        }
        
        List<PlantSuitability> suitabilities = suitabilityRepository.findByOptionIdWithDetails(optionId);
        return mapper.toResponseList(suitabilities);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlantSuitabilityResponse getSuitabilityByPlantAndOption(Long plantId, Long optionId) {
        PlantSuitability suitability = suitabilityRepository.findByPlantIdAndOptionIdWithDetails(plantId, optionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Suitability not found for plantId=" + plantId + " and optionId=" + optionId));
        return mapper.toResponse(suitability);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantSuitabilityResponse> getAllSuitabilities() {
        return mapper.toResponseList(suitabilityRepository.findAllWithDetails());
    }
    
    @Override
    public PlantSuitabilityResponse updateSuitability(Long id, PlantSuitabilityRequest request) {
        log.info("Updating suitability with ID: {}", id);
        
        // Use query with details to avoid LazyInitializationException
        PlantSuitability suitability = suitabilityRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Suitability not found with ID: " + id));
        
        // Update plant if provided
        if (request.getPlantId() != null && !request.getPlantId().equals(suitability.getPlant().getId())) {
            Plant plant = plantRepository.findById(request.getPlantId())
                    .orElseThrow(() -> new EntityNotFoundException("Plant not found with ID: " + request.getPlantId()));
            suitability.setPlant(plant);
        }
        
        // Update option if provided
        if (request.getOptionId() != null && !request.getOptionId().equals(suitability.getOption().getId())) {
            QuestionOption option = optionRepository.findByIdWithQuestion(request.getOptionId())
                    .orElseThrow(() -> new EntityNotFoundException("Option not found with ID: " + request.getOptionId()));
            suitability.setOption(option);
        }
        
        mapper.updateEntity(suitability, request);
        PlantSuitability saved = suitabilityRepository.save(suitability);
        
        log.info("Updated suitability with ID: {}", saved.getId());
        return mapper.toResponse(saved);
    }
    
    @Override
    public void deleteSuitability(Long id) {
        if (!suitabilityRepository.existsById(id)) {
            throw new EntityNotFoundException("Suitability not found with ID: " + id);
        }
        suitabilityRepository.deleteById(id);
        log.info("Deleted suitability with ID: {}", id);
    }
    
    @Override
    public void deleteSuitabilitiesByPlantId(Long plantId) {
        if (!plantRepository.existsById(plantId)) {
            throw new EntityNotFoundException("Plant not found with ID: " + plantId);
        }
        suitabilityRepository.deleteByPlantId(plantId);
        log.info("Deleted suitabilities for plantId: {}", plantId);
    }
    
    @Override
    public void deleteSuitabilityByPlantAndOption(Long plantId, Long optionId) {
        if (!suitabilityRepository.findByPlantIdAndOptionId(plantId, optionId).isPresent()) {
            throw new EntityNotFoundException(
                    "Suitability not found for plantId=" + plantId + " and optionId=" + optionId);
        }
        suitabilityRepository.deleteByPlantIdAndOptionId(plantId, optionId);
        log.info("Deleted suitability for plantId={}, optionId={}", plantId, optionId);
    }
    
    // ===================== Statistics & Analytics =====================
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPlantSuitabilityStats(Long plantId) {
        List<PlantSuitability> suitabilities = suitabilityRepository.findByPlantId(plantId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("plantId", plantId);
        stats.put("totalSuitabilities", suitabilities.size());
        
        if (!suitabilities.isEmpty()) {
            double avgScore = suitabilities.stream()
                    .mapToInt(PlantSuitability::getScore)
                    .average()
                    .orElse(0);
            
            int maxScore = suitabilities.stream()
                    .mapToInt(PlantSuitability::getScore)
                    .max()
                    .orElse(0);
            
            int minScore = suitabilities.stream()
                    .mapToInt(PlantSuitability::getScore)
                    .min()
                    .orElse(0);
            
            stats.put("averageScore", Math.round(avgScore * 100.0) / 100.0);
            stats.put("maxScore", maxScore);
            stats.put("minScore", minScore);
            stats.put("overallLevel", mapper.getScoreLevel((int) avgScore));
            stats.put("overallLevelAr", mapper.getScoreLevelAr((int) avgScore));
        }
        
        return stats;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPlantsWithAverageScores() {
        List<Plant> plants = plantRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Plant plant : plants) {
            List<PlantSuitability> suitabilities = suitabilityRepository.findByPlantId(plant.getId());
            
            if (!suitabilities.isEmpty()) {
                double avgScore = suitabilities.stream()
                        .mapToInt(PlantSuitability::getScore)
                        .average()
                        .orElse(0);
                
                Map<String, Object> plantScore = new HashMap<>();
                plantScore.put("plantId", plant.getId());
                plantScore.put("nameAr", plant.getNameAr());
                plantScore.put("nameEn", plant.getNameEn());
                plantScore.put("averageScore", Math.round(avgScore * 100.0) / 100.0);
                plantScore.put("suitabilityCount", suitabilities.size());
                plantScore.put("level", mapper.getScoreLevel((int) avgScore));
                
                result.add(plantScore);
            }
        }
        
        // Sort by average score descending
        result.sort((a, b) -> Double.compare(
                (Double) b.get("averageScore"),
                (Double) a.get("averageScore")));
        
        return result;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countSuitabilitiesByScoreLevel(Long plantId) {
        List<PlantSuitability> suitabilities = suitabilityRepository.findByPlantId(plantId);
        
        return suitabilities.stream()
                .collect(Collectors.groupingBy(
                        s -> mapper.getScoreLevel(s.getScore()),
                        Collectors.counting()));
    }
    
    // ===================== Plant Recommendation Algorithm =====================
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantRecommendationResponse> getRecommendations(PlantRecommendationRequest request) {
        log.info("Getting plant recommendations for {} answers", request.getAnswers().size());
        
        // 1. Convert user answers to option IDs
        List<Long> selectedOptionIds = resolveOptionIds(request.getAnswers());
        
        if (selectedOptionIds.isEmpty()) {
            log.warn("No valid options found for the provided answers");
            return Collections.emptyList();
        }
        
        log.info("Resolved {} option IDs from answers", selectedOptionIds.size());
        
        // 2. Get plant scores from database
        List<Object[]> plantScores = suitabilityRepository.findPlantScoresByOptions(selectedOptionIds);
        
        // 3. Calculate max possible score
        int maxPossibleScore = selectedOptionIds.size() * 100;
        
        // 4. Build recommendation responses
        List<PlantRecommendationResponse> recommendations = new ArrayList<>();
        
        for (Object[] row : plantScores) {
            Long plantId = (Long) row[0];
            Long totalScore = (Long) row[1];
            
            Plant plant = plantRepository.findById(plantId).orElse(null);
            if (plant == null) continue;
            
            // Calculate match percentage
            double matchPercentage = maxPossibleScore > 0 
                    ? (totalScore.doubleValue() / maxPossibleScore) * 100 
                    : 0;
            
            // Apply minimum match filter
            if (request.getMinMatchPercentage() != null && matchPercentage < request.getMinMatchPercentage()) {
                continue;
            }
            
            // Get detailed question matches
            List<PlantRecommendationResponse.QuestionMatchDetail> questionMatches = 
                    getQuestionMatchDetails(plantId, selectedOptionIds);
            
            // Get adjustment tips
            List<String> adjustmentTips = getAdjustmentTips(plantId, selectedOptionIds);
            
            PlantRecommendationResponse recommendation = PlantRecommendationResponse.builder()
                    .plantId(plantId)
                    .nameAr(plant.getNameAr())
                    .nameEn(plant.getNameEn())
                    .shortDescriptionAr(plant.getShortDescriptionAr())
                    .shortDescriptionEn(plant.getShortDescriptionEn())
                    .totalScore(totalScore.intValue())
                    .maxPossibleScore(maxPossibleScore)
                    .matchPercentage(Math.round(matchPercentage * 100.0) / 100.0)
                    .matchLevel(getMatchLevel(matchPercentage))
                    .matchLevelAr(getMatchLevelAr(matchPercentage))
                    .questionMatches(questionMatches)
                    .adjustmentTips(adjustmentTips)
                    .build();
            
            recommendations.add(recommendation);
        }
        
        // Apply limit if specified
        int limit = request.getLimit() != null ? request.getLimit() : recommendations.size();
        
        return recommendations.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantRecommendationResponse> getTopRecommendations(PlantRecommendationRequest request, int limit) {
        request.setLimit(limit);
        return getRecommendations(request);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlantRecommendationResponse calculatePlantMatch(Long plantId, Map<String, Object> answers) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new EntityNotFoundException("Plant not found with ID: " + plantId));
        
        List<Long> selectedOptionIds = resolveOptionIds(answers);
        
        if (selectedOptionIds.isEmpty()) {
            throw new IllegalArgumentException("No valid options found for the provided answers");
        }
        
        // Get plant's suitabilities for selected options
        List<PlantSuitability> suitabilities = suitabilityRepository.findByPlantIdWithOptionDetails(plantId);
        
        int totalScore = 0;
        int matchedOptions = 0;
        
        for (PlantSuitability suit : suitabilities) {
            if (selectedOptionIds.contains(suit.getOption().getId())) {
                totalScore += suit.getScore();
                matchedOptions++;
            }
        }
        
        int maxPossibleScore = selectedOptionIds.size() * 100;
        double matchPercentage = maxPossibleScore > 0 
                ? (totalScore * 100.0) / maxPossibleScore 
                : 0;
        
        List<PlantRecommendationResponse.QuestionMatchDetail> questionMatches = 
                getQuestionMatchDetails(plantId, selectedOptionIds);
        
        List<String> adjustmentTips = getAdjustmentTips(plantId, selectedOptionIds);
        
        return PlantRecommendationResponse.builder()
                .plantId(plantId)
                .nameAr(plant.getNameAr())
                .nameEn(plant.getNameEn())
                .shortDescriptionAr(plant.getShortDescriptionAr())
                .shortDescriptionEn(plant.getShortDescriptionEn())
                .totalScore(totalScore)
                .maxPossibleScore(maxPossibleScore)
                .matchPercentage(Math.round(matchPercentage * 100.0) / 100.0)
                .matchLevel(getMatchLevel(matchPercentage))
                .matchLevelAr(getMatchLevelAr(matchPercentage))
                .questionMatches(questionMatches)
                .adjustmentTips(adjustmentTips)
                .build();
    }
    
    // ===================== Setup & Initialization =====================
    
    @Override
    public List<PlantSuitabilityResponse> initializePlantSuitabilities(Long plantId) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new EntityNotFoundException("Plant not found with ID: " + plantId));
        
        log.info("Initializing suitabilities for plant: {}", plant.getNameEn());
        
        // Get all options with questions loaded to avoid LazyInitializationException
        List<QuestionOption> allOptions = optionRepository.findAllWithQuestion();
        List<PlantSuitabilityResponse> responses = new ArrayList<>();
        
        for (QuestionOption option : allOptions) {
            // Skip if already exists
            if (suitabilityRepository.findByPlantIdAndOptionId(plantId, option.getId()).isPresent()) {
                continue;
            }
            
            PlantSuitability suitability = new PlantSuitability();
            suitability.setPlant(plant);
            suitability.setOption(option);
            suitability.setScore(50); // Default score
            
            PlantSuitability saved = suitabilityRepository.save(suitability);
            responses.add(mapper.toResponse(saved));
        }
        
        log.info("Initialized {} suitabilities for plantId: {}", responses.size(), plantId);
        return responses;
    }
    
    @Override
    public List<PlantSuitabilityResponse> copySuitabilitiesFromPlant(Long sourcePlantId, Long targetPlantId) {
        if (!plantRepository.existsById(sourcePlantId)) {
            throw new EntityNotFoundException("Source plant not found with ID: " + sourcePlantId);
        }
        
        Plant targetPlant = plantRepository.findById(targetPlantId)
                .orElseThrow(() -> new EntityNotFoundException("Target plant not found with ID: " + targetPlantId));
        
        // Use the method with option details to avoid LazyInitializationException
        List<PlantSuitability> sourceSuitabilities = suitabilityRepository.findByPlantIdWithOptionDetails(sourcePlantId);
        List<PlantSuitabilityResponse> responses = new ArrayList<>();
        
        for (PlantSuitability source : sourceSuitabilities) {
            // Skip if already exists
            if (suitabilityRepository.findByPlantIdAndOptionId(targetPlantId, source.getOption().getId()).isPresent()) {
                continue;
            }
            
            PlantSuitability newSuitability = new PlantSuitability();
            newSuitability.setPlant(targetPlant);
            newSuitability.setOption(source.getOption());
            newSuitability.setScore(source.getScore());
            newSuitability.setAdjustmentTipAr(source.getAdjustmentTipAr());
            newSuitability.setAdjustmentTipEn(source.getAdjustmentTipEn());
            
            PlantSuitability saved = suitabilityRepository.save(newSuitability);
            responses.add(mapper.toResponse(saved));
        }
        
        log.info("Copied {} suitabilities from plantId {} to plantId {}", 
                responses.size(), sourcePlantId, targetPlantId);
        return responses;
    }
    
    @Override
    public List<PlantSuitabilityResponse> updatePlantSuitabilities(Long plantId, List<PlantSuitabilityRequest> requests) {
        if (!plantRepository.existsById(plantId)) {
            throw new EntityNotFoundException("Plant not found with ID: " + plantId);
        }
        
        List<PlantSuitabilityResponse> responses = new ArrayList<>();
        
        for (PlantSuitabilityRequest request : requests) {
            if (request.getOptionId() == null) continue;
            
            // Use query with details to avoid LazyInitializationException
            Optional<PlantSuitability> existingOpt = suitabilityRepository
                    .findByPlantIdAndOptionIdWithDetails(plantId, request.getOptionId());
            
            if (existingOpt.isPresent()) {
                // Update existing
                PlantSuitability existing = existingOpt.get();
                mapper.updateEntity(existing, request);
                PlantSuitability saved = suitabilityRepository.save(existing);
                responses.add(mapper.toResponse(saved));
            } else {
                // Create new
                request.setPlantId(plantId);
                responses.add(createSuitability(request));
            }
        }
        
        log.info("Updated {} suitabilities for plantId: {}", responses.size(), plantId);
        return responses;
    }
    
    // ===================== Helper Methods =====================
    
    /**
     * Resolve option IDs from user answers map
     * Supports both single selection (String) and multi-selection (List<String>)
     */
    private List<Long> resolveOptionIds(Map<String, Object> answers) {
        List<Long> optionIds = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : answers.entrySet()) {
            String questionKey = entry.getKey();
            Object answerValue = entry.getValue();
            
            // Find the question
            Optional<PlantingQuestion> questionOpt = questionRepository.findByQuestionKeyWithOptions(questionKey);
            if (questionOpt.isEmpty()) {
                log.warn("Question not found for key: {}", questionKey);
                continue;
            }
            
            PlantingQuestion question = questionOpt.get();
            
            // Handle single selection
            if (answerValue instanceof String) {
                String optionKey = (String) answerValue;
                findOptionId(question, optionKey).ifPresent(optionIds::add);
            }
            // Handle multi-selection (for prefs questions)
            else if (answerValue instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> optionKeys = (List<String>) answerValue;
                for (String optionKey : optionKeys) {
                    findOptionId(question, optionKey).ifPresent(optionIds::add);
                }
            }
        }
        
        return optionIds;
    }
    
    /**
     * Find option ID by question and option key
     */
    private Optional<Long> findOptionId(PlantingQuestion question, String optionKey) {
        return question.getOptions().stream()
                .filter(o -> o.getOptionKey().equals(optionKey))
                .map(QuestionOption::getId)
                .findFirst();
    }
    
    /**
     * Get detailed question match information for a plant
     */
    private List<PlantRecommendationResponse.QuestionMatchDetail> getQuestionMatchDetails(
            Long plantId, List<Long> selectedOptionIds) {
        
        List<PlantSuitability> suitabilities = suitabilityRepository.findByPlantIdWithOptionDetails(plantId);
        List<PlantRecommendationResponse.QuestionMatchDetail> details = new ArrayList<>();
        
        // Group by question
        Map<Long, List<PlantSuitability>> byQuestion = suitabilities.stream()
                .filter(s -> selectedOptionIds.contains(s.getOption().getId()))
                .collect(Collectors.groupingBy(s -> s.getOption().getQuestion().getId()));
        
        for (Map.Entry<Long, List<PlantSuitability>> entry : byQuestion.entrySet()) {
            List<PlantSuitability> questionSuits = entry.getValue();
            if (questionSuits.isEmpty()) continue;
            
            PlantSuitability first = questionSuits.get(0);
            PlantingQuestion question = first.getOption().getQuestion();
            
            // Calculate average score for this question
            double avgScore = questionSuits.stream()
                    .mapToInt(PlantSuitability::getScore)
                    .average()
                    .orElse(0);
            
            String selectedOptions = questionSuits.stream()
                    .map(s -> s.getOption().getOptionTextAr())
                    .collect(Collectors.joining(", "));
            
            PlantRecommendationResponse.QuestionMatchDetail detail = PlantRecommendationResponse.QuestionMatchDetail.builder()
                    .questionId(question.getId())
                    .questionKey(question.getQuestionKey())
                    .questionTextAr(question.getQuestionTextAr())
                    .questionTextEn(question.getQuestionTextEn())
                    .selectedOptionText(selectedOptions)
                    .score((int) avgScore)
                    .scoreLevel(mapper.getScoreLevel((int) avgScore))
                    .build();
            
            details.add(detail);
        }
        
        return details;
    }
    
    /**
     * Get adjustment tips for low-scoring options
     */
    private List<String> getAdjustmentTips(Long plantId, List<Long> selectedOptionIds) {
        List<PlantSuitability> suitabilities = suitabilityRepository.findByPlantIdWithOptionDetails(plantId);
        
        return suitabilities.stream()
                .filter(s -> selectedOptionIds.contains(s.getOption().getId()))
                .filter(s -> s.getScore() < 60) // Only for low scores
                .filter(s -> s.getAdjustmentTipAr() != null && !s.getAdjustmentTipAr().isEmpty())
                .map(PlantSuitability::getAdjustmentTipAr)
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * Get match level text
     */
    private String getMatchLevel(double percentage) {
        if (percentage >= 80) return "excellent";
        if (percentage >= 60) return "good";
        if (percentage >= 40) return "fair";
        return "poor";
    }
    
    /**
     * Get match level in Arabic
     */
    private String getMatchLevelAr(double percentage) {
        if (percentage >= 80) return "ممتاز";
        if (percentage >= 60) return "جيد";
        if (percentage >= 40) return "مقبول";
        return "ضعيف";
    }
    
    /**
     * Validate request for creating suitability
     */
    private void validateRequest(PlantSuitabilityRequest request, boolean isCreate) {
        if (isCreate) {
            if (request.getPlantId() == null) {
                throw new IllegalArgumentException("Plant ID is required");
            }
            if (request.getOptionId() == null) {
                throw new IllegalArgumentException("Option ID is required");
            }
            if (request.getScore() == null) {
                throw new IllegalArgumentException("Score is required");
            }
        }
        
        if (request.getScore() != null && (request.getScore() < 0 || request.getScore() > 100)) {
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }
    }
}
