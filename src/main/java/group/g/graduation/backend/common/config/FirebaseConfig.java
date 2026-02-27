package group.g.graduation.backend.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Firebase Configuration - إعداد Firebase للإشعارات
 * 
 * يحتاج ملف service account JSON من Firebase Console:
 * 1. اذهب إلى Firebase Console → Project Settings → Service Accounts
 * 2. اضغط "Generate new private key"
 * 3. احفظ الملف باسم firebase-service-account.json
 * 4. ضعه في src/main/resources/ أو حدد مساره في FIREBASE_CONFIG_PATH
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.config-path:#{null}}")
    private String configPath;

    @PostConstruct
    public void initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream serviceAccount = getServiceAccountStream();
                
                if (serviceAccount == null) {
                    log.warn("⚠️ Firebase service account not found — FCM push notifications disabled");
                    log.warn("📄 To enable: place firebase-service-account.json in src/main/resources/");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase initialized successfully — FCM push notifications enabled");

            } catch (IOException e) {
                log.warn("⚠️ Failed to initialize Firebase: {} — FCM push notifications disabled", e.getMessage());
            }
        }
    }

    private InputStream getServiceAccountStream() {
        // 1. Try explicit config path (environment variable)
        if (configPath != null && !configPath.isBlank()) {
            try {
                Path path = Path.of(configPath);
                if (Files.exists(path)) {
                    log.info("📄 Loading Firebase config from: {}", configPath);
                    return new FileInputStream(configPath);
                }
            } catch (IOException e) {
                log.warn("⚠️ Could not read Firebase config from {}: {}", configPath, e.getMessage());
            }
        }

        // 2. Try classpath (src/main/resources/)
        try {
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
            if (resource.exists()) {
                log.info("📄 Loading Firebase config from classpath");
                return resource.getInputStream();
            }
        } catch (IOException e) {
            log.warn("⚠️ Could not read Firebase config from classpath: {}", e.getMessage());
        }

        return null;
    }
}
