package group.g.graduation.backend.user.service;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.Security.repository.UserRepository;
import group.g.graduation.backend.Security.util.SecurityUtils;
import group.g.graduation.backend.common.enums.PlantStatus;
import group.g.graduation.backend.common.enums.TaskStatus;
import group.g.graduation.backend.common.model.*;
import group.g.graduation.backend.common.repository.*;
import group.g.graduation.backend.user.dto.crop.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserMyCropsService - صفحة محاصيلي
 * Tests: Overview, PlantsByStatus, PlannedDetail, PlantingSteps, MarkPlanted, PlantedOverview,
 *        PlantedTasks, PerformTaskAction (COMPLETE + 3 SNOOZE types),
 *        PlantedInfo, MarkHarvested
 */
@ExtendWith(MockitoExtension.class)
class UserMyCropsServiceTest {

    @InjectMocks
    private UserMyCropsService service;

    @Mock private UserPlantRepository userPlantRepository;
    @Mock private UserPlantTaskRepository userPlantTaskRepository;
    @Mock private PlantTaskRepository plantTaskRepository;
    @Mock private PlantImageRepository plantImageRepository;
    @Mock private WateringHistoryRepository wateringHistoryRepository;
    @Mock private MonthPlantRepository monthPlantRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlantRepository plantRepository;

    private User testUser;
    private Plant testPlant;
    private UserPlant plannedUserPlant;
    private UserPlant plantedUserPlant;
    private TaskType wateringTaskType;
    private TaskType fertilizingTaskType;

    // ===== مستخدم تجريبي ونباتات تجريبية =====
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        testPlant = new Plant();
        testPlant.setId(10L);
        testPlant.setNameAr("نعناع");
        testPlant.setNameEn("Mint");
        testPlant.setNameScientific("Mentha");
        testPlant.setLightInfoAr("ضوء شمس جزئي");
        testPlant.setLightInfoEn("Partial sunlight");
        testPlant.setSoilInfoAr("تربة خصبة");
        testPlant.setSoilInfoEn("Fertile soil");
        testPlant.setWateringInfoAr("ري معتدل");
        testPlant.setWateringInfoEn("Moderate watering");
        testPlant.setWateringIntervalDays(3);
        testPlant.setCareInfoAr("اعتني بالنبتة يومياً. تحتاج ضوء شمس.");
        testPlant.setCareInfoEn("Take care daily. Needs sunlight.");
        testPlant.setHarvestInfoAr("احصد بعد الإزهار");
        testPlant.setHarvestInfoEn("Harvest after blooming");
        testPlant.setUsesInfoAr("شاي النعناع،تنكيه الطعام،طب شعبي");
        testPlant.setUsesInfoEn("Mint tea,Flavoring,Folk medicine");
        testPlant.setPlantingStepsAr("ازرع في تربة رطبة");
        testPlant.setPlantingStepsEn("Plant in moist soil");
        testPlant.setDaysToHarvest(60);
        testPlant.setSpacingCm(20);
        testPlant.setGerminationDays(7);
        testPlant.setMinTemp(15);
        testPlant.setMaxTemp(35);
        testPlant.setImages(new ArrayList<>());
        testPlant.setTasks(new ArrayList<>());
        testPlant.setMonthPlants(new ArrayList<>());

        plannedUserPlant = new UserPlant();
        plannedUserPlant.setId(100L);
        plannedUserPlant.setUser(testUser);
        plannedUserPlant.setPlant(testPlant);
        plannedUserPlant.setStatus(PlantStatus.PLANNED);
        plannedUserPlant.setPlannedDate(LocalDate.now().minusDays(5));

        plantedUserPlant = new UserPlant();
        plantedUserPlant.setId(101L);
        plantedUserPlant.setUser(testUser);
        plantedUserPlant.setPlant(testPlant);
        plantedUserPlant.setStatus(PlantStatus.PLANTED);
        plantedUserPlant.setPlantedDate(LocalDate.now().minusDays(10));
        plantedUserPlant.setNickname("نعنوعتي");

        wateringTaskType = new TaskType();
        wateringTaskType.setId(1L);
        wateringTaskType.setNameAr("ري");
        wateringTaskType.setNameEn("Watering");
        wateringTaskType.setIcon("💧");

