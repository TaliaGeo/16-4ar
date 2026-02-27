package group.g.graduation.backend.user.service;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.common.email.EmailService;
import group.g.graduation.backend.common.enums.NotificationType;
import group.g.graduation.backend.common.enums.TaskStatus;
import group.g.graduation.backend.common.model.*;
import group.g.graduation.backend.common.repository.NotificationRepository;
import group.g.graduation.backend.common.repository.UserPlantTaskRepository;
import group.g.graduation.backend.common.repository.UserPreferenceRepository;
import group.g.graduation.backend.common.service.FcmService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlantCareReminderScheduler - مجدول تذكيرات العناية
 */
@ExtendWith(MockitoExtension.class)
class PlantCareReminderSchedulerTest {

    @InjectMocks
    private PlantCareReminderScheduler scheduler;

    @Mock private UserPlantTaskRepository taskRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private UserPreferenceRepository preferenceRepository;
    @Mock private EmailService emailService;
    @Mock private FcmService fcmService;

    private User testUser;
    private Plant testPlant;
    private UserPlant testUserPlant;
    private TaskType wateringType;
    private TaskType fertilizingType;
    private UserPlantTask wateringTask;
    private UserPreference testPreference;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@gharsih.ps");
        testUser.setFullName("Test User");

        testPlant = new Plant();
        testPlant.setId(10L);
        testPlant.setNameAr("نعناع");
        testPlant.setNameEn("Mint");

        testUserPlant = new UserPlant();
        testUserPlant.setId(100L);
        testUserPlant.setUser(testUser);
        testUserPlant.setPlant(testPlant);
        testUserPlant.setNickname("نعنوعتي");

        wateringType = new TaskType();
        wateringType.setId(1L);
        wateringType.setNameAr("ري");
        wateringType.setNameEn("Watering");

        fertilizingType = new TaskType();
        fertilizingType.setId(2L);
        fertilizingType.setNameAr("تسميد");
        fertilizingType.setNameEn("Fertilizing");

        wateringTask = new UserPlantTask();
        wateringTask.setId(200L);
        wateringTask.setUserPlant(testUserPlant);
        wateringTask.setTaskType(wateringType);
        wateringTask.setDueDate(LocalDate.now());
        wateringTask.setStatus(TaskStatus.PENDING);

