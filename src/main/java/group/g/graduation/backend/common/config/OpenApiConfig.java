package group.g.graduation.backend.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration - إعدادات توثيق الـ API
 * 
 * تطبيق غرسة - Gharsih App
 * نظام إدارة النباتات الطبية الفلسطينية
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Authentication - أدخل الـ Token بدون كلمة 'Bearer'"
)
public class OpenApiConfig {
    
    @Value("${server.port:8081}")
    private String serverPort;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .tags(apiTags())
                .externalDocs(externalDocumentation());
    }
    
    private Info apiInfo() {
        return new Info()
                .title("غرسة - Gharsih API")
                .description("""
                    ## 🌿 تطبيق غرسة - نظام النباتات الطبية الفلسطينية
                    
                    ### وصف المشروع
                    تطبيق شامل لإدارة وتوثيق النباتات الطبية الفلسطينية التقليدية.
                    يوفر معلومات عن النباتات، نصائح الزراعة، والتقويم الزراعي.
                    
                    ### الميزات الرئيسية
                    - 🌱 **إدارة النباتات**: إضافة، تعديل، حذف النباتات مع الصور
                    - 📅 **التقويم الزراعي**: مواعيد الزراعة والحصاد لكل نبتة
                    - 💬 **الاقتباسات**: حكم ونصائح زراعية يومية
                    - ❓ **أسئلة الزراعة**: نظام توصيات ذكي للنباتات
                    - 👥 **إدارة المستخدمين**: تسجيل، تسجيل دخول، أدوار وصلاحيات
                    - 🔔 **الإشعارات**: نظام إشعارات للتذكيرات والأخبار
                    - 📊 **لوحة التحكم**: إحصائيات شاملة للمشرفين
                    - 📝 **سجلات التدقيق**: تتبع جميع العمليات
                    
                    ### المصادقة
                    يستخدم التطبيق **JWT (JSON Web Tokens)** للمصادقة.
                    1. سجل دخول عبر `/api/auth/login`
                    2. استخدم الـ Token في الـ Header: `Authorization: Bearer <token>`
                    
                    ### المصادقة
                    استخدم بيانات الدخول المقدمة من مدير النظام.
                    """)
                .version("1.0.0")
                .contact(apiContact())
                .license(apiLicense());
    }
    
    private Contact apiContact() {
        return new Contact()
                .name("فريق تطوير غرسة")
                .email("support@gharsih.ps")
                .url("https://gharsih.ps");
    }
    
    private License apiLicense() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }
    
    private List<Server> apiServers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("🖥️ خادم التطوير المحلي - Local Development"),
                new Server()
                        .url("https://api.gharsih.ps")
                        .description("🌐 خادم الإنتاج - Production Server")
        );
    }
    
    private List<Tag> apiTags() {
        return List.of(
                // Authentication & Users
                new Tag().name("Authentication").description("🔐 المصادقة - تسجيل الدخول والخروج"),
                new Tag().name("Users").description("👤 المستخدمين - إدارة الحسابات الشخصية"),
                
                // Admin Tags
                new Tag().name("Admin - Plants").description("🌱 إدارة النباتات - Plants Management"),
                new Tag().name("Admin - Plant Images").description("🖼️ إدارة صور النباتات - Plant Images"),
                new Tag().name("Admin - Quotes").description("💬 إدارة الاقتباسات - Quotes Management"),
                new Tag().name("Admin - Months").description("📅 إدارة الأشهر - Months Management"),
                new Tag().name("Admin - Task Types").description("📋 إدارة أنواع المهام - Task Types"),
                new Tag().name("Admin - Plant Tasks").description("✅ إدارة مهام النباتات - Plant Tasks"),
                new Tag().name("Admin - Planting Questions").description("❓ إدارة أسئلة الزراعة - Questions"),
                new Tag().name("Admin - Question Options").description("🔘 إدارة خيارات الأسئلة - Options"),
                new Tag().name("Admin - Plant Suitability").description("🎯 إدارة ملاءمة النباتات - Suitability"),
                new Tag().name("Admin - Users").description("👥 إدارة المستخدمين - Users Management"),
                new Tag().name("Admin - Notifications").description("🔔 إدارة الإشعارات - Notifications"),
                new Tag().name("Admin - Month Plants").description("🌿 إدارة نباتات الأشهر - Month Plants"),
                new Tag().name("Admin - Dashboard").description("📊 لوحة التحكم - Dashboard & Statistics"),
                new Tag().name("Admin - Audit Logs").description("📝 سجلات التدقيق - Audit Logs"),
                new Tag().name("Admin - Roles & Permissions").description("🔑 إدارة الأدوار والصلاحيات"),
                
                // Public Tags
                new Tag().name("Public - Plants").description("🌱 النباتات العامة - Public Plants API"),
                new Tag().name("Public - Quotes").description("💬 الاقتباسات العامة - Public Quotes"),
                new Tag().name("Public - Calendar").description("📅 التقويم الزراعي - Planting Calendar")
        );
    }
    
    private ExternalDocumentation externalDocumentation() {
        return new ExternalDocumentation()
                .description("📖 توثيق GitHub الكامل - Full GitHub Documentation")
                .url("https://github.com/Iman-Shakhtour/graduation-project-backend");
    }
}
