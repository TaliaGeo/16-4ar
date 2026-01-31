package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.PlantingQuestionRequest;
import group.g.graduation.backend.admin.dto.PlantingQuestionResponse;
import group.g.graduation.backend.admin.dto.QuestionOptionRequest;
import group.g.graduation.backend.admin.dto.QuestionOptionResponse;
import group.g.graduation.backend.admin.mapper.PlantingQuestionMapper;
import group.g.graduation.backend.common.model.PlantingQuestion;
import group.g.graduation.backend.common.model.QuestionOption;
import group.g.graduation.backend.common.repository.PlantingQuestionRepository;
import group.g.graduation.backend.common.repository.QuestionOptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
            throw new IllegalArgumentException("مفتاح السؤال موجود مسبقاً: " + request.getQuestionKey());
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
            throw new IllegalArgumentException("مفتاح السؤال موجود مسبقاً: " + request.getQuestionKey());
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
    
    // ============ Initialization ============
    
    @Override
    public void initializeDefaultQuestions() {
        log.info("Initializing default planting questions...");
        
        if (hasDefaultQuestions()) {
            log.info("Default questions already exist, skipping initialization");
            return;
        }
        
        // ===== السؤال 1: أين ستزرع؟ (site) =====
        createQuestionWithOptionsInternal(
                "أين ستزرع؟",
                "Where will you plant?",
                "site",
                true, false, 1,
                List.of(
                        new OptionData("داخل المنزل", "Indoor", "indoor", 1),
                        new OptionData("شرفة أو بلكونة", "Balcony", "balcony", 2),
                        new OptionData("سطح أو تراس", "Rooftop", "rooftop", 3),
                        new OptionData("حديقة أو أرض", "Garden/Ground", "garden", 4),
                        new OptionData("مش متأكد", "Not sure", "unknown", 5)
                )
        );
        
        // ===== السؤال 2: كم ضوء يصل للمكان؟ (light) =====
        createQuestionWithOptionsInternal(
                "كم ضوء يصل للمكان؟",
                "How much light does the place get?",
                "light",
                true, false, 2,
                List.of(
                        new OptionData("شمس مباشرة (6+ ساعات)", "Full sun (6+ hours)", "full_sun", 1),
                        new OptionData("شمس جزئية (3-6 ساعات)", "Partial sun (3-6 hours)", "partial_sun", 2),
                        new OptionData("ضوء غير مباشر ساطع", "Bright indirect light", "bright_indirect", 3),
                        new OptionData("ضوء ضعيف", "Low light", "low_light", 4),
                        new OptionData("مش متأكد", "Not sure", "unknown", 5)
                )
        );
        
        // ===== السؤال 3: ستزرع في ماذا؟ (container) =====
        createQuestionWithOptionsInternal(
                "ستزرع في ماذا؟",
                "What will you plant in?",
                "container",
                true, false, 3,
                List.of(
                        new OptionData("أصيص صغير (<20 سم)", "Small pot (<20cm)", "small_pot", 1),
                        new OptionData("أصيص متوسط (20-35 سم)", "Medium pot (20-35cm)", "medium_pot", 2),
                        new OptionData("أصيص كبير (>35 سم)", "Large pot (>35cm)", "large_pot", 3),
                        new OptionData("حوض أو أرض", "Bed or ground", "bed_or_ground", 4),
                        new OptionData("مش متأكد", "Not sure", "unknown", 5)
                )
        );
        
        // ===== السؤال 4: كم مرة تقدر تروي؟ (water) =====
        createQuestionWithOptionsInternal(
                "كم مرة تقدر تروي؟",
                "How often can you water?",
                "water",
                true, false, 4,
                List.of(
                        new OptionData("يومياً", "Daily", "daily", 1),
                        new OptionData("كل 2-3 أيام", "Every 2-3 days", "every_2_3", 2),
                        new OptionData("مرة أسبوعياً", "Weekly", "weekly", 3),
                        new OptionData("غير منتظم", "Irregular", "irregular", 4),
                        new OptionData("مش متأكد", "Not sure", "unknown", 5)
                )
        );
        
        // ===== السؤال 5: نوع التربة (soil) =====
        createQuestionWithOptionsInternal(
                "ما نوع التربة المتاحة؟",
                "What type of soil is available?",
                "soil",
                true, false, 5,
                List.of(
                        new OptionData("تربة أصص جاهزة", "Potting mix", "potting_mix", 1),
                        new OptionData("تربة حديقة عادية", "Garden soil", "garden_soil", 2),
                        new OptionData("تربة رملية", "Sandy soil", "sandy", 3),
                        new OptionData("تربة طينية", "Clay soil", "clay", 4),
                        new OptionData("خليط كمبوست", "Compost mix", "compost_mix", 5),
                        new OptionData("مش متأكد", "Not sure", "unknown", 6)
                )
        );
        
        // ===== الأسئلة الاختيارية =====
        
        // السؤال 6: نوع التصريف (drainage) - اختياري
        createQuestionWithOptionsInternal(
                "كيف حال التصريف في المكان؟",
                "How is the drainage in the area?",
                "drainage",
                false, false, 6,
                List.of(
                        new OptionData("تصريف جيد", "Good drainage", "good", 1),
                        new OptionData("تصريف متوسط", "Average drainage", "average", 2),
                        new OptionData("تصريف ضعيف (ماء واقف)", "Poor drainage", "poor", 3),
                        new OptionData("مش متأكد", "Not sure", "unknown", 4)
                )
        );
        
        // السؤال 7: فتحات تصريف الأصيص (holes) - اختياري
        createQuestionWithOptionsInternal(
                "هل للأصيص فتحات تصريف؟",
                "Does the pot have drainage holes?",
                "holes",
                false, false, 7,
                List.of(
                        new OptionData("نعم، فتحات كافية", "Yes, enough holes", "yes", 1),
                        new OptionData("فتحة واحدة صغيرة", "One small hole", "partial", 2),
                        new OptionData("لا يوجد فتحات", "No holes", "no", 3),
                        new OptionData("سأزرع في الأرض", "Planting in ground", "ground", 4)
                )
        );
        
        // السؤال 8: التعرض للرياح (wind) - اختياري (يظهر لو اختار شرفة/سطح/حديقة)
        createQuestionWithOptionsInternal(
                "هل المكان معرض للرياح؟",
                "Is the area exposed to wind?",
                "wind",
                false, false, 8,
                List.of(
                        new OptionData("محمي من الرياح", "Sheltered from wind", "sheltered", 1),
                        new OptionData("رياح خفيفة أحياناً", "Light wind sometimes", "light", 2),
                        new OptionData("رياح قوية متكررة", "Strong frequent wind", "strong", 3),
                        new OptionData("مش متأكد", "Not sure", "unknown", 4)
                )
        );
        
        // السؤال 9: إضاءة نمو (growlight) - اختياري (يظهر لو اختار داخل المنزل أو ضوء ضعيف)
        createQuestionWithOptionsInternal(
                "هل لديك إضاءة نمو صناعية؟",
                "Do you have grow lights?",
                "growlight",
                false, false, 9,
                List.of(
                        new OptionData("نعم، إضاءة LED للنباتات", "Yes, LED grow lights", "yes_led", 1),
                        new OptionData("نعم، إضاءة عادية قوية", "Yes, strong regular lights", "yes_regular", 2),
                        new OptionData("لا يوجد", "No", "no", 3),
                        new OptionData("سأشتري إذا لزم", "Will buy if needed", "will_buy", 4)
                )
        );
        
        // السؤال 10: التفضيلات (prefs) - اختياري، متعدد الاختيارات
        createQuestionWithOptionsInternal(
                "ما تفضيلاتك للنباتات؟",
                "What are your plant preferences?",
                "prefs",
                false, true, 10,  // allowMultiple = true
                List.of(
                        new OptionData("سهلة للمبتدئين", "Easy for beginners", "beginner", 1),
                        new OptionData("للشاي والمشروبات", "For tea and drinks", "tea", 2),
                        new OptionData("للطبخ والتتبيل", "For cooking", "cooking", 3),
                        new OptionData("عطرية", "Aromatic", "aromatic", 4),
                        new OptionData("طبية وعلاجية", "Medicinal", "medicinal", 5),
                        new OptionData("قليلة استهلاك الماء", "Low water needs", "low_water", 6),
                        new OptionData("سريعة النمو", "Fast growing", "fast_growing", 7),
                        new OptionData("مزهرة", "Flowering", "flowering", 8)
                )
        );
        
        log.info("Default planting questions initialized successfully!");
    }
    
    /**
     * Helper method to create a question with its options
     */
    private void createQuestionWithOptionsInternal(
            String textAr, String textEn, String key,
            boolean required, boolean allowMultiple, int order,
            List<OptionData> optionsData) {
        
        PlantingQuestion question = new PlantingQuestion();
        question.setQuestionTextAr(textAr);
        question.setQuestionTextEn(textEn);
        question.setQuestionKey(key);
        question.setIsRequired(required);
        question.setAllowMultiple(allowMultiple);
        question.setDisplayOrder(order);
        question.setIsActive(true);
        
        for (OptionData data : optionsData) {
            QuestionOption option = new QuestionOption();
            option.setOptionTextAr(data.textAr);
            option.setOptionTextEn(data.textEn);
            option.setOptionKey(data.key);
            option.setDisplayOrder(data.order);
            option.setIsActive(true);
            option.setQuestion(question);
            question.getOptions().add(option);
        }
        
        questionRepository.save(question);
        log.debug("Created question: {} with {} options", key, optionsData.size());
    }
    
    /**
     * Helper record for option data
     */
    private record OptionData(String textAr, String textEn, String key, int order) {}
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasDefaultQuestions() {
        // Check if at least the 5 required questions exist
        return questionRepository.findByQuestionKey("site").isPresent()
                && questionRepository.findByQuestionKey("light").isPresent()
                && questionRepository.findByQuestionKey("container").isPresent()
                && questionRepository.findByQuestionKey("water").isPresent()
                && questionRepository.findByQuestionKey("soil").isPresent();
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
