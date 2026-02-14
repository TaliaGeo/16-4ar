package group.g.graduation.backend.common.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.util.Map;

/**
 * خدمة إرسال البريد الإلكتروني
 * Email Service for sending various types of emails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${email.from}")
    private String fromEmail;

    @Value("${email.from-name}")
    private String fromName;

    /**
     * إرسال بريد إلكتروني نصي بسيط
     * Send a simple text email
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromEmail + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
            throw new EmailSendException("Failed to send email", e);
        }
    }

    /**
     * إرسال بريد إلكتروني HTML
     * Send an HTML email
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new EmailSendException("Failed to send HTML email", e);
        }
    }

    /**
     * إرسال بريد إلكتروني باستخدام قالب
     * Send an email using a Thymeleaf template
     */
    @Async
    public void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            
            String htmlContent = templateEngine.process("email/" + templateName, context);
            sendHtmlEmail(to, subject, htmlContent);
            
            log.info("Templated email '{}' sent successfully to: {}", templateName, to);
        } catch (Exception e) {
            log.error("Failed to send templated email '{}' to: {}", templateName, to, e);
            throw new EmailSendException("Failed to send templated email", e);
        }
    }

    /**
     * إرسال بريد إلكتروني مع مرفقات
     * Send an email with attachments
     */
    @Async
    public void sendEmailWithAttachment(String to, String subject, String text, File attachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);
            helper.addAttachment(attachment.getName(), attachment);
            
            mailSender.send(message);
            log.info("Email with attachment sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to: {}", to, e);
            throw new EmailSendException("Failed to send email with attachment", e);
        }
    }

    /**
     * إرسال رمز التحقق عبر البريد الإلكتروني
     * Send verification code email
     */
    @Async
    public void sendVerificationCode(String to, String code, String userName) {
        Map<String, Object> variables = Map.of(
            "userName", userName != null ? userName : "المستخدم",
            "verificationCode", code,
            "appName", "غرسي"
        );
        sendTemplatedEmail(to, "رمز التحقق - غرسي", "verification-code", variables);
    }

    /**
     * إرسال بريد إلكتروني للترحيب
     * Send welcome email
     */
    @Async
    public void sendWelcomeEmail(String to, String userName) {
        Map<String, Object> variables = Map.of(
            "userName", userName != null ? userName : "المستخدم",
            "appName", "غرسي"
        );
        sendTemplatedEmail(to, "مرحباً بك في غرسي! 🌱", "welcome", variables);
    }

    /**
     * إرسال بريد إلكتروني لإعادة تعيين كلمة المرور
     * Send password reset email
     */
    @Async
    public void sendPasswordResetEmail(String to, String resetLink, String userName) {
        Map<String, Object> variables = Map.of(
            "userName", userName != null ? userName : "المستخدم",
            "resetLink", resetLink,
            "appName", "غرسي"
        );
        sendTemplatedEmail(to, "إعادة تعيين كلمة المرور - غرسي", "password-reset", variables);
    }

    /**
     * إرسال تذكير بالسقي
     * Send watering reminder
     */
    @Async
    public void sendWateringReminder(String to, String userName, String plantName) {
        Map<String, Object> variables = Map.of(
            "userName", userName != null ? userName : "المستخدم",
            "plantName", plantName,
            "appName", "غرسي"
        );
        sendTemplatedEmail(to, "تذكير بسقي " + plantName + " 💧", "watering-reminder", variables);
    }

    /**
     * إرسال تذكير بالتسميد
     * Send fertilizing reminder
     */
    @Async
    public void sendFertilizingReminder(String to, String userName, String plantName) {
        Map<String, Object> variables = Map.of(
            "userName", userName != null ? userName : "المستخدم",
            "plantName", plantName,
            "appName", "غرسي"
        );
        sendTemplatedEmail(to, "تذكير بتسميد " + plantName + " 🌿", "fertilizing-reminder", variables);
    }
}