        testPreference = new UserPreference();
        testPreference.setUser(testUser);
        testPreference.setNotificationEnabled(true);
        testPreference.setPushEnabled(true);
        testPreference.setLanguage("ar");
    }

    // =====================================================
    // ===== sendMorningReminders =====
    // =====================================================

    @Test
    @DisplayName("تذكير صباحي - يرسل إشعار + إيميل لمهمة ري مستحقة")
    void sendMorningReminders_wateringTask_sendsNotificationAndEmail() {
        when(taskRepository.findAllDueTasksForDate(any(LocalDate.class), eq(TaskStatus.PENDING)))
                .thenReturn(List.of(wateringTask));
        when(preferenceRepository.findByUserId(1L))
                .thenReturn(Optional.of(testPreference));

        scheduler.sendMorningReminders();

        // تحقق من تحديث الحالة
        assertEquals(TaskStatus.DUE_TODAY, wateringTask.getStatus());
        verify(taskRepository).save(wateringTask);

        // تحقق من إنشاء إشعار
        verify(notificationRepository).save(any(Notification.class));

        // تحقق من إرسال إيميل ري
        verify(emailService).sendWateringReminder(eq("test@gharsih.ps"), eq("Test User"), eq("Mint"));
    }

    @Test
    @DisplayName("تذكير صباحي - يرسل إيميل تسميد لمهمة تسميد")
    void sendMorningReminders_fertilizingTask_sendsFertilizingEmail() {
        UserPlantTask fertilizingTask = new UserPlantTask();
        fertilizingTask.setId(201L);
        fertilizingTask.setUserPlant(testUserPlant);
        fertilizingTask.setTaskType(fertilizingType);
        fertilizingTask.setDueDate(LocalDate.now());
        fertilizingTask.setStatus(TaskStatus.PENDING);

        when(taskRepository.findAllDueTasksForDate(any(LocalDate.class), eq(TaskStatus.PENDING)))
                .thenReturn(List.of(fertilizingTask));
        when(preferenceRepository.findByUserId(1L))
                .thenReturn(Optional.of(testPreference));

        scheduler.sendMorningReminders();

        verify(emailService).sendFertilizingReminder(eq("test@gharsih.ps"), eq("Test User"), eq("Mint"));
    }

    @Test
    @DisplayName("تذكير صباحي - لا يرسل إذا الإشعارات معطلة")
    void sendMorningReminders_notificationsDisabled_skipsUser() {
        testPreference.setNotificationEnabled(false);

        when(taskRepository.findAllDueTasksForDate(any(LocalDate.class), eq(TaskStatus.PENDING)))
                .thenReturn(List.of(wateringTask));
        when(preferenceRepository.findByUserId(1L))
                .thenReturn(Optional.of(testPreference));

        scheduler.sendMorningReminders();

        // لا يجب أن يتم إنشاء أي إشعار أو إرسال إيميل
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(emailService, never()).sendWateringReminder(any(), any(), any());
    }

    @Test
    @DisplayName("تذكير صباحي - لا مهام مستحقة = لا إشعارات")
    void sendMorningReminders_noTasksDue_doesNothing() {
        when(taskRepository.findAllDueTasksForDate(any(LocalDate.class), eq(TaskStatus.PENDING)))
                .thenReturn(Collections.emptyList());

        scheduler.sendMorningReminders();

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(emailService, never()).sendWateringReminder(any(), any(), any());
    }

    // =====================================================
    // ===== checkOverdueTasks =====
    // =====================================================

    @Test
    @DisplayName("فحص المتأخرة - يحدث الحالة + إشعار تحذيري")
    void checkOverdueTasks_overdueTask_updatesStatusAndNotifies() {
        wateringTask.setDueDate(LocalDate.now().minusDays(2));
        wateringTask.setStatus(TaskStatus.PENDING);

        when(taskRepository.findAllOverdueTasks(any(LocalDate.class), eq(TaskStatus.PENDING)))
                .thenReturn(List.of(wateringTask));
        when(taskRepository.findAllOverdueTasks(any(LocalDate.class), eq(TaskStatus.DUE_TODAY)))
                .thenReturn(Collections.emptyList());

        scheduler.checkOverdueTasks();

        assertEquals(TaskStatus.OVERDUE, wateringTask.getStatus());
        verify(taskRepository).save(wateringTask);
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.TASK_OVERDUE));
    }

    @Test
    @DisplayName("فحص المتأخرة - لا مهام متأخرة = لا تغييرات")
    void checkOverdueTasks_noOverdue_doesNothing() {
        when(taskRepository.findAllOverdueTasks(any(LocalDate.class), any(TaskStatus.class)))
                .thenReturn(Collections.emptyList());

        scheduler.checkOverdueTasks();

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    // =====================================================
    // ===== processScheduledNotifications =====
    // =====================================================

    @Test
    @DisplayName("معالجة المجدولة - يحدث isPushed و sentAt")
    void processScheduledNotifications_pendingNotifications_marksPushed() {
        Notification pendingNotif = new Notification();
        pendingNotif.setId(50L);
        pendingNotif.setUser(testUser);
        pendingNotif.setTitleAr("تذكير");
        pendingNotif.setIsPushed(false);
        pendingNotif.setScheduledAt(Instant.now().minusSeconds(60));

        when(notificationRepository.findScheduledNotificationsToSend(any(Instant.class)))
                .thenReturn(List.of(pendingNotif));
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(testPreference));

        scheduler.processScheduledNotifications();

        assertTrue(pendingNotif.getIsPushed());
        assertNotNull(pendingNotif.getSentAt());
        verify(notificationRepository).save(pendingNotif);
    }

    @Test
    @DisplayName("معالجة المجدولة - لا إشعارات معلقة = لا عمليات")
    void processScheduledNotifications_noPending_doesNothing() {
        when(notificationRepository.findScheduledNotificationsToSend(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        scheduler.processScheduledNotifications();

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    // =====================================================
    // ===== Multiple users grouping =====
    // =====================================================

    @Test
    @DisplayName("تذكير صباحي - يجمع مهام نفس المستخدم")
    void sendMorningReminders_multipleTasksSameUser_groupedCorrectly() {
        UserPlantTask task2 = new UserPlantTask();
        task2.setId(202L);
        task2.setUserPlant(testUserPlant); // نفس المستخدم
        task2.setTaskType(fertilizingType);
        task2.setDueDate(LocalDate.now());
        task2.setStatus(TaskStatus.PENDING);

        when(taskRepository.findAllDueTasksForDate(any(LocalDate.class), eq(TaskStatus.PENDING)))
                .thenReturn(List.of(wateringTask, task2));
        when(preferenceRepository.findByUserId(1L))
                .thenReturn(Optional.of(testPreference));

        scheduler.sendMorningReminders();

        // يجب إنشاء إشعارين
        verify(notificationRepository, times(2)).save(any(Notification.class));

        // يجب إرسال إيميل ري + إيميل تسميد
        verify(emailService).sendWateringReminder(any(), any(), any());
        verify(emailService).sendFertilizingReminder(any(), any(), any());
    }
}
