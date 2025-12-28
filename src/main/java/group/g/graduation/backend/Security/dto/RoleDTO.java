package group.g.graduation.backend.Security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data Transfer Object for Role entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role information with associated permissions")
public class RoleDTO {
    
    @Schema(description = "Role ID", example = "1")
    private Long id;
    
    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters")
    @Schema(description = "Role name", example = "ADMIN", required = true)
    private String name;
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Schema(description = "Role description", example = "Administrator with full access")
    private String description;
    
    @Schema(description = "Set of permissions associated with this role")
    @Builder.Default
    private Set<PermissionDTO> permissions = new HashSet<>();
    
    @Schema(description = "Whether the role is active", example = "true")
    @JsonProperty("isActive")
    @Builder.Default
    private boolean active = true;
    
    /**
     * Constructor with essential fields
     */
    public RoleDTO(String name, String description) {
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
        this.active = true;
    }
    
    /**
     * Constructor with id, name and description
     */
    public RoleDTO(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
        this.active = true;
    }
    
    /**
     * Add a permission to this role
     */
    public void addPermission(PermissionDTO permission) {
        if (permissions == null) {
            permissions = new HashSet<>();
        }
        permissions.add(permission);
    }
    
    /**
     * Remove a permission from this role
     */
    public void removePermission(PermissionDTO permission) {
        if (permissions != null) {
            permissions.remove(permission);
        }
    }
    
    /**
     * Check if this role has a specific permission
     */
    public boolean hasPermission(String permissionName) {
        return permissions != null && permissions.stream()
                .anyMatch(p -> permissionName.equals(p.getName()));
    }
    
    /**
     * Get permission names as a set of strings
     */
    public Set<String> getPermissionNames() {
        if (permissions == null) {
            return new HashSet<>();
        }
        return permissions.stream()
                .map(PermissionDTO::getName)
                .collect(Collectors.toSet());
    }
    
    /**
     * Get count of permissions
     */
    public int getPermissionCount() {
        return permissions != null ? permissions.size() : 0;
    }
    
    /**
     * Get display name for the role
     */
    public String getDisplayName() {
        if (description != null && !description.isBlank()) {
            return description;
        }
        return name;
    }
    
    /**
     * Check if this is an admin role
     */
    public boolean isAdminRole() {
        return "ADMIN".equalsIgnoreCase(name);
    }
    
    /**
     * Check if this is a regular user role
     */
    public boolean isUserRole() {
        return "USER".equalsIgnoreCase(name);
    }
    
    /**
     * Get permissions for a specific resource
     */
    public Set<PermissionDTO> getPermissionsForResource(String resource) {
        if (permissions == null) {
            return new HashSet<>();
        }
        return permissions.stream()
                .filter(p -> p.isResourcePermission(resource))
                .collect(Collectors.toSet());
    }
}