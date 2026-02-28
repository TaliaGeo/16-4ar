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
import group.g.graduation.backend.common.model.MonthPlant;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.model.PlantImage;
import group.g.graduation.backend.common.model.PlantRecommendation;
import group.g.graduation.backend.common.model.PlantSuitability;
import group.g.graduation.backend.common.model.PlantingQuestion;
import group.g.graduation.backend.common.model.QuestionOption;
import group.g.graduation.backend.common.model.UserPlant;
import group.g.graduation.backend.common.model.UserPreference;
import group.g.graduation.backend.common.model.UserQuestionResponse;
import group.g.graduation.backend.common.repository.MonthPlantRepository;
import group.g.graduation.backend.common.repository.MonthRepository;
import group.g.graduation.backend.common.repository.PlantImageRepository;
import group.g.graduation.backend.common.repository.PlantRecommendationRepository;
import group.g.graduation.backend.common.repository.PlantRepository;
import group.g.graduation.backend.common.repository.PlantSuitabilityRepository;
import group.g.graduation.backend.common.repository.PlantingQuestionRepository;
import group.g.graduation.backend.common.repository.QuestionOptionRepository;
import group.g.graduation.backend.common.repository.UserPlantRepository;
import group.g.graduation.backend.common.repository.UserPreferenceRepository;
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
    private final UserPreferenceRepository userPreferenceRepository;
    private final MonthRepository monthRepository;
    private final MonthPlantRepository monthPlantRepository;

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

        // 1. تحديد موقع اليوزر: GPS من الطلب → موقع محفوظ → الافتراضي 🌍
        String city = null;
        Double lat = request.getLatitude();
        Double lon = request.getLongitude();
        boolean locationUsed = false;

        if (lat != null && lon != null) {
            // استخدام GPS المباشر من التطبيق
            log.info("📍 Using real-time GPS from request: ({}, {})", lat, lon);
            locationUsed = true;
        } else {
            // محاولة جلب الموقع المحفوظ للمستخدم
            Optional<UserPreference> prefOpt = userPreferenceRepository.findByUserId(currentUser.getId());
            if (prefOpt.isPresent() && prefOpt.get().getLatitude() != null && prefOpt.get().getLongitude() != null) {
                UserPreference pref = prefOpt.get();
                city = pref.getCity();
                lat = pref.getLatitude();
                lon = pref.getLongitude();
                locationUsed = true;
                log.info("📍 Using user's saved location: {} ({}, {})", city, lat, lon);
            } else {
                log.info("📍 No user location set — using default (Nablus)");
            }
        }

        // 2. جلب بيانات الطقس بناءً على الموقع الفعلي 🌤️
        WeatherResponse weather = weatherService.getWeather(city, lat, lon);
        log.info("🌤️ Weather for {}: season={}, temp={}°C, humidity={}%",
                weather.getLocationEn(), weather.getSeasonEn(), weather.getTemperature(), weather.getHumidity());

        // 3. تحديد المنطقة المناخية بناءً على الموقع
        String climateZoneEn = determineClimateZone(lat, lon, weather.getLocationEn());
        String climateZoneAr = translateClimateZone(climateZoneEn);
        log.info("🏔️ Climate zone: {} ({})", climateZoneEn, climateZoneAr);

        // 4. التحقق من الأسئلة الإجبارية
        validateRequiredQuestions(request);

        // 5. إضافة بيانات الموسم تلقائياً إذا اختار المستخدم "الموسم الحالي"
        request = addWeatherInfoToRequest(request, weather);

        // 6. إنشاء جلسة جديدة
        String sessionId = generateSessionId();

        // 7. حفظ إجابات اليوزر
        List<Long> allSelectedOptionIds = saveUserResponses(currentUser, sessionId, request);

        // 8. حساب الاقتراحات مع مراعاة الطقس والموسم والموقع
        List<RecommendationListResponse.RecommendedPlant> recommendations =
                calculateRecommendationsWithWeather(currentUser, sessionId, allSelectedOptionIds, weather, climateZoneEn);

        // 9. بناء ملخص ظروف اليوزر مع معلومات الطقس
        List<String> conditionsSummary = buildConditionsSummaryWithWeather(request, weather);

        // 10. بناء سياق الطقس للاستجابة
        RecommendationListResponse.WeatherContext weatherContext = RecommendationListResponse.WeatherContext.builder()
                .locationAr(weather.getLocationAr())
                .locationEn(weather.getLocationEn())
                .temperature(weather.getTemperature())
                .seasonAr(weather.getSeasonAr())
                .seasonEn(weather.getSeasonEn())
                .climateZoneAr(climateZoneAr)
                .climateZoneEn(climateZoneEn)
                .locationUsed(locationUsed)
                .weatherDescriptionAr(weather.getDescriptionAr())
                .weatherDescriptionEn(weather.getDescriptionEn())
                .build();

        log.info("✅ Generated {} recommendations for session {} (location: {}, season: {}, zone: {})",
                recommendations.size(), sessionId, weather.getLocationEn(), weather.getSeasonEn(), climateZoneEn);

        return RecommendationListResponse.builder()
                .sessionId(sessionId)
                .totalRecommendations(recommendations.size())
                .summaryAr("رتّبنا المحاصيل حسب مطابقة ظروفك وطقس منطقتك. الاقتراحات تأخذ بعين الاعتبار الموسم الحالي ودرجة الحرارة والمنطقة المناخية.")
                .summaryEn("We ranked crops based on your conditions and local weather. Recommendations consider current season, temperature, and your climate zone.")
                .userConditionsSummary(conditionsSummary)
                .recommendations(recommendations)
                .weatherContext(weatherContext)
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

            List<Long> optionIds = answer.getSelectedOptionIds();
            if (optionIds == null || optionIds.isEmpty()) {
                throw new IllegalArgumentException(
                        "يجب اختيار خيار واحد على الأقل للسؤال: " + question.getQuestionTextAr());
            }

            // التحقق من أنو ما اختار أكثر من خيار لسؤال ما بيسمح
            if (Boolean.FALSE.equals(question.getAllowMultiple()) && optionIds.size() > 1) {
                throw new IllegalArgumentException(
                        "السؤال \"" + question.getQuestionTextAr() + "\" يسمح باختيار خيار واحد فقط");
            }

            // التحقق من صحة كل الخيارات المختارة
            for (Long optionId : optionIds) {
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
     * حساب الاقتراحات مع مراعاة الطقس والموسم والمنطقة المناخية
     * 
     * الخوارزمية المحسّنة:
     * 1. الأساس: نقاط من PlantSuitability (إجابات اليوزر)
     * 2. مكافأة الموسم: +8% إذا النبتة موجودة في جدول MonthPlant للشهر الحالي
     * 3. مكافأة الحرارة: +7% إذا درجة الحرارة ضمن مدى النبتة (minTemp-maxTemp)
     *                     -10% إذا الحرارة خارج المدى تماماً
     * 4. مكافأة المنطقة: +5% للنباتات المناسبة للمنطقة المناخية
     */
    private List<RecommendationListResponse.RecommendedPlant> calculateRecommendationsWithWeather(
            User user, String sessionId, List<Long> selectedOptionIds, WeatherResponse weather, String climateZone) {
        
        // حساب الاقتراحات الأساسية
        List<RecommendationListResponse.RecommendedPlant> recommendations = 
                calculateRecommendations(user, sessionId, selectedOptionIds);
        
        // الشهر الحالي (للبحث في month_plants)
        int currentMonth = LocalDate.now().getMonthValue();
        
        // جلب كل النباتات المناسبة للشهر الحالي من MonthPlant
        Set<Long> monthPlantIds = getPlantIdsForMonth(currentMonth);
        log.info("📅 Month {} has {} plants in MonthPlant table", currentMonth, monthPlantIds.size());
        
        // تطبيق المكافآت الذكية
        return recommendations.stream()
                .map(rec -> applySmartWeatherBonus(rec, weather, climateZone, monthPlantIds))
                .sorted((a, b) -> Double.compare(b.getMatchPercentage(), a.getMatchPercentage()))
                .collect(Collectors.toList());
    }
    
    /**
     * جلب معرّفات النباتات المناسبة لشهر معين من جدول MonthPlant
     */
    private Set<Long> getPlantIdsForMonth(int monthNumber) {
        try {
            Optional<group.g.graduation.backend.common.model.Month> monthOpt = monthRepository.findByMonthNumber(monthNumber);
            if (monthOpt.isPresent()) {
                List<MonthPlant> monthPlants = monthPlantRepository.findByMonthId(monthOpt.get().getId());
                return monthPlants.stream()
                        .map(mp -> mp.getPlant().getId())
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to load month plants for month {}: {}", monthNumber, e.getMessage());
        }
        return Collections.emptySet();
    }
    
    /**
     * تطبيق مكافآت ذكية حسب الطقس والموسم والمنطقة
     * 
     * المكافآت:
     * - الموسم (MonthPlant):  +8%  إذا النبتة في جدول الشهر الحالي
     * - الحرارة:              +7%  إذا درجة الحرارة ضمن minTemp–maxTemp
     *                         -10% إذا الحرارة خارج المدى بأكثر من 10 درجات
     *                         -5%  إذا الحرارة خارج المدى بأقل من 10 درجات
     * - المنطقة المناخية:     +5%  للنباتات المناسبة لنوع المنطقة
     */
    private RecommendationListResponse.RecommendedPlant applySmartWeatherBonus(
            RecommendationListResponse.RecommendedPlant rec,
            WeatherResponse weather, String climateZone, Set<Long> monthPlantIds) {
        
        double currentMatch = rec.getMatchPercentage();
        double totalBonus = 0.0;
        boolean seasonalMatch = false;
        StringBuilder noteAr = new StringBuilder();
        StringBuilder noteEn = new StringBuilder();
        
        Plant plant = plantRepository.findById(rec.getPlantId()).orElse(null);
        if (plant == null) return rec;
        
        // ─── 1. مكافأة الموسم من MonthPlant (data-driven) ───
        if (monthPlantIds.contains(rec.getPlantId())) {
            totalBonus += 8.0;
            seasonalMatch = true;
            noteAr.append("✅ مناسب للزراعة في ").append(weather.getSeasonAr()).append(" • ");
            noteEn.append("✅ Suitable for planting in ").append(weather.getSeasonEn()).append(" • ");
            log.debug("🌿 Seasonal bonus +8% for {} (in MonthPlant for month)", rec.getNameEn());
        }
        
        // ─── 2. مكافأة/عقوبة الحرارة ───
        if (weather.getTemperature() != null && plant.getMinTemp() != null && plant.getMaxTemp() != null) {
            double temp = weather.getTemperature();
            int minT = plant.getMinTemp();
            int maxT = plant.getMaxTemp();
            
            if (temp >= minT && temp <= maxT) {
                // درجة الحرارة مثالية
                totalBonus += 7.0;
                noteAr.append("🌡️ الحرارة مناسبة (").append(String.format("%.0f", temp)).append("°م) • ");
                noteEn.append("🌡️ Temperature ideal (").append(String.format("%.0f", temp)).append("°C) • ");
                log.debug("🌡️ Temperature bonus +7% for {} ({}°C in range {}-{}°C)",
                        rec.getNameEn(), temp, minT, maxT);
            } else if (temp < minT - 10 || temp > maxT + 10) {
                // حرارة بعيدة جداً عن المدى
                totalBonus -= 10.0;
                noteAr.append("⚠️ الحرارة غير مناسبة (").append(String.format("%.0f", temp)).append("°م، المدى المثالي: ").append(minT).append("-").append(maxT).append("°م) • ");
                noteEn.append("⚠️ Temperature not ideal (").append(String.format("%.0f", temp)).append("°C, ideal: ").append(minT).append("-").append(maxT).append("°C) • ");
                log.debug("🌡️ Temperature penalty -10% for {} ({}°C far from {}-{}°C)",
                        rec.getNameEn(), temp, minT, maxT);
            } else {
                // حرارة قريبة من المدى
                totalBonus -= 5.0;
                noteAr.append("🌡️ الحرارة قريبة من المدى المناسب • ");
                noteEn.append("🌡️ Temperature near suitable range • ");
                log.debug("🌡️ Temperature mild penalty -5% for {} ({}°C near {}-{}°C)",
                        rec.getNameEn(), temp, minT, maxT);
            }
        }
        
        // ─── 3. مكافأة المنطقة المناخية ───
        double climateBonus = calculateClimateZoneBonus(plant, climateZone);
        if (climateBonus != 0) {
            totalBonus += climateBonus;
            if (climateBonus > 0) {
                noteAr.append("🏔️ مناسب لمنطقتك المناخية • ");
                noteEn.append("🏔️ Suited for your climate zone • ");
            }
        }
        
        // تطبيق المكافأة النهائية
        if (totalBonus != 0) {
            double newMatch = Math.max(5.0, Math.min(100.0, currentMatch + totalBonus));
            newMatch = Math.round(newMatch * 100.0) / 100.0;
            rec.setMatchPercentage(newMatch);
            rec.setMatchLevel(getMatchLevel(newMatch));
            rec.setMatchLevelAr(getMatchLevelAr(newMatch));
            rec.setNeedsAdjustment(newMatch < 80);
            
            log.debug("📊 Weather adjustment for {}: {}% → {}% (bonus={}%)",
                    rec.getNameEn(), currentMatch, newMatch, totalBonus);
        }
        
        // تعيين الملاحظات الموسمية
        rec.setSeasonalMatch(seasonalMatch);
        String noteArStr = noteAr.toString().trim();
        String noteEnStr = noteEn.toString().trim();
        if (noteArStr.endsWith("•")) noteArStr = noteArStr.substring(0, noteArStr.length() - 1).trim();
        if (noteEnStr.endsWith("•")) noteEnStr = noteEnStr.substring(0, noteEnStr.length() - 1).trim();
        rec.setSeasonalNoteAr(noteArStr.isEmpty() ? null : noteArStr);
        rec.setSeasonalNoteEn(noteEnStr.isEmpty() ? null : noteEnStr);
        
        return rec;
    }
    
    /**
     * حساب مكافأة المنطقة المناخية
     * تصنيف المناطق: coastal (ساحلي)، mountain (جبلي)، valley (غور/وادي)، desert (صحراوي)، inland (داخلي)
     */
    private double calculateClimateZoneBonus(Plant plant, String climateZone) {
        if (climateZone == null || plant.getNameEn() == null) return 0;
        
        String name = plant.getNameEn().toLowerCase();
        String scientific = plant.getNameScientific() != null ? plant.getNameScientific().toLowerCase() : "";
        
        return switch (climateZone) {
            case "coastal" -> {
                // المناطق الساحلية: رطوبة عالية، حرارة معتدلة
                // مناسب: ريحان، نعناع، بابونج
                if (name.contains("basil") || name.contains("mint") || name.contains("chamomile"))
                    yield 5.0;
                // غير مناسب: نباتات تحتاج جفاف شديد
                if (name.contains("cactus")) yield -3.0;
                yield 0;
            }
            case "mountain" -> {
                // المناطق الجبلية: بارد شتاءً، معتدل صيفاً
                // مناسب: زعتر، ميرمية، إكليل الجبل
                if (name.contains("thyme") || name.contains("sage") || name.contains("rosemary")
                        || scientific.contains("thymus") || scientific.contains("salvia"))
                    yield 5.0;
                // أقل مناسبة: نباتات استوائية
                if (name.contains("tropical") || name.contains("mango")) yield -5.0;
                yield 0;
            }
            case "valley" -> {
                // منطقة الأغوار: حار جداً، مدار السنة
                // مناسب: نباتات تتحمل الحرارة
                if (name.contains("date") || name.contains("pepper") || name.contains("tomato"))
                    yield 5.0;
                // أقل مناسبة: نباتات تحتاج برد
                if (name.contains("rosemary") || name.contains("sage")) yield -3.0;
                yield 0;
            }
            case "desert" -> {
                // المناطق الصحراوية: جاف جداً
                // مناسب: نباتات مقاومة الجفاف
                if (name.contains("rosemary") || name.contains("thyme") || name.contains("lavender"))
                    yield 5.0;
                // أقل مناسبة: نباتات تحتاج مياه كثيرة
                if (name.contains("basil") || name.contains("mint")) yield -3.0;
                yield 0;
            }
            default -> 0; // inland — لا مكافأة إضافية
        };
    }
    
    /**
     * تحديد المنطقة المناخية بناءً على الإحداثيات واسم المدينة
     * 
     * التصنيفات:
     * - coastal: مدن ساحلية (يافا، حيفا، عكا، غزة)
     * - mountain: مدن جبلية (نابلس، الخليل، رام الله، القدس، صفد، جنين)
     * - valley: منطقة الأغوار (أريحا، طوباس)
     * - desert: منطقة صحراوية (بئر السبع)
     * - inland: داخلي (باقي المدن)
     */
    private String determineClimateZone(Double latitude, Double longitude, String cityName) {
        if (cityName == null && latitude == null) return "inland";
        
        String city = cityName != null ? cityName.toLowerCase() : "";
        
        // تصنيف حسب اسم المدينة أولاً
        if (city.contains("jaffa") || city.contains("haifa") || city.contains("acre")
                || city.contains("gaza") || city.contains("khan") || city.contains("rafah")
                || city.contains("deir") || city.contains("beit hanoun") || city.contains("jabalia")
                || city.contains("يافا") || city.contains("حيفا") || city.contains("عكا")
                || city.contains("غزة") || city.contains("خان") || city.contains("رفح")) {
            return "coastal";
        }
        
        if (city.contains("jericho") || city.contains("أريحا")) {
            return "valley";
        }
        
        if (city.contains("beersheba") || city.contains("بئر السبع")) {
            return "desert";
        }
        
        if (city.contains("nablus") || city.contains("hebron") || city.contains("ramallah")
                || city.contains("jerusalem") || city.contains("safed") || city.contains("jenin")
                || city.contains("bethlehem") || city.contains("salfit") || city.contains("nazareth")
                || city.contains("umm al-fahm")
                || city.contains("نابلس") || city.contains("الخليل") || city.contains("رام الله")
                || city.contains("القدس") || city.contains("صفد") || city.contains("جنين")) {
            return "mountain";
        }
        
        // تصنيف حسب الإحداثيات إذا اسم المدينة غير معروف
        if (latitude != null && longitude != null) {
            // ساحلي: longitude < 34.8 (قريب من البحر)
            if (longitude < 34.8) return "coastal";
            // غور: أريحا وما حولها (lat ~31.8, lon > 35.4)
            if (longitude > 35.4 && latitude < 32.4 && latitude > 31.5) return "valley";
            // صحراوي: جنوب وجاف
            if (latitude < 31.3) return "desert";
            // جبلي: معظم الضفة الغربية
            if (longitude > 34.9 && longitude < 35.5 && latitude > 31.3) return "mountain";
        }
        
        return "inland";
    }
    
    /**
     * ترجمة المنطقة المناخية للعربي
     */
    private String translateClimateZone(String zone) {
        return switch (zone) {
            case "coastal" -> "ساحلي";
            case "mountain" -> "جبلي";
            case "valley" -> "غور / وادي";
            case "desert" -> "صحراوي";
            case "inland" -> "داخلي";
            default -> "غير محدد";
        };
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
