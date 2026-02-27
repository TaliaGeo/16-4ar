package group.g.graduation.backend.user.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import group.g.graduation.backend.Security.model.User;
import group.g.graduation.backend.common.email.EmailService;
import group.g.graduation.backend.common.enums.NotificationType;
import group.g.graduation.backend.common.enums.TaskStatus;
import group.g.graduation.backend.common.model.Notification;
import group.g.graduation.backend.common.model.UserPlantTask;
import group.g.graduation.backend.common.model.UserPreference;
import group.g.graduation.backend.common.repository.NotificationRepository;
import group.g.graduation.backend.common.repository.UserPlantTaskRepository;
import group.g.graduation.backend.common.repository.UserPreferenceRepository;
import group.g.graduation.backend.common.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Plant Care Reminder Scheduler - مجدول تذكيرات العناية بالنباتات 🔔
 * 
 * يعمل تلقائياً كل يوم الصبح (7:00 صباحاً) ويقوم بـ:
 * 1. فحص كل المهام المستحقة اليوم (ري، تسميد، حصاد...)
 * 2. فحص المهام المتأخرة وتحديث حالتها
 * 3. إنشاء إشعارات داخلية (Notification) لكل مهمة
 * 4. إرسال إيميلات تذكير (للري والتسميد)
 * 
 * الجدول:
 * - كل يوم الساعة 7:00 صباحاً: فحص المهام المستحقة + إرسال تذكيرات
 * - كل يوم الساعة 12:00 ظهراً: فحص المهام المتأخرة + إشعار تحذيري
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlantCareReminderScheduler {

    private final UserPlantTaskRepository taskRepository;
    private final NotificationRepository notificationRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final EmailService emailService;
    private final FcmService fcmService;

    // ═══════════════════════════════════════════════════════
    // 1. تذكير صباحي - كل يوم الساعة 7:00 صباحاً
    // ═══════════════════════════════════════════════════════

    /**
     * يفحص المهام المستحقة اليوم ويرسل تذكيرات
     * Runs daily at 7:00 AM server time
     */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void sendMorningReminders() {
        LocalDate today = LocalDate.now();
        log.info("🔔 [Morning Reminder] Starting daily task check for {}", today);

        try {
            // جلب كل المهام المستحقة اليوم بحالة PENDING
            List<UserPlantTask> dueTasks = taskRepository.findAllDueTasksForDate(today, TaskStatus.PENDING);
            log.info("📋 Found {} tasks due today", dueTasks.size());

            if (dueTasks.isEmpty()) {
                log.info("✅ No tasks due today — nothing to send");
                return;
            }

            // تجميع المهام حسب اليوزر
            Map<Long, List<UserPlantTask>> tasksByUser = dueTasks.stream()
                    .collect(Collectors.groupingBy(t -> t.getUserPlant().getUser().getId()));

            int notificationsSent = 0;
            int emailsSent = 0;

            for (Map.Entry<Long, List<UserPlantTask>> entry : tasksByUser.entrySet()) {
                Long userId = entry.getKey();
                List<UserPlantTask> userTasks = entry.getValue();
                User user = userTasks.get(0).getUserPlant().getUser();

                // تحقق من تفضيلات اليوزر
                Optional<UserPreference> prefOpt = preferenceRepository.findByUserId(userId);
                boolean notificationsEnabled = prefOpt.map(p -> 
                    p.getNotificationEnabled() == null || p.getNotificationEnabled()
                ).orElse(true);

                if (!notificationsEnabled) {
                    log.debug("🔕 User {} has notifications disabled — skipping", userId);
                    continue;
                }

                for (UserPlantTask task : userTasks) {
                    // تحديث حالة المهمة إلى DUE_TODAY
                    task.setStatus(TaskStatus.DUE_TODAY);
                    taskRepository.save(task);

                    // إنشاء إشعار داخلي
                    createTaskNotification(user, task);
                    notificationsSent++;

                    // إرسال إيميل حسب نوع المهمة
                    if (user.getEmail() != null) {
                        sendTaskEmail(user, task);
                        emailsSent++;
                    }
                }
            }

            log.info("✅ [Morning Reminder] Done: {} notifications, {} emails for {} users",
                    notificationsSent, emailsSent, tasksByUser.size());

        } catch (Exception e) {
            log.error("❌ [Morning Reminder] Failed: {}", e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════
    // 2. فحص المهام المتأخرة - كل يوم الساعة 12:00 ظهراً
    // ═══════════════════════════════════════════════════════

    /**
     * يفحص المهام المتأخرة ويحدث حالتها ويرسل تحذيرات
     * Runs daily at 12:00 PM
     */
    @Scheduled(cron = "0 0 12 * * *")
    @Transactional
    public void checkOverdueTasks() {
        LocalDate today = LocalDate.now();
        log.info("⏰ [Overdue Check] Checking for overdue tasks before {}", today);

        try {
            // جلب المهام المتأخرة اللي لسا PENDING أو DUE_TODAY
            List<UserPlantTask> overduePending = taskRepository.findAllOverdueTasks(today, TaskStatus.PENDING);
            List<UserPlantTask> overdueDueToday = taskRepository.findAllOverdueTasks(today, TaskStatus.DUE_TODAY);

            List<UserPlantTask> allOverdue = new java.util.ArrayList<>(overduePending);
            allOverdue.addAll(overdueDueToday);

            log.info("⚠️ Found {} overdue tasks", allOverdue.size());

            int updated = 0;
            for (UserPlantTask task : allOverdue) {
                // تحديث الحالة إلى OVERDUE
                task.setStatus(TaskStatus.OVERDUE);
                taskRepository.save(task);

                // إشعار تحذيري
                User user = task.getUserPlant().getUser();
                createOverdueNotification(user, task);
                updated++;
            }

            log.info("✅ [Overdue Check] Updated {} tasks to OVERDUE status", updated);

        } catch (Exception e) {
            log.error("❌ [Overdue Check] Failed: {}", e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════
    // 3. معالجة الإشعارات المجدولة - كل 30 دقيقة
    // ═══════════════════════════════════════════════════════

    /**
     * يعالج الإشعارات اللي وصل وقتها (scheduledAt) بس ما انرسلت بعد
     */
    @Scheduled(fixedRate = 1800000) // كل 30 دقيقة
    @Transactional
    public void processScheduledNotifications() {
        try {
            List<Notification> pendingNotifications = notificationRepository
                    .findScheduledNotificationsToSend(Instant.now());

            if (pendingNotifications.isEmpty()) return;

            log.info("📬 Processing {} scheduled notifications", pendingNotifications.size());

            int pushed = 0;
            for (Notification notification : pendingNotifications) {
                // محاولة إرسال FCM push notification
                boolean sent = trySendFcmPush(notification);
                
                notification.setIsPushed(true);
                notification.setSentAt(Instant.now());
                notificationRepository.save(notification);
                
                if (sent) pushed++;
            }

            log.info("✅ Processed {} scheduled notifications ({} FCM pushed)", pendingNotifications.size(), pushed);

        } catch (Exception e) {
            log.error("❌ Failed to process scheduled notifications: {}", e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════

    /**
     * محاولة إرسال FCM push — يرجع true إذا نجح
     */
    private boolean trySendFcmPush(Notification notification) {
        try {
            User user = notification.getUser();
            Optional<UserPreference> prefOpt = preferenceRepository.findByUserId(user.getId());
            
            if (prefOpt.isEmpty()) return false;
            UserPreference pref = prefOpt.get();
            
            // التحقق أن Push مفعّل + يوجد FCM token
            if (!Boolean.TRUE.equals(pref.getPushEnabled())) return false;
            if (pref.getFcmToken() == null || pref.getFcmToken().isBlank()) return false;
            
            return fcmService.sendPushNotification(pref.getFcmToken(), notification);
        } catch (Exception e) {
            log.warn("⚠️ FCM push failed for notification #{}: {}", notification.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * إنشاء إشعار داخلي لمهمة مستحقة
     */
    private void createTaskNotification(User user, UserPlantTask task) {
        String plantName = task.getUserPlant().getPlant().getNameAr();
        String plantNameEn = task.getUserPlant().getPlant().getNameEn();
        String taskTypeName = task.getTaskType().getNameAr();
        String taskTypeNameEn = task.getTaskType().getNameEn();
        String nickname = task.getUserPlant().getNickname();

        String displayName = nickname != null ? nickname : plantName;
        String displayNameEn = nickname != null ? nickname : plantNameEn;

        // تحديد نوع الإشعار الصحيح حسب نوع المهمة
        NotificationType notifType = determineNotificationType(task.getTaskType().getNameEn());

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitleAr("تذكير: " + taskTypeName + " 🌱");
        notification.setTitleEn("Reminder: " + taskTypeNameEn + " 🌱");
        notification.setMessageAr("حان وقت " + taskTypeName + " لنبتتك \"" + displayName + "\" — لا تنسى! 💚");
        notification.setMessageEn("Time to " + taskTypeNameEn.toLowerCase() + " your plant \"" + displayNameEn + "\" — don't forget! 💚");
        notification.setType(notifType);
        notification.setUserPlant(task.getUserPlant());
        notification.setTask(task);
        notification.setIsRead(false);
        notification.setIsPushed(false);
        notification.setScheduledAt(Instant.now());

        notificationRepository.save(notification);
    }

    /**
     * إنشاء إشعار تحذيري لمهمة متأخرة
     */
    private void createOverdueNotification(User user, UserPlantTask task) {
        String plantName = task.getUserPlant().getPlant().getNameAr();
        String plantNameEn = task.getUserPlant().getPlant().getNameEn();
        String taskTypeName = task.getTaskType().getNameAr();
        String taskTypeNameEn = task.getTaskType().getNameEn();
        String nickname = task.getUserPlant().getNickname();

        String displayName = nickname != null ? nickname : plantName;
        String displayNameEn = nickname != null ? nickname : plantNameEn;

        long daysLate = LocalDate.now().toEpochDay() - task.getDueDate().toEpochDay();

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitleAr("⚠️ مهمة متأخرة: " + taskTypeName);
        notification.setTitleEn("⚠️ Overdue: " + taskTypeNameEn);
        notification.setMessageAr("نبتتك \"" + displayName + "\" تحتاج " + taskTypeName + " — متأخر " + daysLate + " يوم!");
        notification.setMessageEn("Your plant \"" + displayNameEn + "\" needs " + taskTypeNameEn.toLowerCase() + " — " + daysLate + " day(s) overdue!");
        notification.setType(NotificationType.TASK_OVERDUE);
        notification.setUserPlant(task.getUserPlant());
        notification.setTask(task);
        notification.setIsRead(false);
        notification.setIsPushed(false);
        notification.setScheduledAt(Instant.now());

        notificationRepository.save(notification);
    }

    /**
     * إرسال إيميل حسب نوع المهمة
     */
    private void sendTaskEmail(User user, UserPlantTask task) {
        try {
            String taskType = task.getTaskType().getNameEn();
            String plantName = task.getUserPlant().getPlant().getNameEn();
            String userName = user.getFullName() != null ? user.getFullName() : "User";

            if (taskType != null && taskType.toLowerCase().contains("water")) {
                emailService.sendWateringReminder(user.getEmail(), userName, plantName);
                log.debug("💧 Watering email sent to {} for {}", user.getEmail(), plantName);
            } else if (taskType != null && taskType.toLowerCase().contains("fertiliz")) {
                emailService.sendFertilizingReminder(user.getEmail(), userName, plantName);
                log.debug("🧪 Fertilizing email sent to {} for {}", user.getEmail(), plantName);
            }
            // أنواع أخرى (حصاد، تقليم...) يمكن إضافة templates لها لاحقاً
        } catch (Exception e) {
            log.warn("⚠️ Failed to send email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * تحديد نوع الإشعار حسب نوع المهمة
     */
    private NotificationType determineNotificationType(String taskTypeNameEn) {
        if (taskTypeNameEn == null) return NotificationType.GENERAL;
        String lower = taskTypeNameEn.toLowerCase();
        if (lower.contains("water")) return NotificationType.WATERING_REMINDER;
        if (lower.contains("fertiliz")) return NotificationType.FERTILIZING_REMINDER;
        if (lower.contains("harvest")) return NotificationType.HARVEST_REMINDER;
        return NotificationType.GENERAL;
    }
}
