package group.g.graduation.backend.Security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Permission entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Permission information")
public class PermissionDTO {
    
    @Schema(description = "Permission ID", example = "1")
    private Long id;
    
    @NotBlank(message = "Permission name is required")
    @Size(min = 3, max = 100, message = "Permission name must be between 3 and 100 characters")
    @Schema(description = "Permission name", example = "user:read", required = true)
    private String name;
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Schema(description = "Permission description", example = "Permission to read user information")
    private String description;
    
    @Schema(description = "Whether the permission is active", example = "true")
    @JsonProperty("isActive")
    private boolean active = true;
    
    /**
     * Constructor with essential fields
     */
    public PermissionDTO(String name, String description) {
        this.name = name;
        this.description = description;
        this.active = true;
    }
    
    /**
     * Constructor with id, name and description
     */
    public PermissionDTO(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = true;
    }
    
    /**
     * Get display name for the permission
     */
    public String getDisplayName() {
        if (description != null && !description.isBlank()) {
            return description;
        }
        return name;
    }
    
    /**
     * Check if this permission is for a specific resource
     */
    public boolean isResourcePermission(String resource) {
        return name != null && name.startsWith(resource + ":");
    }
    
    /**
     * Get the action part of the permission name (after the colon)
     */
    public String getAction() {
        if (name == null || !name.contains(":")) {
            return name;
        }
        return name.substring(name.indexOf(":") + 1);
    }
    
    /**
     * Get the resource part of the permission name (before the colon)
     */
    public String getResource() {
        if (name == null || !name.contains(":")) {
            return null;
        }
        return name.substring(0, name.indexOf(":"));
    }
}