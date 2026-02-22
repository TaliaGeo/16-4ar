package group.g.graduation.backend.admin.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import group.g.graduation.backend.admin.dto.PlantingQuestionRequest;
import group.g.graduation.backend.admin.dto.PlantingQuestionResponse;
import group.g.graduation.backend.admin.dto.QuestionOptionRequest;
import group.g.graduation.backend.admin.dto.QuestionOptionResponse;
import group.g.graduation.backend.admin.mapper.PlantingQuestionMapper;
import group.g.graduation.backend.common.exception.DuplicateResourceException;
import group.g.graduation.backend.common.model.PlantingQuestion;
import group.g.graduation.backend.common.model.QuestionOption;
import group.g.graduation.backend.common.repository.PlantingQuestionRepository;
import group.g.graduation.backend.common.repository.QuestionOptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for Planting Question management
 * تنفيذ خدمة إدارة أسئلة الزراعة - الأسئلة الخمسة الأساسية + الاختيارية
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminPlantingQuestionServiceImpl implements AdminPlantingQuestionService {
    
    private final PlantingQuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final PlantingQuestionMapper mapper;
    
    // ============ PlantingQuestion Operations ============
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantingQuestionResponse> getAllQuestions() {
        log.debug("Fetching all planting questions");
        List<PlantingQuestion> questions = questionRepository.findAll();
        return mapper.toResponseList(questions);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantingQuestionResponse> getAllQuestionsWithOptions() {
        log.debug("Fetching all planting questions with options");
        List<PlantingQuestion> questions = questionRepository.findAllActiveWithOptions();
        return mapper.toResponseListWithOptions(questions);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantingQuestionResponse> getActiveQuestions() {
        log.debug("Fetching active planting questions");
        List<PlantingQuestion> questions = questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return mapper.toResponseList(questions);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlantingQuestionResponse> getActiveQuestionsWithOptions() {
        log.debug("Fetching active planting questions with options");
        List<PlantingQuestion> questions = questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return mapper.toResponseListWithOptions(questions);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlantingQuestionResponse getQuestionById(Long id) {
        log.debug("Fetching planting question by ID: {}", id);
        PlantingQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("السؤال غير موجود بالرقم: " + id));
        return mapper.toResponseWithOptions(question);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlantingQuestionResponse getQuestionByKey(String key) {
        log.debug("Fetching planting question by key: {}", key);
        PlantingQuestion question = questionRepository.findByQuestionKey(key)
                .orElseThrow(() -> new EntityNotFoundException("السؤال غير موجود بالمفتاح: " + key));
        return mapper.toResponseWithOptions(question);
    }
    
    @Override
    public PlantingQuestionResponse createQuestion(PlantingQuestionRequest request) {
        log.info("Creating new planting question: {}", request.getQuestionKey());
        
        // Validate required fields for creation
        validateQuestionForCreate(request);
        
        // Check if question key already exists
        if (questionRepository.findByQuestionKey(request.getQuestionKey()).isPresent()) {
            String suggestedKey = generateUniqueQuestionKey(request.getQuestionKey());
            String errorMsg = String.format(
                "مفتاح السؤال '%s' موجود مسبقاً. جرّب: '%s'",
                request.getQuestionKey(),
                suggestedKey
            );
            throw new DuplicateResourceException(errorMsg);
        }
        
        PlantingQuestion question = mapper.toEntity(request);
        
        // Set display order if not provided
        if (question.getDisplayOrder() == null) {
            long count = questionRepository.count();
            question.setDisplayOrder((int) count + 1);
        }
        
        PlantingQuestion saved = questionRepository.save(question);
        log.info("Created planting question with ID: {}", saved.getId());
        
        return mapper.toResponse(saved);
    }
    
    @Override
    public PlantingQuestionResponse createQuestionWithOptions(PlantingQuestionRequest request) {
        log.info("Creating new planting question with options: {}", request.getQuestionKey());
        
        // Validate required fields for creation
        validateQuestionForCreate(request);
        
        // Check if question key already exists
        if (questionRepository.findByQuestionKey(request.getQuestionKey()).isPresent()) {
            String suggestedKey = generateUniqueQuestionKey(request.getQuestionKey());
            String errorMsg = String.format(
                "مفتاح السؤال '%s' موجود مسبقاً. جرّب: '%s'",
                request.getQuestionKey(),
                suggestedKey
            );
            throw new DuplicateResourceException(errorMsg);
        }
        
        PlantingQuestion question = mapper.toEntity(request);
        
        // Set display order if not provided
        if (question.getDisplayOrder() == null) {
            long count = questionRepository.count();
            question.setDisplayOrder((int) count + 1);
        }
        
        // Add options if provided
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            int order = 1;
            for (QuestionOptionRequest optionRequest : request.getOptions()) {
                QuestionOption option = mapper.toOptionEntity(optionRequest);
                option.setQuestion(question);
                if (option.getDisplayOrder() == null) {
                    option.setDisplayOrder(order++);
                }
                question.getOptions().add(option);
            }
        }
        
        PlantingQuestion saved = questionRepository.save(question);
        log.info("Created planting question with ID: {} and {} options", saved.getId(), saved.getOptions().size());
        
        return mapper.toResponseWithOptions(saved);
    }
    
    @Override
    public PlantingQuestionResponse updateQuestion(Long id, PlantingQuestionRequest request) {
        log.info("Updating planting question ID: {}", id);
        
        PlantingQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("السؤال غير موجود بالرقم: " + id));
        
        // Check if key changed and new key already exists
        if (request.getQuestionKey() != null && !request.getQuestionKey().equals(question.getQuestionKey())) {
            if (questionRepository.findByQuestionKey(request.getQuestionKey()).isPresent()) {
                throw new IllegalArgumentException("مفتاح السؤال موجود مسبقاً: " + request.getQuestionKey());
            }
        }
        
        mapper.updateEntity(question, request);
        PlantingQuestion saved = questionRepository.save(question);
        log.info("Updated planting question ID: {}", id);
        
        return mapper.toResponseWithOptions(saved);
    }
    
    @Override
    public void deleteQuestion(Long id) {
        log.info("Deleting planting question ID: {}", id);
        
        if (!questionRepository.existsById(id)) {
            throw new EntityNotFoundException("السؤال غير موجود بالرقم: " + id);
        }
        
        questionRepository.deleteById(id);
        log.info("Deleted planting question ID: {}", id);
    }
    
    @Override
    public PlantingQuestionResponse toggleQuestionActive(Long id) {
        log.info("Toggling active status for question ID: {}", id);
        
        PlantingQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("السؤال غير موجود بالرقم: " + id));
        
        question.setIsActive(!question.getIsActive());
        PlantingQuestion saved = questionRepository.save(question);
        log.info("Question ID: {} active status changed to: {}", id, saved.getIsActive());
        
        return mapper.toResponse(saved);
    }
    
    @Override
    public void reorderQuestions(List<Long> questionIds) {
        log.info("Reordering {} questions", questionIds.size());
        
        int order = 1;
        for (Long questionId : questionIds) {
            PlantingQuestion question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new EntityNotFoundException("السؤال غير موجود بالرقم: " + questionId));
            question.setDisplayOrder(order++);
            questionRepository.save(question);
        }
        
        log.info("Reordered questions successfully");
    }
    
    // ============ QuestionOption Operations ============
    
    @Override
    @Transactional(readOnly = true)
    public List<QuestionOptionResponse> getOptionsByQuestionId(Long questionId) {
        log.debug("Fetching options for question ID: {}", questionId);
        
        if (!questionRepository.existsById(questionId)) {
            throw new EntityNotFoundException("السؤال غير موجود بالرقم: " + questionId);
        }
        
        List<QuestionOption> options = optionRepository.findByQuestionIdOrderByDisplayOrderAsc(questionId);
        return mapper.toOptionResponseList(options);
    }
    
    @Override
    @Transactional(readOnly = true)
    public QuestionOptionResponse getOptionById(Long optionId) {
        log.debug("Fetching option by ID: {}", optionId);
        QuestionOption option = optionRepository.findByIdWithQuestion(optionId)
                .orElseThrow(() -> new EntityNotFoundException("الخيار غير موجود بالرقم: " + optionId));
        return mapper.toOptionResponse(option);
    }
    
    @Override
    public QuestionOptionResponse createOption(Long questionId, QuestionOptionRequest request) {
        log.info("Creating new option for question ID: {}", questionId);
        
        // Validate required fields for creation
        validateOptionForCreate(request);
        
        PlantingQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("السؤال غير موجود بالرقم: " + questionId));
        
        // Check if option key already exists for this question
        if (optionRepository.findByQuestionIdAndOptionKey(questionId, request.getOptionKey()).isPresent()) {
            throw new IllegalArgumentException("مفتاح الخيار موجود مسبقاً لهذا السؤال: " + request.getOptionKey());
        }
        
        QuestionOption option = mapper.toOptionEntity(request);
        option.setQuestion(question);
        
        // Set display order if not provided
        if (option.getDisplayOrder() == null) {
            List<QuestionOption> existingOptions = optionRepository.findByQuestionIdOrderByDisplayOrderAsc(questionId);
            option.setDisplayOrder(existingOptions.size() + 1);
        }
        
        QuestionOption saved = optionRepository.save(option);
        log.info("Created option with ID: {} for question ID: {}", saved.getId(), questionId);
        
        return mapper.toOptionResponse(saved);
    }
    
    @Override
    public List<QuestionOptionResponse> createOptions(Long questionId, List<QuestionOptionRequest> requests) {
        log.info("Creating {} options for question ID: {}", requests.size(), questionId);
        
        PlantingQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("السؤال غير موجود بالرقم: " + questionId));
        
        List<QuestionOption> existingOptions = optionRepository.findByQuestionIdOrderByDisplayOrderAsc(questionId);
        int startOrder = existingOptions.size() + 1;
        
        List<QuestionOption> savedOptions = new ArrayList<>();
        
        for (QuestionOptionRequest request : requests) {
            // Validate each option
            validateOptionForCreate(request);
            
            // Check if option key already exists
            if (optionRepository.findByQuestionIdAndOptionKey(questionId, request.getOptionKey()).isPresent()) {
                log.warn("Skipping duplicate option key: {}", request.getOptionKey());
                continue;
            }
            
            QuestionOption option = mapper.toOptionEntity(request);
            option.setQuestion(question);
            
            if (option.getDisplayOrder() == null) {
                option.setDisplayOrder(startOrder++);
            }
            
            savedOptions.add(optionRepository.save(option));
        }
        
        log.info("Created {} options for question ID: {}", savedOptions.size(), questionId);
        
        return mapper.toOptionResponseList(savedOptions);
    }
    
    @Override
    public QuestionOptionResponse updateOption(Long optionId, QuestionOptionRequest request) {
        log.info("Updating option ID: {}", optionId);
        
        // Use query that fetches question to avoid LazyInitializationException
        QuestionOption option = optionRepository.findByIdWithQuestion(optionId)
                .orElseThrow(() -> new EntityNotFoundException("الخيار غير موجود بالرقم: " + optionId));
        
        // Check if key changed and new key already exists for this question
        if (request.getOptionKey() != null && !request.getOptionKey().equals(option.getOptionKey())) {
            Long questionId = option.getQuestion().getId();
            if (optionRepository.findByQuestionIdAndOptionKey(questionId, request.getOptionKey()).isPresent()) {
                throw new IllegalArgumentException("مفتاح الخيار موجود مسبقاً لهذا السؤال: " + request.getOptionKey());
            }
        }
        
        mapper.updateOptionEntity(option, request);
        QuestionOption saved = optionRepository.save(option);
        log.info("Updated option ID: {}", optionId);
        
        return mapper.toOptionResponse(saved);
    }
    
    @Override
    public void deleteOption(Long optionId) {
        log.info("Deleting option ID: {}", optionId);
        
        if (!optionRepository.existsById(optionId)) {
            throw new EntityNotFoundException("الخيار غير موجود بالرقم: " + optionId);
        }
        
        optionRepository.deleteById(optionId);
        log.info("Deleted option ID: {}", optionId);
    }
    
    @Override
    public QuestionOptionResponse toggleOptionActive(Long optionId) {
        log.info("Toggling active status for option ID: {}", optionId);
        
        QuestionOption option = optionRepository.findByIdWithQuestion(optionId)
                .orElseThrow(() -> new EntityNotFoundException("الخيار غير موجود بالرقم: " + optionId));
        
        option.setIsActive(!option.getIsActive());
        QuestionOption saved = optionRepository.save(option);
        log.info("Option ID: {} active status changed to: {}", optionId, saved.getIsActive());
        
        return mapper.toOptionResponse(saved);
    }
    
    @Override
    public void reorderOptions(Long questionId, List<Long> optionIds) {
        log.info("Reordering {} options for question ID: {}", optionIds.size(), questionId);
        
        if (!questionRepository.existsById(questionId)) {
            throw new EntityNotFoundException("السؤال غير موجود بالرقم: " + questionId);
        }
        
        int order = 1;
        for (Long optionId : optionIds) {
            QuestionOption option = optionRepository.findById(optionId)
                    .orElseThrow(() -> new EntityNotFoundException("الخيار غير موجود بالرقم: " + optionId));
            
            if (!option.getQuestion().getId().equals(questionId)) {
                throw new IllegalArgumentException("الخيار لا يتبع لهذا السؤال: " + optionId);
            }
            
            option.setDisplayOrder(order++);
            optionRepository.save(option);
        }
        
        log.info("Reordered options for question ID: {} successfully", questionId);
    }
    

    
    @Override
    @Transactional(readOnly = true)
    public PlantingQuestionStats getQuestionStats() {
        log.debug("Fetching question statistics");
        
        List<PlantingQuestion> allQuestions = questionRepository.findAll();
        List<QuestionOption> allOptions = optionRepository.findAll();
        
        long totalQuestions = allQuestions.size();
        long activeQuestions = allQuestions.stream().filter(PlantingQuestion::getIsActive).count();
        long requiredQuestions = allQuestions.stream().filter(PlantingQuestion::getIsRequired).count();
        long totalOptions = allOptions.size();
        long activeOptions = allOptions.stream().filter(QuestionOption::getIsActive).count();
        
        return new PlantingQuestionStats(
                totalQuestions,
                activeQuestions,
                requiredQuestions,
                totalOptions,
                activeOptions
        );
    }
    
    // ============ Validation Helpers ============
    
    /**
     * Validate question data for creation (required fields)
     */
    private void validateQuestionForCreate(PlantingQuestionRequest request) {
        if (request.getQuestionTextAr() == null || request.getQuestionTextAr().isBlank()) {
            throw new IllegalArgumentException("نص السؤال بالعربي مطلوب");
        }
        if (request.getQuestionKey() == null || request.getQuestionKey().isBlank()) {
            throw new IllegalArgumentException("مفتاح السؤال مطلوب");
        }
    }
    
    /**
     * Generate unique question key by appending number if duplicate exists
     * توليد مفتاح فريد للسؤال بإضافة رقم إذا كان مكرراً
     */
    private String generateUniqueQuestionKey(String baseKey) {
        String suggestedKey = baseKey;
        int counter = 2;
        
        // Try up to 100 variations
        while (counter <= 100 && questionRepository.findByQuestionKey(suggestedKey).isPresent()) {
            suggestedKey = baseKey + "_" + counter++;
        }
        
        return suggestedKey;
    }
    
    /**
     * Validate option data for creation (required fields)
     */
    private void validateOptionForCreate(QuestionOptionRequest request) {
        if (request.getOptionTextAr() == null || request.getOptionTextAr().isBlank()) {
            throw new IllegalArgumentException("نص الخيار بالعربي مطلوب");
        }
        if (request.getOptionKey() == null || request.getOptionKey().isBlank()) {
            throw new IllegalArgumentException("مفتاح الخيار مطلوب");
        }
    }
}
