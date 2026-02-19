package group.g.graduation.backend.admin.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Context holder for user information during audit logging
 * Extracted from SecurityContext in main thread before async operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditUserContext {
    private Long userId;
    private String userName;
    private String userEmail;
    
    public static AuditUserContext system() {
        return AuditUserContext.builder()
                .userId(-1L)
                .userName("SYSTEM")
                .userEmail("system@internal")
                .build();
    }
    
    public boolean isSystem() {
        return userId != null && userId == -1L;
    }
}