        fertilizingTaskType = new TaskType();
        fertilizingTaskType.setId(2L);
        fertilizingTaskType.setNameAr("تسميد");
        fertilizingTaskType.setNameEn("Fertilizing");
        fertilizingTaskType.setIcon("🌱");
    }

    // =====================================================
    // ===== 1. getMyCropsOverview =====
    // =====================================================

    @Test
    @DisplayName("نظرة عامة - ترجع الأقسام الثلاثة بأعدادها")
    void getMyCropsOverview_returnsThreeSections() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            when(userPlantRepository.findByUserIdAndStatusWithPlant(1L, PlantStatus.PLANNED))
                    .thenReturn(List.of(plannedUserPlant));
            when(userPlantRepository.findByUserIdAndStatusWithPlant(1L, PlantStatus.PLANTED))
                    .thenReturn(List.of(plantedUserPlant));
            when(userPlantRepository.findByUserIdAndStatusWithPlant(1L, PlantStatus.HARVESTED))
                    .thenReturn(Collections.emptyList());

            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(anyLong()))
                    .thenReturn(Collections.emptyList());
            when(userPlantTaskRepository.findByUserPlantId(anyLong()))
                    .thenReturn(Collections.emptyList());

            MyCropsOverviewResponse result = service.getMyCropsOverview();

            assertEquals(1, result.getPlannedCount());
            assertEquals(1, result.getPlantedCount());
            assertEquals(0, result.getHarvestedCount());
            assertEquals(1, result.getPlanned().size());
            assertEquals(1, result.getPlanted().size());
            assertEquals(0, result.getHarvested().size());
        }
    }

    @Test
    @DisplayName("نظرة عامة - مستخدم جديد بدون نباتات")
    void getMyCropsOverview_emptyUser_returnsZeros() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            when(userPlantRepository.findByUserIdAndStatusWithPlant(eq(1L), any()))
                    .thenReturn(Collections.emptyList());

            MyCropsOverviewResponse result = service.getMyCropsOverview();

            assertEquals(0, result.getPlannedCount());
            assertEquals(0, result.getPlantedCount());
            assertEquals(0, result.getHarvestedCount());
        }
    }

    // =====================================================
    // ===== 2. getPlantsByStatus =====
    // =====================================================

    @Test
    @DisplayName("جلب المخططة - ترجع قائمة كاردات")
    void getPlantsByStatus_planned_returnsCards() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByUserIdAndStatusWithPlant(1L, PlantStatus.PLANNED))
                    .thenReturn(List.of(plannedUserPlant));
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(anyLong()))
                    .thenReturn(Collections.emptyList());

            List<UserPlantCardResponse> result = service.getPlantsByStatus(PlantStatus.PLANNED);

            assertEquals(1, result.size());
            assertEquals("PLANNED", result.get(0).getStatus());
            assertEquals("نعناع", result.get(0).getPlantNameAr());
        }
    }

    // =====================================================
    // ===== 3. getPlannedPlantDetail =====
    // =====================================================

    @Test
    @DisplayName("تفاصيل مخطط - ترجع كل معلومات النبتة")
    void getPlannedPlantDetail_returnsFullDetails() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(100L)).thenReturn(Optional.of(plannedUserPlant));
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(10L))
                    .thenReturn(Collections.emptyList());

            PlannedPlantDetailResponse result = service.getPlannedPlantDetail(100L);

            assertEquals(100L, result.getUserPlantId());
            assertEquals("نعناع", result.getPlantNameAr());
            assertEquals("Mint", result.getPlantNameEn());
            assertEquals("Mentha", result.getNameScientific());
            assertEquals("ضوء شمس جزئي", result.getLightInfoAr());
            assertEquals("تربة خصبة", result.getSoilInfoAr());
            assertEquals(3, result.getWateringIntervalDays());
            assertEquals(60, result.getDaysToHarvest());
        }
    }

    @Test
    @DisplayName("تفاصيل مخطط - نبتة غير موجودة ترجع 404")
    void getPlannedPlantDetail_notFound_throwsException() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.getPlannedPlantDetail(999L));
        }
    }

    @Test
    @DisplayName("تفاصيل مخطط - نبتة ليست للمستخدم ترجع SecurityException")
    void getPlannedPlantDetail_notOwned_throwsSecurityException() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            User otherUser = new User();
            otherUser.setId(99L);
            UserPlant otherPlant = new UserPlant();
            otherPlant.setId(200L);
            otherPlant.setUser(otherUser);
            otherPlant.setPlant(testPlant);
            when(userPlantRepository.findByIdWithPlant(200L)).thenReturn(Optional.of(otherPlant));

            assertThrows(SecurityException.class, () -> service.getPlannedPlantDetail(200L));
        }
    }

    // =====================================================
    // ===== 3.1 getPlantingSteps (صفحة منفصلة) =====
    // =====================================================

    @Test
    @DisplayName("خطوات الزراعة - ترجع الخطوات والفيديو والمعلومات التقنية")
    void getPlantingSteps_returnsStepsAndTechnicalInfo() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(100L)).thenReturn(Optional.of(plannedUserPlant));
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(10L))
                    .thenReturn(Collections.emptyList());

            PlantingStepsResponse result = service.getPlantingSteps(100L);

            assertEquals(100L, result.getUserPlantId());
            assertEquals(10L, result.getPlantId());
            assertEquals("نعناع", result.getPlantNameAr());
            assertEquals("Mint", result.getPlantNameEn());
            assertEquals("ازرع في تربة رطبة", result.getPlantingStepsAr());
            assertEquals("Plant in moist soil", result.getPlantingStepsEn());
            assertEquals(20, result.getSpacingCm());
            assertEquals(7, result.getGerminationDays());
            assertEquals(15, result.getMinTemp());
            assertEquals(35, result.getMaxTemp());
        }
    }

    @Test
    @DisplayName("خطوات الزراعة - نبتة غير موجودة ترجع 404")
    void getPlantingSteps_notFound_throwsException() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.getPlantingSteps(999L));
        }
    }

    @Test
    @DisplayName("خطوات الزراعة - نبتة ليست للمستخدم ترجع SecurityException")
    void getPlantingSteps_notOwned_throwsSecurityException() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            User otherUser = new User();
            otherUser.setId(99L);
            UserPlant otherPlant = new UserPlant();
            otherPlant.setId(200L);
            otherPlant.setUser(otherUser);
            otherPlant.setPlant(testPlant);
            when(userPlantRepository.findByIdWithPlant(200L)).thenReturn(Optional.of(otherPlant));

            assertThrows(SecurityException.class, () -> service.getPlantingSteps(200L));
        }
    }

    // =====================================================
    // ===== 4. markAsPlanted =====
    // =====================================================

    @Test
    @DisplayName("زراعة نبتة - تتحول من PLANNED إلى PLANTED")
    void markAsPlanted_changesStatusToPlanted() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(100L)).thenReturn(Optional.of(plannedUserPlant));
            when(userPlantRepository.save(any(UserPlant.class))).thenAnswer(inv -> inv.getArgument(0));
            when(plantTaskRepository.findByPlantIdWithTaskType(10L)).thenReturn(Collections.emptyList());
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(anyLong()))
                    .thenReturn(Collections.emptyList());
            when(userPlantTaskRepository.findByUserPlantId(anyLong()))
                    .thenReturn(Collections.emptyList());

            MarkPlantedRequest request = new MarkPlantedRequest();
            request.setUserPlantId(100L);
            request.setNickname("نعنوعتي");

            UserPlantCardResponse result = service.markAsPlanted(request);

            assertEquals("PLANTED", result.getStatus());
            assertEquals(LocalDate.now(), result.getPlantedDate());
            verify(userPlantRepository).save(any(UserPlant.class));
        }
    }

    @Test
    @DisplayName("زراعة نبتة - تنشئ مهام تلقائية من القوالب")
    void markAsPlanted_generatesTasksFromTemplates() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(100L)).thenReturn(Optional.of(plannedUserPlant));
            when(userPlantRepository.save(any(UserPlant.class))).thenAnswer(inv -> inv.getArgument(0));

            PlantTask wateringTemplate = new PlantTask();
            wateringTemplate.setId(1L);
            wateringTemplate.setPlant(testPlant);
            wateringTemplate.setTaskType(wateringTaskType);
            wateringTemplate.setDescriptionAr("اسقِ النبتة");
            wateringTemplate.setDescriptionEn("Water the plant");
            wateringTemplate.setStartDayAfterPlanting(0);
            wateringTemplate.setIsRecurring(true);
            wateringTemplate.setIntervalDays(3);

            PlantTask fertilizingTemplate = new PlantTask();
            fertilizingTemplate.setId(2L);
            fertilizingTemplate.setPlant(testPlant);
            fertilizingTemplate.setTaskType(fertilizingTaskType);
            fertilizingTemplate.setDescriptionAr("سمّد النبتة");
            fertilizingTemplate.setDescriptionEn("Fertilize the plant");
            fertilizingTemplate.setStartDayAfterPlanting(7);
            fertilizingTemplate.setIsRecurring(true);
            fertilizingTemplate.setIntervalDays(14);

            when(plantTaskRepository.findByPlantIdWithTaskType(10L))
                    .thenReturn(List.of(wateringTemplate, fertilizingTemplate));
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(anyLong()))
                    .thenReturn(Collections.emptyList());
            when(userPlantTaskRepository.findByUserPlantId(anyLong()))
                    .thenReturn(Collections.emptyList());

            MarkPlantedRequest request = new MarkPlantedRequest();
            request.setUserPlantId(100L);

            service.markAsPlanted(request);

            // يجب إنشاء مهمتين
            verify(userPlantTaskRepository, times(2)).save(any(UserPlantTask.class));
        }
    }

    @Test
    @DisplayName("زراعة نبتة مزروعة مسبقاً - ترجع خطأ")
    void markAsPlanted_alreadyPlanted_throwsIllegalState() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(101L)).thenReturn(Optional.of(plantedUserPlant));

            MarkPlantedRequest request = new MarkPlantedRequest();
            request.setUserPlantId(101L);

            assertThrows(IllegalStateException.class, () -> service.markAsPlanted(request));
        }
    }

    // =====================================================
    // ===== 5. getPlantedOverview =====
    // =====================================================

    @Test
    @DisplayName("نظرة عامة مزروعة - ترجع بيانات صحيحة مع رسالة ترحيب")
    void getPlantedOverview_returnsCorrectData() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(101L)).thenReturn(Optional.of(plantedUserPlant));
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(10L))
                    .thenReturn(Collections.emptyList());
            when(wateringHistoryRepository.findFirstByUserPlantIdOrderByWateredAtDesc(101L))
                    .thenReturn(Optional.empty());

            PlantedOverviewResponse result = service.getPlantedOverview(101L);

            assertEquals(101L, result.getUserPlantId());
            assertEquals("نعناع", result.getPlantNameAr());
            assertEquals("PLANTED", result.getStatus());
            assertNotNull(result.getWelcomeMessageAr());
            assertNotNull(result.getWelcomeMessageEn());
            assertEquals(10, result.getDaysSincePlanting());
            assertTrue(result.getDaysSincePlantingTextAr().contains("10"));
            assertTrue(result.getDaysSincePlantingTextEn().contains("10"));
            assertEquals("نعنوعتي", result.getNickname());
        }
    }

    @Test
    @DisplayName("نظرة عامة مزروعة - مع سجل ري سابق ترجع آخر ري والقادم")
    void getPlantedOverview_withWateringHistory_returnsRelativeDates() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(101L)).thenReturn(Optional.of(plantedUserPlant));
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(10L))
                    .thenReturn(Collections.emptyList());

            WateringHistory lastWatering = new WateringHistory();
            lastWatering.setUserPlant(plantedUserPlant);
            lastWatering.setWateredAt(LocalDateTime.now().minusDays(2));
            when(wateringHistoryRepository.findFirstByUserPlantIdOrderByWateredAtDesc(101L))
                    .thenReturn(Optional.of(lastWatering));

            PlantedOverviewResponse result = service.getPlantedOverview(101L);

            assertNotNull(result.getLastWateringDate());
            assertEquals("قبل يومين", result.getLastWateringRelativeAr());
            assertEquals("2 days ago", result.getLastWateringRelativeEn());
            assertNotNull(result.getNextWateringDate());
            assertNotNull(result.getNextWateringRelativeAr());
        }
    }

    // =====================================================
    // ===== 6. getPlantedTasks =====
    // =====================================================

    @Test
    @DisplayName("مهام المزروعة - ترجع المهام مع الأعداد والحالات")
    void getPlantedTasks_returnsTasksWithCounts() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(101L)).thenReturn(Optional.of(plantedUserPlant));

            UserPlantTask pendingTask = createTask(1L, wateringTaskType, LocalDate.now().plusDays(3), TaskStatus.PENDING);
            UserPlantTask dueTodayTask = createTask(2L, fertilizingTaskType, LocalDate.now(), TaskStatus.PENDING);
            UserPlantTask overdueTask = createTask(3L, wateringTaskType, LocalDate.now().minusDays(2), TaskStatus.PENDING);
            UserPlantTask completedTask = createTask(4L, fertilizingTaskType, LocalDate.now().minusDays(1), TaskStatus.COMPLETED);

            when(userPlantTaskRepository.findByUserPlantIdWithTaskType(101L))
                    .thenReturn(List.of(pendingTask, dueTodayTask, overdueTask, completedTask));

            PlantedTasksResponse result = service.getPlantedTasks(101L);

            assertEquals(101L, result.getUserPlantId());
            assertEquals("PLANTED", result.getStatus());
            assertEquals(4, result.getTotalTasks());
            assertEquals(1, result.getPendingCount());
            assertEquals(1, result.getDueTodayCount());
            assertEquals(1, result.getOverdueCount());
            assertEquals(1, result.getCompletedCount());
            assertEquals(0, result.getSnoozedCount());
            assertEquals(4, result.getTasks().size());
        }
    }

    @Test
    @DisplayName("مهام المزروعة - التواريخ النسبية صحيحة")
    void getPlantedTasks_relativeDatesAreCorrect() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(101L)).thenReturn(Optional.of(plantedUserPlant));

            UserPlantTask dueTomorrow = createTask(1L, wateringTaskType, LocalDate.now().plusDays(1), TaskStatus.PENDING);
            UserPlantTask dueToday = createTask(2L, fertilizingTaskType, LocalDate.now(), TaskStatus.PENDING);
            UserPlantTask overdue = createTask(3L, wateringTaskType, LocalDate.now().minusDays(3), TaskStatus.PENDING);
            UserPlantTask completed = createTask(4L, fertilizingTaskType, LocalDate.now(), TaskStatus.COMPLETED);

            when(userPlantTaskRepository.findByUserPlantIdWithTaskType(101L))
                    .thenReturn(List.of(dueTomorrow, dueToday, overdue, completed));

            PlantedTasksResponse result = service.getPlantedTasks(101L);

            // غداً
            UserPlantTaskResponse tomorrowDto = result.getTasks().stream()
                    .filter(t -> t.getTaskId().equals(1L)).findFirst().orElseThrow();
            assertEquals("غداً", tomorrowDto.getDueDateRelativeAr());
            assertEquals("Tomorrow", tomorrowDto.getDueDateRelativeEn());
            assertEquals("PENDING", tomorrowDto.getStatus());

            // مستحق اليوم
            UserPlantTaskResponse todayDto = result.getTasks().stream()
                    .filter(t -> t.getTaskId().equals(2L)).findFirst().orElseThrow();
            assertEquals("مستحق اليوم", todayDto.getDueDateRelativeAr());
            assertEquals("DUE_TODAY", todayDto.getStatus());

            // متأخر
            UserPlantTaskResponse overdueDto = result.getTasks().stream()
                    .filter(t -> t.getTaskId().equals(3L)).findFirst().orElseThrow();
            assertTrue(overdueDto.getDueDateRelativeAr().contains("متأخر"));
            assertEquals("OVERDUE", overdueDto.getStatus());

            // مكتملة
            UserPlantTaskResponse completedDto = result.getTasks().stream()
                    .filter(t -> t.getTaskId().equals(4L)).findFirst().orElseThrow();
            assertEquals("تمت", completedDto.getDueTimeDisplay());
            assertEquals("COMPLETED", completedDto.getStatus());
        }
    }

    // =====================================================
    // ===== 7. performTaskAction =====
    // =====================================================

    @Test
    @DisplayName("إكمال مهمة - تتحول لـ COMPLETED")
    void performTaskAction_complete_changesStatusToCompleted() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            UserPlantTask task = createTask(1L, fertilizingTaskType, LocalDate.now(), TaskStatus.PENDING);
            task.setUserPlant(plantedUserPlant);
            when(userPlantTaskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(userPlantTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(plantTaskRepository.findByPlantIdWithTaskType(10L)).thenReturn(Collections.emptyList());

            TaskActionRequest request = new TaskActionRequest();
            request.setTaskId(1L);
            request.setAction(TaskActionRequest.Action.COMPLETE);

            UserPlantTaskResponse result = service.performTaskAction(request);

            assertEquals("COMPLETED", result.getStatus());
            assertNotNull(result.getCompletedAt());
            assertEquals("تمت", result.getDueTimeDisplay());
        }
    }

    @Test
    @DisplayName("إكمال مهمة ري - تسجل بسجل الري")
    void performTaskAction_completeWatering_logsWateringHistory() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            UserPlantTask task = createTask(1L, wateringTaskType, LocalDate.now(), TaskStatus.PENDING);
            task.setUserPlant(plantedUserPlant);
            when(userPlantTaskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(userPlantTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(plantTaskRepository.findByPlantIdWithTaskType(10L)).thenReturn(Collections.emptyList());

            TaskActionRequest request = new TaskActionRequest();
            request.setTaskId(1L);
            request.setAction(TaskActionRequest.Action.COMPLETE);

            service.performTaskAction(request);

            // التحقق من تسجيل الري
            verify(wateringHistoryRepository).save(any(WateringHistory.class));
        }
    }

    @Test
    @DisplayName("تأجيل مهمة ساعة - SNOOZE_1_HOUR")
    void performTaskAction_snooze1Hour_setsStatusSnoozed() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            UserPlantTask task = createTask(1L, wateringTaskType, LocalDate.now(), TaskStatus.PENDING);
            task.setUserPlant(plantedUserPlant);
            when(userPlantTaskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(userPlantTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TaskActionRequest request = new TaskActionRequest();
            request.setTaskId(1L);
            request.setAction(TaskActionRequest.Action.SNOOZE_1_HOUR);

            UserPlantTaskResponse result = service.performTaskAction(request);

            assertEquals("SNOOZED", result.getStatus());
            // التاريخ يبقى اليوم (الدو ديت ما يتغير)
            assertEquals(LocalDate.now(), result.getDueDate());
        }
    }

    @Test
    @DisplayName("تأجيل مهمة لبكرا - SNOOZE_TOMORROW")
    void performTaskAction_snoozeTomorrow_setsDueDateTomorrow() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            UserPlantTask task = createTask(1L, wateringTaskType, LocalDate.now(), TaskStatus.PENDING);
            task.setUserPlant(plantedUserPlant);
            when(userPlantTaskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(userPlantTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TaskActionRequest request = new TaskActionRequest();
            request.setTaskId(1L);
            request.setAction(TaskActionRequest.Action.SNOOZE_TOMORROW);

            UserPlantTaskResponse result = service.performTaskAction(request);

            assertEquals("SNOOZED", result.getStatus());
            assertEquals(LocalDate.now().plusDays(1), result.getDueDate());
            assertEquals("غداً", result.getDueDateRelativeAr());
            assertEquals("Tomorrow", result.getDueDateRelativeEn());
        }
    }

    @Test
    @DisplayName("تأجيل مهمة 3 أيام - SNOOZE_LATER")
    void performTaskAction_snoozeLater_setsDueDate3DaysLater() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            UserPlantTask task = createTask(1L, wateringTaskType, LocalDate.now(), TaskStatus.PENDING);
            task.setUserPlant(plantedUserPlant);
            when(userPlantTaskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(userPlantTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TaskActionRequest request = new TaskActionRequest();
            request.setTaskId(1L);
            request.setAction(TaskActionRequest.Action.SNOOZE_LATER);

            UserPlantTaskResponse result = service.performTaskAction(request);

            assertEquals("SNOOZED", result.getStatus());
            assertEquals(LocalDate.now().plusDays(3), result.getDueDate());
            assertTrue(result.getDueDateRelativeAr().contains("3"));
        }
    }

    @Test
    @DisplayName("إجراء على مهمة غير موجودة - ترجع 404")
    void performTaskAction_taskNotFound_throwsException() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantTaskRepository.findById(999L)).thenReturn(Optional.empty());

            TaskActionRequest request = new TaskActionRequest();
            request.setTaskId(999L);
            request.setAction(TaskActionRequest.Action.COMPLETE);

            assertThrows(EntityNotFoundException.class, () -> service.performTaskAction(request));
        }
    }

    @Test
    @DisplayName("إجراء على مهمة ليست للمستخدم - ترجع SecurityException")
    void performTaskAction_notOwned_throwsSecurityException() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            User otherUser = new User();
            otherUser.setId(99L);
            UserPlant otherUserPlant = new UserPlant();
            otherUserPlant.setUser(otherUser);

            UserPlantTask task = createTask(1L, wateringTaskType, LocalDate.now(), TaskStatus.PENDING);
            task.setUserPlant(otherUserPlant);
            when(userPlantTaskRepository.findById(1L)).thenReturn(Optional.of(task));

            TaskActionRequest request = new TaskActionRequest();
            request.setTaskId(1L);
            request.setAction(TaskActionRequest.Action.COMPLETE);

            assertThrows(SecurityException.class, () -> service.performTaskAction(request));
        }
    }

    @Test
    @DisplayName("إكمال مهمة متكررة - تجدول المهمة القادمة")
    void performTaskAction_completeRecurring_schedulesNextTask() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            UserPlantTask task = createTask(1L, wateringTaskType, LocalDate.now(), TaskStatus.PENDING);
            task.setUserPlant(plantedUserPlant);
            when(userPlantTaskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(userPlantTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlantTask recurringTemplate = new PlantTask();
            recurringTemplate.setTaskType(wateringTaskType);
            recurringTemplate.setIsRecurring(true);
            recurringTemplate.setIntervalDays(3);
            when(plantTaskRepository.findByPlantIdWithTaskType(10L))
                    .thenReturn(List.of(recurringTemplate));

            TaskActionRequest request = new TaskActionRequest();
            request.setTaskId(1L);
            request.setAction(TaskActionRequest.Action.COMPLETE);

            service.performTaskAction(request);

            // save يُستدعى مرتين: المهمة المكتملة + المهمة القادمة
            verify(userPlantTaskRepository, times(2)).save(any(UserPlantTask.class));
        }
    }

    // =====================================================
    // ===== 8. getPlantedInfo =====
    // =====================================================

    @Test
    @DisplayName("معلومات المزروعة - 4 أقسام صحيحة")
    void getPlantedInfo_returnsFourSections() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(101L)).thenReturn(Optional.of(plantedUserPlant));
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(10L))
                    .thenReturn(Collections.emptyList());
            when(monthPlantRepository.findByPlantId(10L)).thenReturn(Collections.emptyList());
            when(plantTaskRepository.findByPlantIdWithTaskType(10L)).thenReturn(Collections.emptyList());

            PlantedInfoResponse result = service.getPlantedInfo(101L);

            // القسم 1: معلومات
            assertEquals("نعناع", result.getPlantNameAr());
            assertNotNull(result.getShortDescriptionAr()); // أول جملة من careInfo
            assertEquals("ضوء شمس جزئي", result.getLightInfoAr());
            assertEquals("تربة خصبة", result.getSoilInfoAr());
            assertEquals("ري معتدل", result.getWateringInfoAr());
            assertEquals(15, result.getMinTemp());
            assertEquals(35, result.getMaxTemp());

            // القسم 2: العناية
            assertNotNull(result.getCareInfoAr());
            assertEquals("ازرع في تربة رطبة", result.getPlantingStepsAr());
            assertEquals(20, result.getSpacingCm());
            assertEquals(7, result.getGerminationDays());

            // القسم 3: الحصاد
            assertEquals("احصد بعد الإزهار", result.getHarvestInfoAr());
            assertEquals(60, result.getDaysToHarvest());
            assertNotNull(result.getExpectedHarvestDateAr()); // "بعد X يوم تقريباً"
            assertNotNull(result.getExpectedHarvestDateEn());

            // القسم 4: الاستخدامات
            assertEquals("شاي النعناع،تنكيه الطعام،طب شعبي", result.getUsesInfoAr());
            assertNotNull(result.getUseTagsAr());
            assertEquals(3, result.getUseTagsAr().size());
            assertTrue(result.getUseTagsAr().contains("شاي النعناع"));
            assertTrue(result.getUseTagsAr().contains("تنكيه الطعام"));
            assertTrue(result.getUseTagsAr().contains("طب شعبي"));

            // التنبيه
            assertNotNull(result.getDisclaimerAr());
            assertNotNull(result.getDisclaimerEn());
            assertTrue(result.getDisclaimerAr().contains("ملاحظة"));
        }
    }

    @Test
    @DisplayName("معلومات المزروعة - expectedHarvestDate محسوبة صحيح")
    void getPlantedInfo_expectedHarvestDateCalculation() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(101L)).thenReturn(Optional.of(plantedUserPlant));
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(10L))
                    .thenReturn(Collections.emptyList());
            when(monthPlantRepository.findByPlantId(10L)).thenReturn(Collections.emptyList());
            when(plantTaskRepository.findByPlantIdWithTaskType(10L)).thenReturn(Collections.emptyList());

            // تم زراعتها قبل 10 أيام + daysToHarvest = 60 → يبقى ~50 يوم
            PlantedInfoResponse result = service.getPlantedInfo(101L);

            assertTrue(result.getExpectedHarvestDateAr().contains("50"));
            assertTrue(result.getExpectedHarvestDateEn().contains("50"));
        }
    }

    @Test
    @DisplayName("معلومات المزروعة - مع أشهر زراعة")
    void getPlantedInfo_withPlantingMonths() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(101L)).thenReturn(Optional.of(plantedUserPlant));
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(10L))
                    .thenReturn(Collections.emptyList());
            when(plantTaskRepository.findByPlantIdWithTaskType(10L)).thenReturn(Collections.emptyList());

            Month march = new Month();
            march.setId(3L);
            march.setMonthNumber(3);
            march.setNameAr("مارس");
            march.setNameEn("March");

            MonthPlant mp = new MonthPlant();
            mp.setMonth(march);
            mp.setPlant(testPlant);
            mp.setPlantingNoteAr("موسم مثالي");
            mp.setPlantingNoteEn("Ideal season");

            when(monthPlantRepository.findByPlantId(10L)).thenReturn(List.of(mp));

            PlantedInfoResponse result = service.getPlantedInfo(101L);

            assertEquals(1, result.getPlantingMonths().size());
            assertEquals(3, result.getPlantingMonths().get(0).getMonthNumber());
            assertEquals("مارس", result.getPlantingMonths().get(0).getMonthNameAr());
            assertEquals("موسم مثالي", result.getPlantingMonths().get(0).getPlantingNoteAr());
        }
    }

    // =====================================================
    // ===== 9. markAsHarvested =====
    // =====================================================

    @Test
    @DisplayName("حصاد نبتة - تتحول من PLANTED إلى HARVESTED")
    void markAsHarvested_changesStatusToHarvested() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(101L)).thenReturn(Optional.of(plantedUserPlant));
            when(userPlantRepository.save(any(UserPlant.class))).thenAnswer(inv -> inv.getArgument(0));
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(anyLong()))
                    .thenReturn(Collections.emptyList());
            // Note: toCard() only queries tasks for PLANTED status, but markAsHarvested
            // sets status to HARVESTED before calling toCard(), so no task stub needed.

            UserPlantCardResponse result = service.markAsHarvested(101L);

            assertEquals("HARVESTED", result.getStatus());
            verify(userPlantRepository).save(any(UserPlant.class));
        }
    }

    @Test
    @DisplayName("حصاد نبتة مخططة - ترجع خطأ")
    void markAsHarvested_planned_throwsIllegalState() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByIdWithPlant(100L)).thenReturn(Optional.of(plannedUserPlant));

            assertThrows(IllegalStateException.class, () -> service.markAsHarvested(100L));
        }
    }

    // =====================================================
    // ===== 10. Image handling =====
    // =====================================================

    @Test
    @DisplayName("صورة أساسية - ترجع الرئيسية")
    void toCard_withImages_returnsPrimaryImage() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(userPlantRepository.findByUserIdAndStatusWithPlant(1L, PlantStatus.PLANNED))
                    .thenReturn(List.of(plannedUserPlant));

            PlantImage primaryImg = new PlantImage();
            primaryImg.setImageUrl("http://img.com/primary.jpg");
            primaryImg.setIsPrimary(true);
            primaryImg.setDisplayOrder(2);

            PlantImage otherImg = new PlantImage();
            otherImg.setImageUrl("http://img.com/other.jpg");
            otherImg.setIsPrimary(false);
            otherImg.setDisplayOrder(1);

            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(10L))
                    .thenReturn(List.of(otherImg, primaryImg));

            List<UserPlantCardResponse> result = service.getPlantsByStatus(PlantStatus.PLANNED);

            assertEquals("http://img.com/primary.jpg", result.get(0).getImageUrl());
        }
    }

    // =====================================================
    // ===== addPlantToCrops =====
    // =====================================================

    @Test
    @DisplayName("إضافة نبتة للمحاصيل - تنشئ UserPlant بحالة PLANNED")
    void addPlantToCrops_createsPlannedUserPlant() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(plantRepository.findById(10L)).thenReturn(Optional.of(testPlant));
            when(userPlantRepository.save(any(UserPlant.class))).thenAnswer(inv -> {
                UserPlant up = inv.getArgument(0);
                up.setId(200L);
                return up;
            });
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(10L))
                    .thenReturn(Collections.emptyList());

            UserPlantCardResponse result = service.addPlantToCrops(10L, "نعناع بلدي");

            assertNotNull(result);
            assertEquals(200L, result.getUserPlantId());
            assertEquals(10L, result.getPlantId());
            assertEquals("PLANNED", result.getStatus());
            assertEquals("نعناع بلدي", result.getNickname());
            assertEquals("نعناع", result.getPlantNameAr());
            assertEquals("Mint", result.getPlantNameEn());
            assertEquals(LocalDate.now(), result.getPlannedDate());
            verify(userPlantRepository).save(any(UserPlant.class));
        }
    }

    @Test
    @DisplayName("إضافة نبتة غير موجودة - ترمي EntityNotFoundException")
    void addPlantToCrops_plantNotFound_throwsException() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(plantRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.addPlantToCrops(999L, "test"));
        }
    }

    @Test
    @DisplayName("إضافة نبتة بدون لقب - تستخدم اسم النبتة كلقب")
    void addPlantToCrops_nullNickname_usesPlantName() {
        try (MockedStatic<SecurityUtils> secUtils = mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(Optional.of("test@example.com"));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(plantRepository.findById(10L)).thenReturn(Optional.of(testPlant));
            when(userPlantRepository.save(any(UserPlant.class))).thenAnswer(inv -> {
                UserPlant up = inv.getArgument(0);
                up.setId(201L);
                return up;
            });
            when(plantImageRepository.findByPlantIdOrderByDisplayOrderAsc(10L))
                    .thenReturn(Collections.emptyList());

            UserPlantCardResponse result = service.addPlantToCrops(10L, null);

            assertNull(result.getNickname());
        }
    }

    // =====================================================
    // ===== Helper Methods =====
    // =====================================================

    private UserPlantTask createTask(Long id, TaskType taskType, LocalDate dueDate, TaskStatus status) {
        UserPlantTask task = new UserPlantTask();
        task.setId(id);
        task.setUserPlant(plantedUserPlant);
        task.setTaskType(taskType);
        task.setDescriptionAr("وصف المهمة بالعربي");
        task.setDescriptionEn("Task description in English");
        task.setDueDate(dueDate);
        task.setStatus(status);
        if (status == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }
        return task;
    }
}
