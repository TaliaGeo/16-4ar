package group.g.graduation.backend.common.email;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller لاختبار إرسال البريد الإلكتروني
 * Email Testing Controller
 */
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email", description = "واجهة اختبار البريد الإلكتروني")
public class EmailController {

    private final EmailService emailService;

    /**
     * اختبار إرسال بريد إلكتروني بسيط
     */
    @PostMapping("/test/simple")
    @Operation(summary = "اختبار إرسال بريد بسيط")
    public ResponseEntity<Map<String, String>> testSimpleEmail(
            @RequestParam String to,
            @RequestParam(defaultValue = "اختبار من تطبيق غرسي") String subject,
            @RequestParam(defaultValue = "مرحباً! هذا بريد تجريبي من تطبيق غرسي 🌱") String message) {
        
        try {
            emailService.sendSimpleEmail(to, subject, message);
            log.info("Test simple email sent to: {}", to);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "تم إرسال البريد بنجاح إلى " + to
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
    @PostMapping("/test/welcome")
    @Operation(summary = "اختبار إرسال بريد ترحيب")
    public ResponseEntity<Map<String, String>> testWelcomeEmail(
            @RequestParam String to,
            @RequestParam(defaultValue = "مستخدم جديد") String userName) {
        
        try {
            emailService.sendWelcomeEmail(to, userName);
            log.info("Test welcome email sent to: {}", to);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "تم إرسال بريد الترحيب بنجاح إلى " + to
            ));
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", to, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "فشل إرسال البريد: " + e.getMessage()
            ));
        }
    }

    /**
     * اختبار إرسال رمز التحقق
     */
    @PostMapping("/test/verification")
    @Operation(summary = "اختبار إرسال رمز التحقق")
    public ResponseEntity<Map<String, String>> testVerificationEmail(
            @RequestParam String to,
            @RequestParam(defaultValue = "123456") String code,
            @RequestParam(defaultValue = "مستخدم") String userName) {
        
        try {
            emailService.sendVerificationCode(to, code, userName);
            log.info("Test verification email sent to: {}", to);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "تم إرسال رمز التحقق بنجاح إلى " + to,
                "code", code
            ));
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", to, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "فشل إرسال البريد: " + e.getMessage()
            ));
        }
    }

    /**
     * اختبار إرسال تذكير السقي
     */
    @PostMapping("/test/watering-reminder")
    @Operation(summary = "اختبار إرسال تذكير السقي")
    public ResponseEntity<Map<String, String>> testWateringReminder(
            @RequestParam String to,
            @RequestParam(defaultValue = "مستخدم") String userName,
            @RequestParam(defaultValue = "نعناع") String plantName) {
        
        try {
            emailService.sendWateringReminder(to, userName, plantName);
            log.info("Test watering reminder sent to: {}", to);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "تم إرسال تذكير السقي بنجاح إلى " + to
            ));
        } catch (Exception e) {
            log.error("Failed to send watering reminder to: {}", to, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "فشل إرسال البريد: " + e.getMessage()
            ));
        }
    }

    /**
     * اختبار إرسال رابط إعادة تعيين كلمة المرور
     */
    @PostMapping("/test/password-reset")
    @Operation(summary = "اختبار إرسال رابط إعادة تعيين كلمة المرور")
    public ResponseEntity<Map<String, String>> testPasswordResetEmail(
            @RequestParam String to,
            @RequestParam(defaultValue = "مستخدم") String userName,
            @RequestParam(defaultValue = "https://gharsih.app/reset?token=test123") String resetLink) {
        
        try {
            emailService.sendPasswordResetEmail(to, resetLink, userName);
            log.info("Test password reset email sent to: {}", to);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "تم إرسال رابط إعادة التعيين بنجاح إلى " + to
            ));
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "فشل إرسال البريد: " + e.getMessage()
            ));
        }
    }
}
