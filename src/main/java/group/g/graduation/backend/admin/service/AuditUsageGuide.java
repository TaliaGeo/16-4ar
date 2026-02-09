package group.g.graduation.backend.admin.service;

/**
 * ============================================
 * دليل استخدام AuditService بعد التعديلات
 * ============================================
 * 
 * ## المشكلة القديمة:
 * كان @Async يستدعى في thread جديد وSecurityContext لا ينتقل تلقائياً
 * مما يسبب user_id = null في audit_logs
 * 
 * ## الحل:
 * 1. استخراج user context في main thread قبل async call
 * 2. تمرير AuditUserContext كـ parameter للـ async methods
 * 3. استخدام REQUIRES_NEW لعدم rollback العملية الأساسية عند فشل audit
 * 4. wrap save في try/catch لعدم رمي exceptions
 * 
 * ## طريقة الاستخدام:
 * 
 * ### 1. في Controllers (تلقائياً عبر AOP):
 * ```java
 * @PostMapping
 * @Auditable(action = AuditAction.CREATE, entityType = "PLANT")
 * public ResponseEntity<PlantResponse> createPlant(@RequestBody PlantRequest request) {
 *     // الكود العادي - الـ AuditAspect سيسجل تلقائياً
 *     return plantService.createPlant(request);
 * }
 * ```
 * 
 * ### 2. في Services (يدوياً):
 * 
 * #### الطريقة 1: استخدام auto-extract (backward compatible):
 * ```java
 * @Service
 * public class MyService {
 *     private final AuditService auditService;
 *     
 *     public void doSomething() {
 *         // AuditService سيستخرج user تلقائياً من SecurityContext
 *         auditService.logAction(
 *             AuditAction.UPDATE, 
 *             "PLANT", 
 *             plantId, 
 *             "تحديث النبتة",
 *             oldPlant, 
 *             newPlant
 *         );
 *     }
 * }
 * ```
 * 
 * #### الطريقة 2: استخراج يدوي (موصى به للـ batch operations):
 * ```java
 * @Service
 * public class MyService {
 *     private final AuditService auditService;
 *     
 *     public void processBatch(List<Plant> plants) {
 *         // استخرج user مرة واحدة قبل الـ loop
 *         AuditUserContext userContext = auditService.getCurrentUser();
 *         
 *         for (Plant plant : plants) {
 *             // استخدم نفس userContext لكل العمليات
 *             auditService.logAction(
 *                 userContext,
 *                 AuditAction.UPDATE,
 *                 "PLANT",
 *                 plant.getId(),
 *                 "تحديث النبتة",
 *                 null,
 *                 plant
 *             );
 *         }
 *     }
 * }
 * ```
 * 
 * #### الطريقة 3: تسجيل عمليات فاشلة:
 * ```java
 * public void createPlant(PlantRequest request) {
 *     AuditUserContext userContext = auditService.getCurrentUser();
 *     
 *     try {
 *         Plant plant = plantRepository.save(new Plant(request));
 *         
 *         auditService.logAction(
 *             userContext,
 *             AuditAction.CREATE,
 *             "PLANT",
 *             plant.getId(),
 *             "إنشاء نبتة جديدة",
 *             null,
 *             plant
 *         );
 *         
 *         return plant;
 *     } catch (Exception e) {
 *         // تسجيل الفشل مع نفس userContext
 *         auditService.logFailedAction(
 *             userContext,
 *             AuditAction.CREATE,
 *             "PLANT",
 *             null,
 *             "محاولة إنشاء نبتة: " + request.getName(),
 *             e.getMessage()
 *         );
 *         throw e;
 *     }
 * }
 * ```
 * 
 * ### 3. عمليات النظام (بدون user):
 * ```java
 * public void systemCleanup() {
 *     // استخدم SYSTEM context
 *     AuditUserContext systemContext = AuditUserContext.system();
 *     
 *     auditService.logAction(
 *         systemContext,
 *         AuditAction.DELETE,
 *         "TEMP_FILES",
 *         null,
 *         "تنظيف الملفات المؤقتة",
 *         null,
 *         null
 *     );
 * }
 * ```
 * 
 * ## الفوائد:
 * 1. ✅ user_id دائماً صحيح (من admin token)
 * 2. ✅ لا rollback للعملية الأساسية عند فشل audit
 * 3. ✅ REQUIRES_NEW transaction منفصلة للـ audit
 * 4. ✅ thread-safe (لا يعتمد على SecurityContext في async thread)
 * 5. ✅ backward compatible (الطرق القديمة ما زالت تعمل)
 * 
 * ## التعديلات المطلوبة:
 * 1. ✅ AuditService: إضافة getCurrentUser() و AuditUserContext parameter
 * 2. ✅ AuditAspect: استخراج userContext قبل proceed()
 * 3. ⚠️  Controllers/Services يدوية: تحديث إذا كنت تستدعي audit مباشرة
 * 
 * ## ملاحظات:
 * - AuditAspect يستخرج user تلقائياً لكل admin controllers
 * - إذا كنت تستخدم audit يدوياً، استخدم getCurrentUser() قبل async operations
 * - userId = -1 يعني عملية نظام (SYSTEM)
 * - فشل حفظ audit لن يؤثر على العملية الأساسية (logged as error فقط)
 */
public class AuditUsageGuide {
    // هذا الملف للتوثيق فقط - لا تستدعيه
}
