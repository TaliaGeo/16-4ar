package group.g.graduation.backend.user.service;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.util.SecurityUtils;
import group.g.graduation.backend.common.enums.PlantStatus;
import group.g.graduation.backend.common.enums.TaskStatus;
import group.g.graduation.backend.common.model.*;
import group.g.graduation.backend.common.repository.*;
import group.g.graduation.backend.user.dto.crop.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * خدمة صفحة محاصيلي - My Crops Service
 * تتعامل مع: المخططة، المزروعة، المحصودة، المهام، الري
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserMyCropsService {

    private final UserPlantRepository userPlantRepository;
    private final UserPlantTaskRepository userPlantTaskRepository;
    private final PlantTaskRepository plantTaskRepository;
    private final PlantImageRepository plantImageRepository;
    private final WateringHistoryRepository wateringHistoryRepository;
    private final MonthPlantRepository monthPlantRepository;
    private final group.g.graduation.backend.Security.repository.UserRepository userRepository;
    private final PlantRepository plantRepository;

    // =====================================================
    // ===== 0. إضافة نبتة مباشرة =====
    // =====================================================

    /**
     * إضافة نبتة للمخططة مباشرة بدون جلسة توصية
     */
    public UserPlantCardResponse addPlantToCrops(Long plantId, String nickname) {
        User user = getCurrentUser();
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new EntityNotFoundException("النبتة غير موجودة - ID: " + plantId));

        UserPlant up = new UserPlant();
        up.setUser(user);
        up.setPlant(plant);
        up.setStatus(PlantStatus.PLANNED);
        up.setPlannedDate(LocalDate.now());
        if (nickname != null && !nickname.isBlank()) {
            up.setNickname(nickname);
        }

        UserPlant saved = userPlantRepository.save(up);
        log.info("تمت إضافة النبتة {} للمخططة - UserPlant ID: {}", plant.getNameAr(), saved.getId());
        return toCard(saved);
    }

    // =====================================================
    // ===== 1. نظرة عامة على محاصيلي =====
    // =====================================================

    /**
     * جلب الأقسام الثلاثة: مخططة، مزروعة، محصودة
     */
    @Transactional(readOnly = true)
    public MyCropsOverviewResponse getMyCropsOverview() {
        User user = getCurrentUser();
        Long userId = user.getId();

        List<UserPlant> planned = userPlantRepository.findByUserIdAndStatusWithPlant(userId, PlantStatus.PLANNED);
        List<UserPlant> planted = userPlantRepository.findByUserIdAndStatusWithPlant(userId, PlantStatus.PLANTED);
        List<UserPlant> harvested = userPlantRepository.findByUserIdAndStatusWithPlant(userId, PlantStatus.HARVESTED);

        return MyCropsOverviewResponse.builder()
                .plannedCount(planned.size())
                .plantedCount(planted.size())
                .harvestedCount(harvested.size())
                .planned(planned.stream().map(this::toCard).collect(Collectors.toList()))
                .planted(planted.stream().map(this::toCard).collect(Collectors.toList()))
                .harvested(harvested.stream().map(this::toCard).collect(Collectors.toList()))
                .build();
    }

    /**
     * جلب النباتات بحسب الحالة
     */
    @Transactional(readOnly = true)
    public List<UserPlantCardResponse> getPlantsByStatus(PlantStatus status) {
        User user = getCurrentUser();
        List<UserPlant> plants = userPlantRepository.findByUserIdAndStatusWithPlant(user.getId(), status);
        return plants.stream().map(this::toCard).collect(Collectors.toList());
    }

    // =====================================================
    // ===== 2. تفاصيل نبتة مخططة =====
    // =====================================================

    /**
     * تفاصيل كاملة لنبتة مخطط لزراعتها (بدون خطوات الزراعة - لها إندبوينت لوحدها)
     */
    @Transactional(readOnly = true)
    public PlannedPlantDetailResponse getPlannedPlantDetail(Long userPlantId) {
        User user = getCurrentUser();
        UserPlant up = findUserPlantOwned(userPlantId, user.getId());
        Plant plant = up.getPlant();

        List<String> imageUrls = plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(plant.getId())
                .stream().map(PlantImage::getImageUrl).collect(Collectors.toList());
        String primaryImage = getPrimaryImageUrl(plant.getId(), imageUrls);

        return PlannedPlantDetailResponse.builder()
                .userPlantId(up.getId())
                .plantId(plant.getId())
                .plantNameAr(plant.getNameAr())
                .plantNameEn(plant.getNameEn())
                .nameScientific(plant.getNameScientific())
                .imageUrl(primaryImage)
                .allImageUrls(imageUrls)
                .lightInfoAr(plant.getLightInfoAr())
                .lightInfoEn(plant.getLightInfoEn())
                .soilInfoAr(plant.getSoilInfoAr())
                .soilInfoEn(plant.getSoilInfoEn())
                .wateringInfoAr(plant.getWateringInfoAr())
                .wateringInfoEn(plant.getWateringInfoEn())
                .careInfoAr(plant.getCareInfoAr())
                .careInfoEn(plant.getCareInfoEn())
                .difficultyLevel(plant.getDifficultyLevel() != null ? plant.getDifficultyLevel().name() : null)
                .category(plant.getCategory() != null ? plant.getCategory().name() : null)
                .wateringIntervalDays(plant.getWateringIntervalDays())
                .daysToHarvest(plant.getDaysToHarvest())
                .nickname(up.getNickname())
                .build();
    }

    // =====================================================
    // ===== 2.1 خطوات الزراعة (صفحة منفصلة) =====
    // =====================================================

    /**
     * خطوات زراعة النبتة - تظهر بصفحة لوحدها
     * يضغط المستخدم "ابدأ الزراعة" من تفاصيل المخططة → تفتح صفحة الخطوات
     */
    @Transactional(readOnly = true)
    public PlantingStepsResponse getPlantingSteps(Long userPlantId) {
        User user = getCurrentUser();
        UserPlant up = findUserPlantOwned(userPlantId, user.getId());
        Plant plant = up.getPlant();

        String primaryImage = getPrimaryImageUrl(plant.getId(),
                plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(plant.getId())
                        .stream().map(PlantImage::getImageUrl).collect(Collectors.toList()));

        return PlantingStepsResponse.builder()
                .userPlantId(up.getId())
                .plantId(plant.getId())
                .plantNameAr(plant.getNameAr())
                .plantNameEn(plant.getNameEn())
                .imageUrl(primaryImage)
                .plantingStepsAr(plant.getPlantingStepsAr())
                .plantingStepsEn(plant.getPlantingStepsEn())
                .plantingVideoUrl(plant.getPlantingVideoUrl())
                .difficultyLevel(plant.getDifficultyLevel() != null ? plant.getDifficultyLevel().name() : null)
                .spacingCm(plant.getSpacingCm())
                .germinationDays(plant.getGerminationDays())
                .minTemp(plant.getMinTemp())
                .maxTemp(plant.getMaxTemp())
                .nickname(up.getNickname())
                .build();
    }

    // =====================================================
    // ===== 3. قمت بزراعتها (مخطط → مزروع) =====
    // =====================================================

    /**
     * نقل نبتة من المخططة إلى المزروعة + إنشاء المهام
     */
    public UserPlantCardResponse markAsPlanted(MarkPlantedRequest request) {
        User user = getCurrentUser();
        UserPlant up = findUserPlantOwned(request.getUserPlantId(), user.getId());

        if (up.getStatus() != PlantStatus.PLANNED) {
            throw new IllegalStateException("هذه النبتة ليست في قسم المخططة، حالتها الحالية: " + up.getStatus());
        }

        up.setStatus(PlantStatus.PLANTED);
        up.setPlantedDate(LocalDate.now());
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            up.setNickname(request.getNickname());
        }

        UserPlant saved = userPlantRepository.save(up);

        // إنشاء المهام تلقائياً من قوالب النبتة
        generateTasksForPlant(saved);

        log.info("تم نقل النبتة {} للمزروعة - UserPlant ID: {}", up.getPlant().getNameAr(), saved.getId());
        return toCard(saved);
    }

    // =====================================================
    // ===== 4. نظرة عامة على نبتة مزروعة =====
    // =====================================================

    /**
     * التبويب الأول: نظرة عامة (اسم، تاريخ زراعة، آخر ري، الري القادم...)
     */
    @Transactional(readOnly = true)
    public PlantedOverviewResponse getPlantedOverview(Long userPlantId) {
        User user = getCurrentUser();
        UserPlant up = findUserPlantOwned(userPlantId, user.getId());
        Plant plant = up.getPlant();

        List<String> imageUrls = plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(plant.getId())
                .stream().map(PlantImage::getImageUrl).collect(Collectors.toList());
        String primaryImage = getPrimaryImageUrl(plant.getId(), imageUrls);

        // حساب آخر ري والري القادم
        String lastWatering = null;
        String lastWateringRelAr = null;
        String lastWateringRelEn = null;
        String nextWatering = null;
        String nextWateringRelAr = null;
        String nextWateringRelEn = null;

        var lastWh = wateringHistoryRepository.findFirstByUserPlantIdOrderByWateredAtDesc(up.getId());
        if (lastWh.isPresent()) {
            LocalDateTime wateredAt = lastWh.get().getWateredAt();
            lastWatering = wateredAt.toLocalDate().toString();
            long daysAgo = ChronoUnit.DAYS.between(wateredAt.toLocalDate(), LocalDate.now());
            lastWateringRelAr = formatRelativePastAr(daysAgo);
            lastWateringRelEn = formatRelativePastEn(daysAgo);

            if (plant.getWateringIntervalDays() != null) {
                LocalDate nextDate = wateredAt.toLocalDate().plusDays(plant.getWateringIntervalDays());
                nextWatering = nextDate.toString();
                long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), nextDate);
                nextWateringRelAr = formatRelativeFutureAr(daysUntil);
                nextWateringRelEn = formatRelativeFutureEn(daysUntil);
            }
        } else if (up.getPlantedDate() != null && plant.getWateringIntervalDays() != null) {
            LocalDate nextDate = up.getPlantedDate().plusDays(plant.getWateringIntervalDays());
            nextWatering = nextDate.toString();
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), nextDate);
            nextWateringRelAr = formatRelativeFutureAr(daysUntil);
            nextWateringRelEn = formatRelativeFutureEn(daysUntil);
        }

        int daysSince = up.getPlantedDate() != null
                ? (int) ChronoUnit.DAYS.between(up.getPlantedDate(), LocalDate.now()) : 0;

        return PlantedOverviewResponse.builder()
                .userPlantId(up.getId())
                .plantId(plant.getId())
                .plantNameAr(plant.getNameAr())
                .plantNameEn(plant.getNameEn())
                .nameScientific(plant.getNameScientific())
                .imageUrl(primaryImage)
                .allImageUrls(imageUrls)
                .nickname(up.getNickname())
                .status(up.getStatus().name())
                .welcomeMessageAr("هنا رح تتابع نمو نبتتك خطوة بخطوة، وتوصلك تنبيهات الري والعناية بوقتها!")
                .welcomeMessageEn("Track your plant's growth step by step, with timely watering and care reminders!")
                .plantedDate(up.getPlantedDate())
                .daysSincePlanting(daysSince)
                .daysSincePlantingTextAr("اليوم " + daysSince + " من الزراعة")
                .daysSincePlantingTextEn("Day " + daysSince + " since planting")
                .lastWateringDate(lastWatering)
                .lastWateringRelativeAr(lastWateringRelAr)
                .lastWateringRelativeEn(lastWateringRelEn)
                .nextWateringDate(nextWatering)
                .nextWateringRelativeAr(nextWateringRelAr)
                .nextWateringRelativeEn(nextWateringRelEn)
                .wateringIntervalDays(plant.getWateringIntervalDays())
                .difficultyLevel(plant.getDifficultyLevel() != null ? plant.getDifficultyLevel().name() : null)
                .category(plant.getCategory() != null ? plant.getCategory().name() : null)
                .build();
    }

    // =====================================================
    // ===== 5. مهام النبتة المزروعة =====
    // =====================================================

    /**
     * التبويب الثاني: المهام (إشعارات الري والتسميد والحصاد...)
     */
    @Transactional(readOnly = true)
    public PlantedTasksResponse getPlantedTasks(Long userPlantId) {
        User user = getCurrentUser();
        UserPlant up = findUserPlantOwned(userPlantId, user.getId());

        List<UserPlantTask> tasks = userPlantTaskRepository.findByUserPlantIdWithTaskType(userPlantId);
        LocalDate today = LocalDate.now();

        List<UserPlantTaskResponse> taskDtos = tasks.stream().map(t -> {
            boolean isDueToday = t.getDueDate().equals(today) && t.getStatus() != TaskStatus.COMPLETED;
            boolean isOverdue = t.getDueDate().isBefore(today) && t.getStatus() != TaskStatus.COMPLETED;

            String effectiveStatus;
            if (t.getStatus() == TaskStatus.COMPLETED) {
                effectiveStatus = "COMPLETED";
            } else if (isOverdue) {
                effectiveStatus = "OVERDUE";
            } else if (isDueToday) {
                effectiveStatus = "DUE_TODAY";
            } else if (t.getStatus() == TaskStatus.SNOOZED) {
                effectiveStatus = "SNOOZED";
            } else {
                effectiveStatus = "PENDING";
            }

            // حساب النص النسبي لتاريخ الاستحقاق
            long daysUntil = ChronoUnit.DAYS.between(today, t.getDueDate());
            String dueDateRelAr;
            String dueDateRelEn;
            String dueTimeDisplay;

            if (isOverdue) {
                long daysLate = Math.abs(daysUntil);
                dueDateRelAr = "متأخر" + (daysLate > 0 ? " بـ " + daysLate + " يوم" : "");
                dueDateRelEn = "Overdue" + (daysLate > 0 ? " by " + daysLate + " day(s)" : "");
                dueTimeDisplay = "متأخر";
            } else if (isDueToday) {
                dueDateRelAr = "مستحق اليوم";
                dueDateRelEn = "Due today";
                dueTimeDisplay = "مستو فوم";
            } else if (daysUntil == 1) {
                dueDateRelAr = "غداً";
                dueDateRelEn = "Tomorrow";
                dueTimeDisplay = "غداً";
            } else {
                dueDateRelAr = "بعد " + daysUntil + " أيام";
                dueDateRelEn = "In " + daysUntil + " days";
                dueTimeDisplay = "بعد " + daysUntil + " أيام";
            }

            if (t.getStatus() == TaskStatus.COMPLETED) {
                dueDateRelAr = "مكتملة";
                dueDateRelEn = "Completed";
                dueTimeDisplay = "تمت";
            }

            return UserPlantTaskResponse.builder()
                    .taskId(t.getId())
                    .taskNameAr(t.getTaskType().getNameAr())
                    .taskNameEn(t.getTaskType().getNameEn())
                    .icon(t.getTaskType().getIcon())
                    .descriptionAr(t.getDescriptionAr())
                    .descriptionEn(t.getDescriptionEn())
                    .dueDate(t.getDueDate())
                    .dueTimeDisplay(dueTimeDisplay)
                    .dueDateRelativeAr(dueDateRelAr)
                    .dueDateRelativeEn(dueDateRelEn)
                    .status(effectiveStatus)
                    .completedAt(t.getCompletedAt() != null ? t.getCompletedAt().toString() : null)
                    .isDueToday(isDueToday)
                    .isOverdue(isOverdue)
                    .build();
        }).collect(Collectors.toList());

        long pending = taskDtos.stream().filter(t -> "PENDING".equals(t.getStatus())).count();
        long dueToday = taskDtos.stream().filter(t -> "DUE_TODAY".equals(t.getStatus())).count();
        long overdue = taskDtos.stream().filter(t -> "OVERDUE".equals(t.getStatus())).count();
        long completed = taskDtos.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long snoozed = taskDtos.stream().filter(t -> "SNOOZED".equals(t.getStatus())).count();

        return PlantedTasksResponse.builder()
                .userPlantId(up.getId())
                .plantNameAr(up.getPlant().getNameAr())
                .plantNameEn(up.getPlant().getNameEn())
                .status(up.getStatus().name())
                .totalTasks(taskDtos.size())
                .pendingCount((int) pending)
                .dueTodayCount((int) dueToday)
                .overdueCount((int) overdue)
                .completedCount((int) completed)
                .snoozedCount((int) snoozed)
                .tasks(taskDtos)
                .build();
    }

    // =====================================================
    // ===== 6. إجراء على مهمة (تمت / ذكّرني غداً) =====
    // =====================================================

    /**
     * تنفيذ إجراء على مهمة: إتمام أو تأجيل ليوم
     */
    public UserPlantTaskResponse performTaskAction(TaskActionRequest request) {
        User user = getCurrentUser();

        UserPlantTask task = userPlantTaskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new EntityNotFoundException("المهمة غير موجودة - ID: " + request.getTaskId()));

        // التحقق أن المهمة تخص المستخدم
        if (!task.getUserPlant().getUser().getId().equals(user.getId())) {
            throw new SecurityException("لا تملك صلاحية على هذه المهمة");
        }

        LocalDate today = LocalDate.now();

        if (request.getAction() == TaskActionRequest.Action.COMPLETE) {
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());

            // إذا كانت مهمة ري، نسجل بسجل الري
            if ("Watering".equalsIgnoreCase(task.getTaskType().getNameEn()) ||
                    "ري".equals(task.getTaskType().getNameAr())) {
                WateringHistory wh = new WateringHistory();
                wh.setUserPlant(task.getUserPlant());
                wh.setWateredAt(LocalDateTime.now());
                wateringHistoryRepository.save(wh);
            }

            // إنشاء مهمة متكررة قادمة إذا كانت recurring
            scheduleNextRecurringTask(task);

        } else if (request.getAction() == TaskActionRequest.Action.SNOOZE_1_HOUR) {
            task.setStatus(TaskStatus.SNOOZED);
            task.setSnoozedUntil(LocalDateTime.now().plusHours(1));
            // الدو ديت يبقى نفسه - بس التذكير يتأخر ساعة
        } else if (request.getAction() == TaskActionRequest.Action.SNOOZE_TOMORROW) {
            task.setStatus(TaskStatus.SNOOZED);
            task.setSnoozedUntil(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0));
            task.setDueDate(today.plusDays(1));
        } else if (request.getAction() == TaskActionRequest.Action.SNOOZE_LATER) {
            task.setStatus(TaskStatus.SNOOZED);
            task.setSnoozedUntil(LocalDateTime.now().plusDays(3).withHour(9).withMinute(0));
            task.setDueDate(today.plusDays(3));
        }

        UserPlantTask saved = userPlantTaskRepository.save(task);

        boolean isDueToday = saved.getDueDate().equals(today) && saved.getStatus() != TaskStatus.COMPLETED;
        boolean isOverdue = saved.getDueDate().isBefore(today) && saved.getStatus() != TaskStatus.COMPLETED;

        // حساب النص النسبي لتاريخ الاستحقاق
        long daysUntil = ChronoUnit.DAYS.between(today, saved.getDueDate());
        String dueTimeDisplay;
        String dueDateRelAr;
        String dueDateRelEn;

        if (saved.getStatus() == TaskStatus.COMPLETED) {
            dueTimeDisplay = "تمت";
            dueDateRelAr = "مكتملة";
            dueDateRelEn = "Completed";
        } else if (isOverdue) {
            dueTimeDisplay = "متأخر";
            dueDateRelAr = "متأخر";
            dueDateRelEn = "Overdue";
        } else if (isDueToday) {
            dueTimeDisplay = "مستو فوم";
            dueDateRelAr = "مستحق اليوم";
            dueDateRelEn = "Due today";
        } else if (daysUntil == 1) {
            dueTimeDisplay = "غداً";
            dueDateRelAr = "غداً";
            dueDateRelEn = "Tomorrow";
        } else {
            dueTimeDisplay = "بعد " + daysUntil + " أيام";
            dueDateRelAr = "بعد " + daysUntil + " أيام";
            dueDateRelEn = "In " + daysUntil + " days";
        }

        return UserPlantTaskResponse.builder()
                .taskId(saved.getId())
                .taskNameAr(saved.getTaskType().getNameAr())
                .taskNameEn(saved.getTaskType().getNameEn())
                .icon(saved.getTaskType().getIcon())
                .descriptionAr(saved.getDescriptionAr())
                .descriptionEn(saved.getDescriptionEn())
                .dueDate(saved.getDueDate())
                .dueTimeDisplay(dueTimeDisplay)
                .dueDateRelativeAr(dueDateRelAr)
                .dueDateRelativeEn(dueDateRelEn)
                .status(saved.getStatus().name())
                .completedAt(saved.getCompletedAt() != null ? saved.getCompletedAt().toString() : null)
                .isDueToday(isDueToday)
                .isOverdue(isOverdue)
                .build();
    }

    // =====================================================
    // ===== 7. معلومات النبتة المزروعة =====
    // =====================================================

    /**
     * التبويب الثالث: معلومات (رعاية، حصاد، خطوات، أشهر زراعة...)
     */
    @Transactional(readOnly = true)
    public PlantedInfoResponse getPlantedInfo(Long userPlantId) {
        User user = getCurrentUser();
        UserPlant up = findUserPlantOwned(userPlantId, user.getId());
        Plant plant = up.getPlant();

        String primaryImage = getPrimaryImageUrl(plant.getId(), null);

        // أشهر الزراعة
        List<MonthPlant> monthPlants = monthPlantRepository.findByPlantId(plant.getId());
        List<PlantedInfoResponse.PlantingMonthInfo> months = monthPlants.stream().map(mp -> {
            Month m = mp.getMonth();
            return PlantedInfoResponse.PlantingMonthInfo.builder()
                    .monthNumber(m.getMonthNumber())
                    .monthNameAr(m.getNameAr())
                    .monthNameEn(m.getNameEn())
                    .plantingNoteAr(mp.getPlantingNoteAr())
                    .plantingNoteEn(mp.getPlantingNoteEn())
                    .build();
        }).collect(Collectors.toList());

        // حساب الوقت المتوقع للحصاد
        String expectedHarvestAr = null;
        String expectedHarvestEn = null;
        if (plant.getDaysToHarvest() != null && up.getPlantedDate() != null) {
            LocalDate harvestDate = up.getPlantedDate().plusDays(plant.getDaysToHarvest());
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), harvestDate);
            if (daysRemaining > 0) {
                expectedHarvestAr = "بعد " + daysRemaining + " يوم تقريباً";
                expectedHarvestEn = "Approximately in " + daysRemaining + " days";
            } else if (daysRemaining == 0) {
                expectedHarvestAr = "اليوم!";
                expectedHarvestEn = "Today!";
            } else {
                expectedHarvestAr = "جاهزة للحصاد";
                expectedHarvestEn = "Ready to harvest";
            }
        }

        // تحويل الاستخدامات إلى تاغات (قائمة)
        List<String> useTagsAr = splitToTags(plant.getUsesInfoAr());
        List<String> useTagsEn = splitToTags(plant.getUsesInfoEn());

        // حساب فترة الري بالأيام من قوالب المهام
        Integer wateringIntervalDays = null;
        List<PlantTask> templates = plantTaskRepository.findByPlantIdWithTaskType(plant.getId());
        for (PlantTask pt : templates) {
            if (pt.getTaskType() != null &&
                    ("Watering".equalsIgnoreCase(pt.getTaskType().getNameEn()) ||
                            "ري".equals(pt.getTaskType().getNameAr()))) {
                wateringIntervalDays = pt.getIntervalDays();
                break;
            }
        }

        // الوصف القصير: أول جملة من careInfoAr / careInfoEn
        String shortDescAr = extractFirstSentence(plant.getCareInfoAr());
        String shortDescEn = extractFirstSentence(plant.getCareInfoEn());

        return PlantedInfoResponse.builder()
                .userPlantId(up.getId())
                .plantId(plant.getId())
                .plantNameAr(plant.getNameAr())
                .plantNameEn(plant.getNameEn())
                .nameScientific(plant.getNameScientific())
                .imageUrl(primaryImage)
                // قسم 1: معلومات
                .shortDescriptionAr(shortDescAr)
                .shortDescriptionEn(shortDescEn)
                .lightInfoAr(plant.getLightInfoAr())
                .lightInfoEn(plant.getLightInfoEn())
                .soilInfoAr(plant.getSoilInfoAr())
                .soilInfoEn(plant.getSoilInfoEn())
                .wateringInfoAr(plant.getWateringInfoAr())
                .wateringInfoEn(plant.getWateringInfoEn())
                .difficultyLevel(plant.getDifficultyLevel() != null ? plant.getDifficultyLevel().name() : null)
                .category(plant.getCategory() != null ? plant.getCategory().name() : null)
                .wateringIntervalDays(wateringIntervalDays)
                .minTemp(plant.getMinTemp())
                .maxTemp(plant.getMaxTemp())
                // قسم 2: العناية
                .careInfoAr(plant.getCareInfoAr())
                .careInfoEn(plant.getCareInfoEn())
                .plantingStepsAr(plant.getPlantingStepsAr())
                .plantingStepsEn(plant.getPlantingStepsEn())
                .plantingVideoUrl(plant.getPlantingVideoUrl())
                .spacingCm(plant.getSpacingCm())
                .germinationDays(plant.getGerminationDays())
                // قسم 3: الحصاد
                .harvestInfoAr(plant.getHarvestInfoAr())
                .harvestInfoEn(plant.getHarvestInfoEn())
                .daysToHarvest(plant.getDaysToHarvest())
                .expectedHarvestDateAr(expectedHarvestAr)
                .expectedHarvestDateEn(expectedHarvestEn)
                // قسم 4: الاستخدامات
                .usesInfoAr(plant.getUsesInfoAr())
                .usesInfoEn(plant.getUsesInfoEn())
                .useTagsAr(useTagsAr)
                .useTagsEn(useTagsEn)
                // تنبيه
                .disclaimerAr("ملاحظة: هذه المعلومات للتثقيف فقط وليست بديلاً عن نصيحة متخصص زراعي.")
                .disclaimerEn("Note: This information is for educational purposes only and is not a substitute for professional agricultural advice.")
                // أشهر الزراعة
                .plantingMonths(months)
                .build();
    }

    // =====================================================
    // ===== 8. حصاد النبتة (مزروع → محصود) =====
    // =====================================================

    /**
     * نقل نبتة من المزروعة إلى المحصودة
     */
    public UserPlantCardResponse markAsHarvested(Long userPlantId) {
        User user = getCurrentUser();
        UserPlant up = findUserPlantOwned(userPlantId, user.getId());

        if (up.getStatus() != PlantStatus.PLANTED) {
            throw new IllegalStateException("هذه النبتة ليست في قسم المزروعة، حالتها الحالية: " + up.getStatus());
        }

        up.setStatus(PlantStatus.HARVESTED);
        up.setHarvestedDate(LocalDate.now());

        UserPlant saved = userPlantRepository.save(up);
        log.info("تم حصاد النبتة {} - UserPlant ID: {}", up.getPlant().getNameAr(), saved.getId());
        return toCard(saved);
    }

    // =====================================================
    // ===== Private Helper Methods =====
    // =====================================================

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new RuntimeException("المستخدم غير مسجل دخول"));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("المستخدم غير موجود"));
    }

    private UserPlant findUserPlantOwned(Long userPlantId, Long userId) {
        UserPlant up = userPlantRepository.findByIdWithPlant(userPlantId)
                .orElseThrow(() -> new EntityNotFoundException("النبتة غير موجودة - ID: " + userPlantId));
        if (!up.getUser().getId().equals(userId)) {
            throw new SecurityException("لا تملك صلاحية على هذه النبتة");
        }
        return up;
    }

    /**
     * تحويل UserPlant إلى كارد
     */
    private UserPlantCardResponse toCard(UserPlant up) {
        Plant plant = up.getPlant();
        String imageUrl = getPrimaryImageUrl(plant.getId(), null);
        int pendingTasks = 0;
        if (up.getStatus() == PlantStatus.PLANTED) {
            pendingTasks = (int) userPlantTaskRepository.findByUserPlantId(up.getId())
                    .stream()
                    .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                    .count();
        }

        return UserPlantCardResponse.builder()
                .userPlantId(up.getId())
                .plantId(plant.getId())
                .plantNameAr(plant.getNameAr())
                .plantNameEn(plant.getNameEn())
                .imageUrl(imageUrl)
                .nickname(up.getNickname())
                .status(up.getStatus().name())
                .plannedDate(up.getPlannedDate())
                .plantedDate(up.getPlantedDate())
                .pendingTasksCount(pendingTasks)
                .build();
    }

    /**
     * جلب رابط الصورة الرئيسية للنبتة
     */
    private String getPrimaryImageUrl(Long plantId, List<String> preloaded) {
        List<PlantImage> images = plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(plantId);
        if (images.isEmpty()) return null;
        return images.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsPrimary()))
                .findFirst()
                .map(PlantImage::getImageUrl)
                .orElse(images.get(0).getImageUrl());
    }

    /**
     * إنشاء مهام تلقائية للنبتة المزروعة من قوالب PlantTask
     */
    private void generateTasksForPlant(UserPlant userPlant) {
        List<PlantTask> templates = plantTaskRepository.findByPlantIdWithTaskType(userPlant.getPlant().getId());

        for (PlantTask template : templates) {
            LocalDate dueDate = userPlant.getPlantedDate();
            if (template.getStartDayAfterPlanting() != null) {
                dueDate = dueDate.plusDays(template.getStartDayAfterPlanting());
            }

            UserPlantTask task = new UserPlantTask();
            task.setUserPlant(userPlant);
            task.setTaskType(template.getTaskType());
            task.setDescriptionAr(template.getDescriptionAr());
            task.setDescriptionEn(template.getDescriptionEn());
            task.setDueDate(dueDate);
            task.setStatus(TaskStatus.PENDING);

            userPlantTaskRepository.save(task);
        }

        log.info("تم إنشاء {} مهمة للنبتة {}", templates.size(), userPlant.getPlant().getNameAr());
    }

    /**
     * جدولة المهمة المتكررة القادمة بعد إتمامها
     */
    private void scheduleNextRecurringTask(UserPlantTask completedTask) {
        // نجد قالب المهمة الأصلي
        List<PlantTask> templates = plantTaskRepository.findByPlantIdWithTaskType(
                completedTask.getUserPlant().getPlant().getId());

        PlantTask template = templates.stream()
                .filter(t -> t.getTaskType().getId().equals(completedTask.getTaskType().getId()))
                .findFirst()
                .orElse(null);

        if (template != null && Boolean.TRUE.equals(template.getIsRecurring()) && template.getIntervalDays() != null) {
            UserPlantTask nextTask = new UserPlantTask();
            nextTask.setUserPlant(completedTask.getUserPlant());
            nextTask.setTaskType(completedTask.getTaskType());
            nextTask.setDescriptionAr(completedTask.getDescriptionAr());
            nextTask.setDescriptionEn(completedTask.getDescriptionEn());
            nextTask.setDueDate(LocalDate.now().plusDays(template.getIntervalDays()));
            nextTask.setStatus(TaskStatus.PENDING);

            userPlantTaskRepository.save(nextTask);
            log.info("تم جدولة المهمة المتكررة القادمة: {} في {}",
                    completedTask.getTaskType().getNameAr(), nextTask.getDueDate());
        }
    }

    /**
     * تحويل نص الاستخدامات إلى قائمة تاغات (تقسيم بالسطر أو الفاصلة أو النقطة)
     */
    private List<String> splitToTags(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split("[،,\\n\\r]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * استخراج أول جملة من نص (حتى أول نقطة أو سطر جديد)
     */
    private String extractFirstSentence(String text) {
        if (text == null || text.isBlank()) return null;
        String trimmed = text.trim();
        int dotIdx = trimmed.indexOf('.');
        int nlIdx = trimmed.indexOf('\n');
        int endIdx = trimmed.length();
        if (dotIdx > 0) endIdx = Math.min(endIdx, dotIdx + 1);
        if (nlIdx > 0) endIdx = Math.min(endIdx, nlIdx);
        return trimmed.substring(0, endIdx).trim();
    }

    /**
     * تنسيق التاريخ النسبي بالعربية (ماضي)
     */
    private String formatRelativePastAr(long days) {
        if (days == 0) return "اليوم";
        if (days == 1) return "أمس";
        if (days == 2) return "قبل يومين";
        if (days <= 10) return "قبل " + days + " أيام";
        return "قبل " + days + " يوم";
    }

    /**
     * تنسيق التاريخ النسبي بالإنجليزية (ماضي)
     */
    private String formatRelativePastEn(long days) {
        if (days == 0) return "Today";
        if (days == 1) return "Yesterday";
        return days + " days ago";
    }

    /**
     * تنسيق التاريخ النسبي بالعربية (مستقبل)
     */
    private String formatRelativeFutureAr(long days) {
        if (days == 0) return "اليوم";
        if (days == 1) return "غداً";
        if (days == 2) return "بعد يومين";
        if (days <= 10) return "بعد " + days + " أيام";
        return "بعد " + days + " يوم";
    }

    /**
     * تنسيق التاريخ النسبي بالإنجليزية (مستقبل)
     */
    private String formatRelativeFutureEn(long days) {
        if (days == 0) return "Today";
        if (days == 1) return "Tomorrow";
        return "In " + days + " days";
    }
}
