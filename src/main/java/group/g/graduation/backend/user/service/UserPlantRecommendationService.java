package group.g.graduation.backend.user.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.UserRepository;
import group.g.graduation.backend.Security.util.SecurityUtils;
import group.g.graduation.backend.common.enums.PlantStatus;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantImage;
import group.g.graduation.backend.common.model.PlantRecommendation;
import group.g.graduation.backend.common.model.PlantSuitability;
import group.g.graduation.backend.common.model.PlantingQuestion;
import group.g.graduation.backend.common.model.QuestionOption;
import group.g.graduation.backend.common.model.UserPlant;
import group.g.graduation.backend.common.model.UserQuestionResponse;
import group.g.graduation.backend.common.repository.PlantImageRepository;
import group.g.graduation.backend.common.repository.PlantRecommendationRepository;
import group.g.graduation.backend.common.repository.PlantRepository;
import group.g.graduation.backend.common.repository.PlantSuitabilityRepository;
import group.g.graduation.backend.common.repository.PlantingQuestionRepository;
import group.g.graduation.backend.common.repository.QuestionOptionRepository;
import group.g.graduation.backend.common.repository.UserPlantRepository;
import group.g.graduation.backend.common.repository.UserQuestionResponseRepository;
import group.g.graduation.backend.user.dto.home.WeatherResponse;
import group.g.graduation.backend.user.dto.plant.PlantingQuestionsResponse;
import group.g.graduation.backend.user.dto.plant.RecommendationDetailResponse;
import group.g.graduation.backend.user.dto.plant.RecommendationListResponse;
import group.g.graduation.backend.user.dto.plant.SelectPlantRequest;
import group.g.graduation.backend.user.dto.plant.SelectPlantResponse;
import group.g.graduation.backend.user.dto.plant.SubmitAnswersRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * UserPlantRecommendationService - خدمة اقتراح النباتات للمستخدم
 * 
 * المسار الكامل:
 * 1. جلب الأسئلة (إجبارية + اختيارية)
 * 2. اليوزر يجاوب على الأسئلة
 * 3. حساب التطابق واقتراح نباتات
 * 4. عرض تفاصيل كل اقتراح
 * 5. اليوزر يختار نبتة → تنضاف لمحاصيله
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPlantRecommendationService {

    private final PlantingQuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final PlantSuitabilityRepository suitabilityRepository;
    private final PlantRepository plantRepository;
    private final PlantImageRepository plantImageRepository;
    private final PlantRecommendationRepository recommendationRepository;
    private final UserQuestionResponseRepository userResponseRepository;
    private final UserPlantRepository userPlantRepository;
    private final UserRepository userRepository;
    private final WeatherService weatherService;

    // =====================================================
    // 1. جلب الأسئلة - GET /questions
    // =====================================================

    /**
     * جلب كل الأسئلة النشطة مع خياراتها
     * مقسمة لإجبارية واختيارية، كل وحدة مرتبة حسب displayOrder
     */
    @Transactional(readOnly = true)
    public PlantingQuestionsResponse getPlantingQuestions() {
        log.info("📋 Fetching planting questions for user");

        List<PlantingQuestion> allQuestions = questionRepository.findAllActiveWithOptions();

        List<PlantingQuestionsResponse.QuestionGroup> required = new ArrayList<>();
        List<PlantingQuestionsResponse.QuestionGroup> optional = new ArrayList<>();

        for (PlantingQuestion q : allQuestions) {
            PlantingQuestionsResponse.QuestionGroup group = mapToQuestionGroup(q);

            if (Boolean.TRUE.equals(q.getIsRequired())) {
                required.add(group);
            } else {
                optional.add(group);
            }
        }

        log.info("📋 Found {} required + {} optional questions", required.size(), optional.size());

        return PlantingQuestionsResponse.builder()
                .requiredQuestions(required)
                .optionalQuestions(optional)
                .totalRequired(required.size())
                .totalOptional(optional.size())
                .build();
    }

    // =====================================================
    // 2. تقديم الإجابات والحصول على اقتراحات - POST /submit-answers
    // =====================================================

    /**
     * اليوزر يبعث إجاباته
     * - التحقق من الإجابة على كل الأسئلة الإجبارية
     * - حفظ الإجابات
     * - حساب التطابق لكل نبتة
     * - إرجاع قائمة اقتراحات مرتبة
     */
    @Transactional
    public RecommendationListResponse submitAnswersAndGetRecommendations(SubmitAnswersRequest request) {
        log.info("📝 User submitting answers: {} questions answered", request.getAnswers().size());

        User currentUser = getCurrentUser();

        // 1. جلب بيانات الطقس والموسم تلقائياً 🌤️
        WeatherResponse weather = weatherService.getWeather(null, null, null);
        log.info("🌤️ Current season: {} | Temperature: {}°C", weather.getSeasonEn(), weather.getTemperature());

        // 2. التحقق من الأسئلة الإجبارية
        validateRequiredQuestions(request);

        // 2. التحقق من الأسئلة الإجبارية
        validateRequiredQuestions(request);

        // 3. إضافة بيانات الموسم تلقائياً إذا اختار المستخدم "الموسم الحالي"
        request = addWeatherInfoToRequest(request, weather);

        // 4. إنشاء جلسة جديدة
        String sessionId = generateSessionId();

        // 5. حفظ إجابات اليوزر
        List<Long> allSelectedOptionIds = saveUserResponses(currentUser, sessionId, request);

        // 6. حساب الاقتراحات مع مراعاة الموسم
        List<RecommendationListResponse.RecommendedPlant> recommendations =
                calculateRecommendationsWithWeather(currentUser, sessionId, allSelectedOptionIds, weather);

        // 7. بناء ملخص ظروف اليوزر مع معلومات الطقس
        List<String> conditionsSummary = buildConditionsSummaryWithWeather(request, weather);

        log.info("✅ Generated {} recommendations for session {}", recommendations.size(), sessionId);

        return RecommendationListResponse.builder()
                .sessionId(sessionId)
                .totalRecommendations(recommendations.size())
                .summaryAr("رتّبنا المحاصيل حسب مطابقة الظروف. إذا كانت بعض الظروف غير مناسبة، ستظهر \"مناسب مع تعديل\" مع نصائح بسيطة.")
                .summaryEn("We ranked crops by condition matching. If some conditions don't match, you'll see \"Suitable with adjustment\" with simple tips.")
                .userConditionsSummary(conditionsSummary)
                .recommendations(recommendations)
                .build();
    }

    // =====================================================
    // 3. جلب اقتراحات جلسة سابقة - GET /recommendations/{sessionId}
    // =====================================================

    /**
     * جلب الاقتراحات المحفوظة لجلسة معينة
     */
    @Transactional(readOnly = true)
    public RecommendationListResponse getSessionRecommendations(String sessionId) {
        log.info("📋 Fetching recommendations for session: {}", sessionId);

        User currentUser = getCurrentUser();

        List<PlantRecommendation> savedRecs = recommendationRepository
                .findByUserIdAndSessionIdWithPlant(currentUser.getId(), sessionId);

        if (savedRecs.isEmpty()) {
            throw new EntityNotFoundException("لا توجد اقتراحات لهذه الجلسة");
        }

        List<RecommendationListResponse.RecommendedPlant> recommendations = savedRecs.stream()
                .map(this::mapSavedRecommendation)
                .collect(Collectors.toList());

        // بناء ملخص الظروف من إجابات الجلسة
        List<UserQuestionResponse> responses = userResponseRepository
                .findBySessionIdWithDetails(sessionId);
        List<String> conditionsSummary = responses.stream()
                .map(r -> r.getOption().getOptionTextAr())
                .distinct()
                .collect(Collectors.toList());

        return RecommendationListResponse.builder()
                .sessionId(sessionId)
                .totalRecommendations(recommendations.size())
                .summaryAr("رتّبنا المحاصيل حسب مطابقة الظروف. إذا كانت بعض الظروف غير مناسبة، ستظهر \"مناسب مع تعديل\" مع نصائح بسيطة.")
                .summaryEn("We ranked crops by condition matching. If some conditions don't match, you'll see \"Suitable with adjustment\" with simple tips.")
                .userConditionsSummary(conditionsSummary)
                .recommendations(recommendations)
                .build();
    }

    // =====================================================
    // 4. تفاصيل اقتراح معين - GET /recommendations/{sessionId}/plant/{plantId}
    // =====================================================

    /**
     * تفاصيل كاملة لنبتة مقترحة:
     * - ليش اقترحناها
     * - تعديلات بسيطة
     * - معلومات العناية
     */
    @Transactional(readOnly = true)
    public RecommendationDetailResponse getRecommendationDetail(String sessionId, Long plantId) {
        log.info("🔍 Fetching recommendation detail: session={}, plant={}", sessionId, plantId);

        // التحقق من أنو اليوزر مسجل دخول
        getCurrentUser();

        // جلب التوصية المحفوظة
        PlantRecommendation recommendation = recommendationRepository
                .findBySessionIdAndPlantId(sessionId, plantId)
                .orElseThrow(() -> new EntityNotFoundException("التوصية غير موجودة لهذه الجلسة"));

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new EntityNotFoundException("النبتة غير موجودة"));

        // جلب الخيارات اللي اختارها اليوزر بهالجلسة
        List<Long> selectedOptionIds = userResponseRepository
                .findSelectedOptionIdsBySessionId(sessionId);

        // جلب تفاصيل التطابق لكل سؤال
        List<RecommendationDetailResponse.QuestionMatchInfo> questionDetails =
                buildQuestionMatchDetails(plantId, selectedOptionIds);

        // أسباب الاقتراح
        List<String> reasonsAr = new ArrayList<>();
        List<String> reasonsEn = new ArrayList<>();
        List<String> adjustmentTipsAr = new ArrayList<>();
        List<String> adjustmentTipsEn = new ArrayList<>();
        List<String> quickTagsAr = new ArrayList<>();
        List<String> quickTagsEn = new ArrayList<>();

        for (RecommendationDetailResponse.QuestionMatchInfo detail : questionDetails) {
            if (detail.getScore() >= 70) {
                reasonsAr.add(detail.getSelectedOptionTextAr() + " مناسب" + (detail.getQuestionTextAr() != null ? "." : ""));
                reasonsEn.add(detail.getSelectedOptionTextEn() + " is suitable.");
            }

            if (detail.getAdjustmentTipAr() != null && !detail.getAdjustmentTipAr().isEmpty()) {
                adjustmentTipsAr.add(detail.getAdjustmentTipAr());
            }
            if (detail.getAdjustmentTipEn() != null && !detail.getAdjustmentTipEn().isEmpty()) {
                adjustmentTipsEn.add(detail.getAdjustmentTipEn());
            }

            // بناء التاجات السريعة
            buildQuickTags(detail, quickTagsAr, quickTagsEn);
        }

        // إضافة أسباب من التوصية المحفوظة
        if (recommendation.getRecommendationReasonAr() != null) {
            reasonsAr.add(0, recommendation.getRecommendationReasonAr());
        }
        if (recommendation.getRecommendationReasonEn() != null) {
            reasonsEn.add(0, recommendation.getRecommendationReasonEn());
        }

        // الصورة الرئيسية
        String imageUrl = getPlantPrimaryImageUrl(plantId);

        boolean needsAdjustment = recommendation.getMatchPercentage() < 80;

        return RecommendationDetailResponse.builder()
                .plantId(plantId)
                .nameAr(plant.getNameAr())
                .nameEn(plant.getNameEn())
                .nameScientific(plant.getNameScientific())
                .shortDescriptionAr(plant.getShortDescriptionAr())
                .shortDescriptionEn(plant.getShortDescriptionEn())
                .imageUrl(imageUrl)
                .category(plant.getCategory() != null ? plant.getCategory().name() : null)
                .difficultyLevel(plant.getDifficultyLevel() != null ? plant.getDifficultyLevel().name() : null)
                .matchPercentage(recommendation.getMatchPercentage())
                .matchLevel(getMatchLevel(recommendation.getMatchPercentage()))
                .matchLevelAr(getMatchLevelAr(recommendation.getMatchPercentage()))
                .needsAdjustment(needsAdjustment)
                .questionDetails(questionDetails)
                .recommendationReasonsAr(reasonsAr)
                .recommendationReasonsEn(reasonsEn)
                .adjustmentTipsAr(adjustmentTipsAr)
                .adjustmentTipsEn(adjustmentTipsEn)
                .quickTagsAr(quickTagsAr)
                .quickTagsEn(quickTagsEn)
                .careInfo(RecommendationDetailResponse.CareInfo.builder()
                        .lightInfoAr(plant.getLightInfoAr())
                        .lightInfoEn(plant.getLightInfoEn())
                        .soilInfoAr(plant.getSoilInfoAr())
                        .soilInfoEn(plant.getSoilInfoEn())
                        .wateringInfoAr(plant.getWateringInfoAr())
                        .wateringInfoEn(plant.getWateringInfoEn())
                        .wateringIntervalDays(plant.getWateringIntervalDays())
                        .careInfoAr(plant.getCareInfoAr())
                        .careInfoEn(plant.getCareInfoEn())
                        .build())
                .build();
    }

    // =====================================================
    // 5. اختيار نبتة - POST /select
    // =====================================================

    /**
     * اليوزر يختار نبتة من الاقتراحات
     * → النبتة تنضاف لمحاصيله بحالة PLANNED
     */
    @Transactional
    public SelectPlantResponse selectPlant(SelectPlantRequest request) {
        log.info("🌱 User selecting plant {} from session {}", request.getPlantId(), request.getSessionId());

        User currentUser = getCurrentUser();

        // التحقق من وجود التوصية
        PlantRecommendation recommendation = recommendationRepository
                .findBySessionIdAndPlantId(request.getSessionId(), request.getPlantId())
                .orElseThrow(() -> new EntityNotFoundException("هذا الاقتراح غير موجود في الجلسة المحددة"));

        Plant plant = plantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new EntityNotFoundException("النبتة غير موجودة"));

        // تعليم التوصية كمختارة
        recommendation.setIsSelected(true);
        recommendationRepository.save(recommendation);

        // إنشاء نبتة المستخدم
        UserPlant userPlant = new UserPlant();
        userPlant.setUser(currentUser);
        userPlant.setPlant(plant);
        userPlant.setStatus(PlantStatus.PLANNED);
        userPlant.setNickname(request.getNickname());
        userPlant.setNotes(request.getNotes());
        userPlant.setPlannedDate(LocalDate.now());

        UserPlant saved = userPlantRepository.save(userPlant);
        log.info("✅ Plant {} added to user's crops with ID {}", plant.getNameAr(), saved.getId());

        String imageUrl = getPlantPrimaryImageUrl(plant.getId());

        return SelectPlantResponse.builder()
                .userPlantId(saved.getId())
                .plantId(plant.getId())
                .plantNameAr(plant.getNameAr())
                .plantNameEn(plant.getNameEn())
                .plantNameScientific(plant.getNameScientific())
                .imageUrl(imageUrl)
                .nickname(saved.getNickname())
                .status(saved.getStatus().name())
                .plannedDate(saved.getPlannedDate())
                .messageAr("تم إضافة " + plant.getNameAr() + " لمحاصيلك بنجاح! 🌱")
                .messageEn(plant.getNameEn() + " has been added to your crops! 🌱")
                .build();
    }

    // =====================================================
    // ===== Private Helper Methods =====
    // =====================================================

    /**
     * جلب المستخدم الحالي من السيكيوريتي كونتكست
     */
    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new RuntimeException("المستخدم غير مسجل دخول"));

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("المستخدم غير موجود"));
    }

    /**
     * تحويل سؤال إلى QuestionGroup DTO
     */
    private PlantingQuestionsResponse.QuestionGroup mapToQuestionGroup(PlantingQuestion q) {
        List<PlantingQuestionsResponse.OptionItem> options = q.getOptions().stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsActive()))
                .map(o -> PlantingQuestionsResponse.OptionItem.builder()
                        .optionId(o.getId())
                        .optionKey(o.getOptionKey())
                        .optionTextAr(o.getOptionTextAr())
                        .optionTextEn(o.getOptionTextEn())
                        .displayOrder(o.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        return PlantingQuestionsResponse.QuestionGroup.builder()
                .questionId(q.getId())
                .questionKey(q.getQuestionKey())
                .questionTextAr(q.getQuestionTextAr())
                .questionTextEn(q.getQuestionTextEn())
                .isRequired(q.getIsRequired())
                .allowMultiple(q.getAllowMultiple())
                .displayOrder(q.getDisplayOrder())
                .options(options)
                .build();
    }

    /**
     * التحقق من إجابة اليوزر على كل الأسئلة الإجبارية
     */
    private void validateRequiredQuestions(SubmitAnswersRequest request) {
        List<PlantingQuestion> requiredQuestions = questionRepository
                .findByIsRequiredTrueAndIsActiveTrueOrderByDisplayOrderAsc();

        Set<Long> answeredQuestionIds = request.getAnswers().stream()
                .map(SubmitAnswersRequest.QuestionAnswer::getQuestionId)
                .collect(Collectors.toSet());

        List<String> missingQuestions = new ArrayList<>();
        for (PlantingQuestion rq : requiredQuestions) {
            if (!answeredQuestionIds.contains(rq.getId())) {
                missingQuestions.add(rq.getQuestionTextAr());
            }
        }

        if (!missingQuestions.isEmpty()) {
            throw new IllegalArgumentException(
                    "يجب الإجابة على كل الأسئلة الإجبارية. الأسئلة الناقصة: " +
                            String.join("، ", missingQuestions));
        }

        // التحقق من صحة الخيارات
        for (SubmitAnswersRequest.QuestionAnswer answer : request.getAnswers()) {
            PlantingQuestion question = questionRepository.findById(answer.getQuestionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "السؤال غير موجود: " + answer.getQuestionId()));

            // التحقق من أنو ما اختار أكثر من خيار لسؤال ما بيسمح
            if (Boolean.FALSE.equals(question.getAllowMultiple()) && answer.getSelectedOptionIds().size() > 1) {
                throw new IllegalArgumentException(
                        "السؤال \"" + question.getQuestionTextAr() + "\" يسمح باختيار خيار واحد فقط");
            }

            // التحقق من صحة كل الخيارات المختارة
            for (Long optionId : answer.getSelectedOptionIds()) {
                if (!optionRepository.existsById(optionId)) {
                    throw new EntityNotFoundException("الخيار غير موجود: " + optionId);
                }
            }
        }
    }

    /**
     * إنشاء معرّف جلسة فريد
     */
    private String generateSessionId() {
        return "session-" + UUID.randomUUID().toString().substring(0, 8) + "-" + System.currentTimeMillis();
    }

    /**
     * حفظ إجابات اليوزر في قاعدة البيانات
     */
    private List<Long> saveUserResponses(User user, String sessionId, SubmitAnswersRequest request) {
        List<Long> allOptionIds = new ArrayList<>();

        for (SubmitAnswersRequest.QuestionAnswer answer : request.getAnswers()) {
            PlantingQuestion question = questionRepository.findById(answer.getQuestionId()).orElse(null);
            if (question == null) continue;

            for (Long optionId : answer.getSelectedOptionIds()) {
                QuestionOption option = optionRepository.findById(optionId).orElse(null);
                if (option == null) continue;

                UserQuestionResponse response = new UserQuestionResponse();
                response.setUser(user);
                response.setSessionId(sessionId);
                response.setQuestion(question);
                response.setOption(option);

                userResponseRepository.save(response);
                allOptionIds.add(optionId);
            }
        }

        log.info("💾 Saved {} user responses for session {}", allOptionIds.size(), sessionId);
        return allOptionIds;
    }

    /**
     * حساب التوصيات بناءً على إجابات اليوزر
     * الخوارزمية:
     * 1. نجمع نقاط كل نبتة من جداول PlantSuitability
     * 2. نحسب النسبة المئوية (مجموع النقاط / أقصى نقاط ممكنة × 100)
     * 3. نرتب من الأعلى للأقل
     * 4. نحفظ النتائج في PlantRecommendation
     */
    private List<RecommendationListResponse.RecommendedPlant> calculateRecommendations(
            User user, String sessionId, List<Long> selectedOptionIds) {

        if (selectedOptionIds.isEmpty()) {
            return Collections.emptyList();
        }

        // جمع نقاط النباتات من قاعدة البيانات
        List<Object[]> plantScores = suitabilityRepository.findPlantScoresByOptions(selectedOptionIds);
        int maxPossibleScore = selectedOptionIds.size() * 100;

        List<RecommendationListResponse.RecommendedPlant> recommendations = new ArrayList<>();

        for (Object[] row : plantScores) {
            Long plantId = (Long) row[0];
            Long totalScore = (Long) row[1];

            Plant plant = plantRepository.findById(plantId).orElse(null);
            if (plant == null) continue;

            double matchPercentage = maxPossibleScore > 0
                    ? (totalScore.doubleValue() / maxPossibleScore) * 100 : 0;

            // فلترة: بس النباتات بنسبة 30% وأكثر
            if (matchPercentage < 30) continue;

            matchPercentage = Math.round(matchPercentage * 100.0) / 100.0;

            // بناء أسباب وملخصات
            List<PlantSuitability> plantSuitabilities = suitabilityRepository
                    .findByPlantIdWithOptionDetails(plantId);

            // بناء ملخص الظروف
            String conditionsSummaryAr = buildPlantConditionsSummary(plantSuitabilities, selectedOptionIds, "ar");
            String conditionsSummaryEn = buildPlantConditionsSummary(plantSuitabilities, selectedOptionIds, "en");

            // التاجات السريعة
            List<String> quickTagsAr = buildPlantQuickTags(plantSuitabilities, selectedOptionIds, "ar");
            List<String> quickTagsEn = buildPlantQuickTags(plantSuitabilities, selectedOptionIds, "en");

            // نصائح التعديل
            List<String> adjustmentTips = plantSuitabilities.stream()
                    .filter(s -> selectedOptionIds.contains(s.getOption().getId()))
                    .filter(s -> s.getScore() < 60)
                    .filter(s -> s.getAdjustmentTipAr() != null && !s.getAdjustmentTipAr().isEmpty())
                    .map(PlantSuitability::getAdjustmentTipAr)
                    .distinct()
                    .collect(Collectors.toList());

            boolean needsAdjustment = matchPercentage < 80;

            // بناء أسباب الاقتراح
            String reasonAr = buildRecommendationReasonAr(plantSuitabilities, selectedOptionIds);
            String reasonEn = buildRecommendationReasonEn(plantSuitabilities, selectedOptionIds);
            String tipsAr = String.join("\n", adjustmentTips);

            // حفظ التوصية في قاعدة البيانات
            PlantRecommendation rec = new PlantRecommendation();
            rec.setUser(user);
            rec.setSessionId(sessionId);
            rec.setPlant(plant);
            rec.setMatchPercentage(matchPercentage);
            rec.setRecommendationReasonAr(reasonAr);
            rec.setRecommendationReasonEn(reasonEn);
            rec.setAdjustmentTipsAr(tipsAr);
            rec.setAdjustmentTipsEn(String.join("\n",
                    plantSuitabilities.stream()
                            .filter(s -> selectedOptionIds.contains(s.getOption().getId()))
                            .filter(s -> s.getScore() < 60)
                            .filter(s -> s.getAdjustmentTipEn() != null && !s.getAdjustmentTipEn().isEmpty())
                            .map(PlantSuitability::getAdjustmentTipEn)
                            .distinct()
                            .collect(Collectors.toList())));
            rec.setIsSelected(false);

            recommendationRepository.save(rec);

            // الصورة
            String imageUrl = getPlantPrimaryImageUrl(plantId);

            recommendations.add(RecommendationListResponse.RecommendedPlant.builder()
                    .plantId(plantId)
                    .nameAr(plant.getNameAr())
                    .nameEn(plant.getNameEn())
                    .nameScientific(plant.getNameScientific())
                    .imageUrl(imageUrl)
                    .matchPercentage(matchPercentage)
                    .matchLevel(getMatchLevel(matchPercentage))
                    .matchLevelAr(getMatchLevelAr(matchPercentage))
                    .quickTagsAr(quickTagsAr)
                    .quickTagsEn(quickTagsEn)
                    .needsAdjustment(needsAdjustment)
                    .conditionsSummaryAr(conditionsSummaryAr)
                    .conditionsSummaryEn(conditionsSummaryEn)
                    .build());
        }

        log.info("📊 Calculated {} recommendations (filtered from {} plants)", recommendations.size(), plantScores.size());
        return recommendations;
    }

    /**
     * بناء ملخص ظروف اليوزر من إجاباته
     */
    private List<String> buildConditionsSummary(SubmitAnswersRequest request) {
        List<String> summary = new ArrayList<>();
        for (SubmitAnswersRequest.QuestionAnswer answer : request.getAnswers()) {
            for (Long optionId : answer.getSelectedOptionIds()) {
                optionRepository.findByIdWithQuestion(optionId).ifPresent(option -> {
                    String questionText = option.getQuestion() != null ? option.getQuestion().getQuestionTextAr() : "";
                    summary.add(questionText + ": " + option.getOptionTextAr());
                });
            }
        }
        return summary;
    }

    /**
     * بناء ملخص ظروف نبتة مقترحة
     */
    private String buildPlantConditionsSummary(List<PlantSuitability> suitabilities,
                                               List<Long> selectedOptionIds, String lang) {
        return suitabilities.stream()
                .filter(s -> selectedOptionIds.contains(s.getOption().getId()))
                .map(s -> {
                    String questionKey = s.getOption().getQuestion().getQuestionKey();
                    String prefix = getQuestionShortLabel(questionKey, lang);
                    String optionText = "ar".equals(lang) ?
                            s.getOption().getOptionTextAr() : s.getOption().getOptionTextEn();
                    return prefix + ": " + optionText;
                })
                .collect(Collectors.joining(" • "));
    }

    /**
     * بناء تاجات سريعة لنبتة
     */
    private List<String> buildPlantQuickTags(List<PlantSuitability> suitabilities,
                                              List<Long> selectedOptionIds, String lang) {
        return suitabilities.stream()
                .filter(s -> selectedOptionIds.contains(s.getOption().getId()))
                .filter(s -> s.getScore() >= 50)
                .map(s -> {
                    String questionKey = s.getOption().getQuestion().getQuestionKey();
                    String prefix = getQuestionShortLabel(questionKey, lang);
                    String optionText = "ar".equals(lang) ?
                            s.getOption().getOptionTextAr() : s.getOption().getOptionTextEn();
                    return prefix + ": " + optionText;
                })
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * بناء تفاصيل التطابق لكل سؤال
     */
    private List<RecommendationDetailResponse.QuestionMatchInfo> buildQuestionMatchDetails(
            Long plantId, List<Long> selectedOptionIds) {

        List<PlantSuitability> suitabilities = suitabilityRepository
                .findByPlantIdWithOptionDetails(plantId);

        // تجميع حسب السؤال
        Map<Long, List<PlantSuitability>> byQuestion = suitabilities.stream()
                .filter(s -> selectedOptionIds.contains(s.getOption().getId()))
                .collect(Collectors.groupingBy(s -> s.getOption().getQuestion().getId()));

        List<RecommendationDetailResponse.QuestionMatchInfo> details = new ArrayList<>();

        for (Map.Entry<Long, List<PlantSuitability>> entry : byQuestion.entrySet()) {
            List<PlantSuitability> questionSuits = entry.getValue();
            if (questionSuits.isEmpty()) continue;

            PlantSuitability first = questionSuits.get(0);
            PlantingQuestion question = first.getOption().getQuestion();

            double avgScore = questionSuits.stream()
                    .mapToInt(PlantSuitability::getScore).average().orElse(0);

            String selectedOptionsAr = questionSuits.stream()
                    .map(s -> s.getOption().getOptionTextAr())
                    .collect(Collectors.joining("، "));
            String selectedOptionsEn = questionSuits.stream()
                    .map(s -> s.getOption().getOptionTextEn())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));

            // جمع نصائح التعديل
            String adjustmentTipAr = questionSuits.stream()
                    .filter(s -> s.getAdjustmentTipAr() != null && !s.getAdjustmentTipAr().isEmpty())
                    .map(PlantSuitability::getAdjustmentTipAr)
                    .collect(Collectors.joining(" "));
            String adjustmentTipEn = questionSuits.stream()
                    .filter(s -> s.getAdjustmentTipEn() != null && !s.getAdjustmentTipEn().isEmpty())
                    .map(PlantSuitability::getAdjustmentTipEn)
                    .collect(Collectors.joining(" "));

            int score = (int) avgScore;

            details.add(RecommendationDetailResponse.QuestionMatchInfo.builder()
                    .questionId(question.getId())
                    .questionKey(question.getQuestionKey())
                    .questionTextAr(question.getQuestionTextAr())
                    .questionTextEn(question.getQuestionTextEn())
                    .selectedOptionTextAr(selectedOptionsAr)
                    .selectedOptionTextEn(selectedOptionsEn)
                    .score(score)
                    .scoreLevel(getScoreLevel(score))
                    .scoreLevelAr(getScoreLevelAr(score))
                    .adjustmentTipAr(adjustmentTipAr.isEmpty() ? null : adjustmentTipAr)
                    .adjustmentTipEn(adjustmentTipEn.isEmpty() ? null : adjustmentTipEn)
                    .build());
        }

        return details;
    }

    /**
     * بناء التاجات السريعة من تفاصيل السؤال
     */
    private void buildQuickTags(RecommendationDetailResponse.QuestionMatchInfo detail,
                                 List<String> tagsAr, List<String> tagsEn) {
        String keyLabel = getQuestionShortLabel(detail.getQuestionKey(), "ar");
        String keyLabelEn = getQuestionShortLabel(detail.getQuestionKey(), "en");

        if (detail.getSelectedOptionTextAr() != null) {
            tagsAr.add(keyLabel + ": " + detail.getSelectedOptionTextAr());
        }
        if (detail.getSelectedOptionTextEn() != null) {
            tagsEn.add(keyLabelEn + ": " + detail.getSelectedOptionTextEn());
        }
    }

    /**
     * بناء سبب الاقتراح بالعربي
     */
    private String buildRecommendationReasonAr(List<PlantSuitability> suitabilities,
                                                List<Long> selectedOptionIds) {
        List<String> goodMatches = suitabilities.stream()
                .filter(s -> selectedOptionIds.contains(s.getOption().getId()))
                .filter(s -> s.getScore() >= 70)
                .map(s -> s.getOption().getOptionTextAr() + " مناسب")
                .distinct()
                .collect(Collectors.toList());

        if (goodMatches.isEmpty()) return "متوافق مع بعض ظروفك.";
        return String.join("، ", goodMatches) + ".";
    }

    /**
     * بناء سبب الاقتراح بالإنجليزي
     */
    private String buildRecommendationReasonEn(List<PlantSuitability> suitabilities,
                                                List<Long> selectedOptionIds) {
        List<String> goodMatches = suitabilities.stream()
                .filter(s -> selectedOptionIds.contains(s.getOption().getId()))
                .filter(s -> s.getScore() >= 70)
                .map(s -> {
                    String optText = s.getOption().getOptionTextEn();
                    return optText != null ? optText + " is suitable" : "";
                })
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (goodMatches.isEmpty()) return "Compatible with some of your conditions.";
        return String.join(", ", goodMatches) + ".";
    }

    /**
     * تحويل توصية محفوظة إلى DTO
     */
    private RecommendationListResponse.RecommendedPlant mapSavedRecommendation(PlantRecommendation rec) {
        Plant plant = rec.getPlant();
        String imageUrl = getPlantPrimaryImageUrl(plant.getId());

        boolean needsAdjustment = rec.getMatchPercentage() < 80;

        // التاجات السريعة من التوصية المحفوظة
        List<String> quickTagsAr = new ArrayList<>();
        List<String> quickTagsEn = new ArrayList<>();

        if (plant.getLightInfoAr() != null) quickTagsAr.add("ضوء: " + plant.getLightInfoAr());
        if (plant.getSoilInfoAr() != null) quickTagsAr.add("تربة: " + plant.getSoilInfoAr());
        if (plant.getWateringInfoAr() != null) quickTagsAr.add("ري: " + plant.getWateringInfoAr());

        if (plant.getLightInfoEn() != null) quickTagsEn.add("Light: " + plant.getLightInfoEn());
        if (plant.getSoilInfoEn() != null) quickTagsEn.add("Soil: " + plant.getSoilInfoEn());
        if (plant.getWateringInfoEn() != null) quickTagsEn.add("Water: " + plant.getWateringInfoEn());

        return RecommendationListResponse.RecommendedPlant.builder()
                .plantId(plant.getId())
                .nameAr(plant.getNameAr())
                .nameEn(plant.getNameEn())
                .nameScientific(plant.getNameScientific())
                .imageUrl(imageUrl)
                .matchPercentage(rec.getMatchPercentage())
                .matchLevel(getMatchLevel(rec.getMatchPercentage()))
                .matchLevelAr(getMatchLevelAr(rec.getMatchPercentage()))
                .quickTagsAr(quickTagsAr)
                .quickTagsEn(quickTagsEn)
                .needsAdjustment(needsAdjustment)
                .conditionsSummaryAr(rec.getRecommendationReasonAr())
                .conditionsSummaryEn(rec.getRecommendationReasonEn())
                .build();
    }

    /**
     * جلب صورة النبتة الرئيسية
     */
    private String getPlantPrimaryImageUrl(Long plantId) {
        return plantImageRepository.findByPlantIdAndIsPrimaryTrue(plantId)
                .map(PlantImage::getImageUrl)
                .orElse(null);
    }

    /**
     * تسمية مختصرة لكل سؤال حسب المفتاح
     */
    private String getQuestionShortLabel(String questionKey, String lang) {
        if (questionKey == null) return "";

        Map<String, String[]> labels = new HashMap<>();
        labels.put("site", new String[]{"مكان", "Location"});
        labels.put("light", new String[]{"ضوء", "Light"});
        labels.put("container", new String[]{"وعاء", "Container"});
        labels.put("water", new String[]{"ري", "Watering"});
        labels.put("soil", new String[]{"تربة", "Soil"});
        labels.put("drainage", new String[]{"تصريف", "Drainage"});
        labels.put("holes", new String[]{"فتحات تصريف", "Drainage Holes"});
        labels.put("wind", new String[]{"رياح", "Wind"});
        labels.put("growlight", new String[]{"إضاءة صناعية", "Grow Light"});
        labels.put("prefs", new String[]{"تفضيلات", "Preference"});

        String[] pair = labels.get(questionKey);
        if (pair != null) {
            return "ar".equals(lang) ? pair[0] : pair[1];
        }
        return questionKey;
    }

    // ===== مستويات التطابق =====

    private String getMatchLevel(double percentage) {
        if (percentage >= 80) return "excellent";
        if (percentage >= 60) return "good";
        if (percentage >= 40) return "fair";
        return "poor";
    }

    private String getMatchLevelAr(double percentage) {
        if (percentage >= 80) return "مناسب";
        if (percentage >= 60) return "مناسب مع تعديل";
        if (percentage >= 40) return "يحتاج تعديلات";
        return "غير مناسب";
    }

    private String getScoreLevel(int score) {
        if (score >= 80) return "excellent";
        if (score >= 60) return "good";
        if (score >= 40) return "fair";
        return "poor";
    }
    
    // =====================================================
    // Weather & Season Integration Methods
    // =====================================================
    
    /**
     * إضافة بيانات الطقس والموسم إلى طلب الإجابات
     */
    private SubmitAnswersRequest addWeatherInfoToRequest(SubmitAnswersRequest request, WeatherResponse weather) {
        // البحث عن سؤال الموسم
        Optional<SubmitAnswersRequest.QuestionAnswer> seasonAnswer = request.getAnswers().stream()
                .filter(ans -> {
                    try {
                        PlantingQuestion q = questionRepository.findById(ans.getQuestionId()).orElse(null);
                        return q != null && "season".equals(q.getQuestionKey());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst();
                
        if (seasonAnswer.isPresent()) {
            // بحث عن خيار "الموسم الحالي"
            Long currentSeasonOptionId = findSeasonOptionId("current");
            if (currentSeasonOptionId != null && seasonAnswer.get().getSelectedOptionIds().contains(currentSeasonOptionId)) {
                // استبدال بخيار الموسم الفعلي
                Long actualSeasonOptionId = findSeasonOptionIdByName(weather.getSeasonEn().toLowerCase());
                if (actualSeasonOptionId != null) {
                    seasonAnswer.get().getSelectedOptionIds().clear();
                    seasonAnswer.get().getSelectedOptionIds().add(actualSeasonOptionId);
                    log.info("🌤️ Auto-selected season: {} (option ID: {})", weather.getSeasonEn(), actualSeasonOptionId);
                }
            }
        }
        
        return request;
    }
    
    /**
     * العثور على ID خيار الموسم
     */
    private Long findSeasonOptionId(String optionKey) {
        try {
            PlantingQuestion seasonQuestion = questionRepository.findByQuestionKey("season").orElse(null);
            if (seasonQuestion == null) return null;
            
            return seasonQuestion.getOptions().stream()
                    .filter(opt -> optionKey.equals(opt.getOptionKey()))
                    .map(QuestionOption::getId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("⚠️ Failed to find season option: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * العثور على ID خيار الموسم حسب الاسم
     */
    private Long findSeasonOptionIdByName(String seasonName) {
        String optionKey = switch (seasonName.toLowerCase()) {
            case "spring" -> "spring";
            case "summer" -> "summer";
            case "autumn", "fall" -> "autumn";
            case "winter" -> "winter";
            default -> null;
        };
        
        return optionKey != null ? findSeasonOptionId(optionKey) : null;
    }
    
    /**
     * حساب الاقتراحات مع مراعاة الطقس والموسم
     */
    private List<RecommendationListResponse.RecommendedPlant> calculateRecommendationsWithWeather(
            User user, String sessionId, List<Long> selectedOptionIds, WeatherResponse weather) {
        
        // استخدام نفس الخوارزمية الأساسية
        List<RecommendationListResponse.RecommendedPlant> recommendations = 
                calculateRecommendations(user, sessionId, selectedOptionIds);
                
        // تحسين النتائج حسب الموسم
        return recommendations.stream()
                .map(rec -> applySeasonalBonus(rec, weather))
                .sorted((a, b) -> Double.compare(b.getMatchPercentage(), a.getMatchPercentage()))
                .collect(Collectors.toList());
    }
    
    /**
     * تطبيق مكافأة موسمية للنباتات
     */
    private RecommendationListResponse.RecommendedPlant applySeasonalBonus(
            RecommendationListResponse.RecommendedPlant plant, WeatherResponse weather) {
        
        double currentMatch = plant.getMatchPercentage();
        double bonus = 0.0;
        
        // مكافآت حسب الموسم والنبات
        String season = weather.getSeasonEn().toLowerCase();
        String plantName = plant.getNameEn().toLowerCase();
        
        // نباتات مناسبة للفصول المختلفة
        if (season.equals("spring") && (plantName.contains("mint") || plantName.contains("basil") || plantName.contains("parsley"))) {
            bonus = 10.0; // مكافأة الربيع
        } else if (season.equals("summer") && (plantName.contains("tomato") || plantName.contains("pepper") || plantName.contains("cucumber"))) {
            bonus = 10.0; // مكافأة الصيف
        } else if (season.equals("autumn") && (plantName.contains("lettuce") || plantName.contains("spinach") || plantName.contains("kale"))) {
            bonus = 10.0; // مكافأة الخريف
        } else if (season.equals("winter") && (plantName.contains("rosemary") || plantName.contains("thyme") || plantName.contains("sage"))) {
            bonus = 10.0; // مكافأة الشتاء
        }
        
        // تطبيق المكافأة
        if (bonus > 0) {
            double newMatch = Math.min(100.0, currentMatch + bonus);
            plant.setMatchPercentage(newMatch);
            plant.setMatchLevel(getMatchLevel(newMatch));
            plant.setMatchLevelAr(getMatchLevelAr(newMatch));
            
            log.debug("🌿 Seasonal bonus for {}: +{}% ({}% -> {}%)", 
                    plant.getNameEn(), bonus, currentMatch, newMatch);
        }
        
        return plant;
    }
    
    /**
     * بناء ملخص الظروف مع معلومات الطقس
     */
    private List<String> buildConditionsSummaryWithWeather(SubmitAnswersRequest request, WeatherResponse weather) {
        List<String> summary = buildConditionsSummary(request);
        
        // إضافة معلومات الطقس
        summary.add(0, "🌤️ الموسم الحالي: " + weather.getSeasonAr() + " | درجة الحرارة: " + weather.getTemperature() + "°م");
        
        return summary;
    }

    private String getScoreLevelAr(int score) {
        if (score >= 80) return "ممتاز";
        if (score >= 60) return "جيد";
        if (score >= 40) return "مقبول";
        return "ضعيف";
    }
}
