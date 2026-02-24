package group.g.graduation.backend.common.email;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller لاختبار إرسال البريد الإلكتروني - بيئة التطوير فقط
 * Email Testing Controller - Development Environment Only
 * 
 * ⚠️  محمي بصلاحيات الأدمن ويعمل فقط في بيئة التطوير
 * ⚠️  Protected by admin permissions and only works in dev environment
 */
@RestController
@RequestMapping("/api/admin/email/test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Email Testing", description = "🧪 اختبار البريد الإلكتروني (بيئة التطوير فقط)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Profile({"dev", "local", "development"})
@ConditionalOnProperty(name = "app.email.testing.enabled", havingValue = "true", matchIfMissing = false)
public class EmailController {

    private final EmailService emailService;

    /**
     * اختبار إرسال بريد إلكتروني بسيط
     */
    @PostMapping("/simple")
    @Operation(summary = "اختبار إرسال بريد بسيط", 
               description = "⚠️ للأدمن فقط - بيئة التطوير")
    public ResponseEntity<Map<String, String>> testSimpleEmail(
            @Parameter(description = "البريد الإلكتروني المستقبل", required = true) 
            @RequestParam String to,
            @Parameter(description = "موضوع الرسالة") 
            @RequestParam(required = false) String subject,
            @Parameter(description = "نص الرسالة") 
            @RequestParam(required = false) String message) {
        
        // Validate input
        if (to == null || to.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "البريد الإلكتروني مطلوب"
            ));
        }
        
        try {
            String finalSubject = (subject != null && !subject.trim().isEmpty()) 
                ? subject : "اختبار من تطبيق غرسي";
            String finalMessage = (message != null && !message.trim().isEmpty()) 
                ? message : "مرحباً! هذا بريد تجريبي من تطبيق غرسي 🌱";
                
            emailService.sendSimpleEmail(to, finalSubject, finalMessage);
            log.info("Test simple email sent to: {} by admin", to);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "تم إرسال البريد بنجاح إلى " + to,
                "environment", "development"
            ));
        } catch (Exception e) {
            log.error("Failed to send test email to: {}", to, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "فشل إرسال البريد: " + e.getMessage()
            ));
        }
    }

    /**
     * اختبار إرسال بريد ترحيب
     */
    @PostMapping("/welcome")
    @Operation(summary = "اختبار بريد الترحيب", 
               description = "⚠️ للأدمن فقط - بيئة التطوير")
    public ResponseEntity<Map<String, String>> testWelcomeEmail(
            @Parameter(description = "البريد الإلكتروني المستقبل", required = true) 
            @RequestParam String to,
            @Parameter(description = "اسم المستخدم", required = true) 
            @RequestParam String userName) {
        
        // Validate input
        if (to == null || to.trim().isEmpty() || userName == null || userName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "البريد الإلكتروني واسم المستخدم مطلوبان"
            ));
        }
        
        try {
            emailService.sendWelcomeEmail(to, userName);
            log.info("Test welcome email sent to: {} for user: {} by admin", to, userName);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "تم إرسال بريد الترحيب بنجاح إلى " + to,
                "environment", "development"
            ));
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {} for user: {}", to, userName, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "فشل إرسال البريد: " + e.getMessage()
            ));
        }
    }

    /**
     * اختبار إرسال رمز التحقق
     */
    @PostMapping("/verification")
    @Operation(summary = "اختبار رمز التحقق", 
               description = "⚠️ للأدمن فقط - بيئة التطوير")
    public ResponseEntity<Map<String, String>> testVerificationEmail(
            @Parameter(description = "البريد الإلكتروني المستقبل", required = true) 
            @RequestParam String to,
            @Parameter(description = "رمز التحقق", required = true) 
            @RequestParam String code,
            @Parameter(description = "اسم المستخدم", required = true) 
            @RequestParam String userName) {
        
        // Validate input
        if (to == null || to.trim().isEmpty() || code == null || code.trim().isEmpty() || 
            userName == null || userName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "جميع الحقول مطلوبة"
            ));
        }
        
        try {
            emailService.sendVerificationCode(to, code, userName);
            log.info("Test verification email sent to: {} for user: {} by admin", to, userName);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "تم إرسال رمز التحقق بنجاح إلى " + to,
                "environment", "development",
                "code", code
            ));
        } catch (Exception e) {
            log.error("Failed to send verification email to: {} for user: {}", to, userName, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "فشل إرسال البريد: " + e.getMessage()
            ));
        }
    }

    /**
     * اختبار إرسال تذكير السقي
     */
    @PostMapping("/watering-reminder")
    @Operation(summary = "اختبار تذكير السقي", 
               description = "⚠️ للأدمن فقط - بيئة التطوير")
    public ResponseEntity<Map<String, String>> testWateringReminder(
            @Parameter(description = "البريد الإلكتروني المستقبل", required = true) 
            @RequestParam String to,
            @Parameter(description = "اسم المستخدم", required = true) 
            @RequestParam String userName,
            @Parameter(description = "اسم النبتة", required = true) 
            @RequestParam String plantName) {
        
        // Validate input
        if (to == null || to.trim().isEmpty() || userName == null || userName.trim().isEmpty() || 
            plantName == null || plantName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "جميع الحقول مطلوبة"
            ));
        }
        
        try {
            emailService.sendWateringReminder(to, userName, plantName);
            log.info("Test watering reminder sent to: {} for plant: {} by admin", to, plantName);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "تم إرسال تذكير السقي بنجاح إلى " + to,
                "environment", "development"
            ));
        } catch (Exception e) {
            log.error("Failed to send watering reminder to: {} for plant: {}", to, plantName, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "فشل إرسال البريد: " + e.getMessage()
            ));
        }
    }

    /**
     * اختبار إرسال رابط إعادة تعيين كلمة المرور
     */
    @PostMapping("/password-reset")
    @Operation(summary = "اختبار رابط إعادة تعيين كلمة المرور", 
               description = "⚠️ للأدمن فقط - بيئة التطوير")
    public ResponseEntity<Map<String, String>> testPasswordResetEmail(
            @Parameter(description = "البريد الإلكتروني المستقبل", required = true) 
            @RequestParam String to,
            @Parameter(description = "اسم المستخدم", required = true) 
            @RequestParam String userName,
            @Parameter(description = "رابط إعادة التعيين", required = true) 
            @RequestParam String resetLink) {
        
        // Validate input
        if (to == null || to.trim().isEmpty() || userName == null || userName.trim().isEmpty() || 
            resetLink == null || resetLink.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "جميع الحقول مطلوبة"
            ));
        }
        
        // Basic URL validation
        if (!resetLink.startsWith("http://") && !resetLink.startsWith("https://")) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "رابط إعادة التعيين غير صالح"
            ));
        }
        
        try {
            emailService.sendPasswordResetEmail(to, resetLink, userName);
            log.info("Test password reset email sent to: {} for user: {} by admin", to, userName);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "تم إرسال رابط إعادة التعيين بنجاح إلى " + to,
                "environment", "development"
            ));
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {} for user: {}", to, userName, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "فشل إرسال البريد: " + e.getMessage()
            ));
        }
    }
}