package group.g.graduation.backend.Security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import group.g.graduation.backend.Security.dto.PermissionDTO;
import group.g.graduation.backend.Security.model.Permission;
import group.g.graduation.backend.Security.repository.PermissionRepository;
import group.g.graduation.backend.Security.repository.RoleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {
    
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    
    /**
     * Get all active permissions
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "permissions", key = "'active'")
    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findByActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all permissions including inactive ones
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "permissions", key = "'all'")
    public List<PermissionDTO> getAllPermissionsIncludingInactive() {
        return permissionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get permission by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "permissions", key = "#id")
    public PermissionDTO getPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found with id: " + id));
        return mapToDto(permission);
    }
    
    /**
     * Get permission by name
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "permissions", key = "'name:' + #name")
    public PermissionDTO getPermissionByName(String name) {
        Permission permission = permissionRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Permission not found with name: " + name));
        return mapToDto(permission);
    }
    
    /**
     * Get permissions by resource (e.g., all "user:*" permissions)
     */
    @Transactional(readOnly = true)
    public List<PermissionDTO> getPermissionsByResource(String resource) {
        return permissionRepository.findAll().stream()
                .filter(permission -> permission.getName().startsWith(resource + ":"))
                .filter(Permission::isActive)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Create a new permission
     */
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionDTO createPermission(PermissionDTO permissionDTO) {
        // Validate input
        validatePermissionDTO(permissionDTO);
        
        // Check if permission already exists
        if (permissionRepository.existsByName(permissionDTO.getName())) {
            throw new IllegalArgumentException("Permission with name '" + permissionDTO.getName() + "' already exists");
        }
        
        Permission permission = new Permission();
        permission.setName(permissionDTO.getName());
        permission.setDescription(permissionDTO.getDescription());
        permission.setActive(true);
        
        Permission savedPermission = permissionRepository.save(permission);
        log.info("Created permission: {} - {}", savedPermission.getName(), savedPermission.getDescription());
        
        return mapToDto(savedPermission);
    }
    
    /**
     * Update an existing permission
     */
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionDTO updatePermission(Long id, PermissionDTO permissionDTO) {
        // Validate input
        validatePermissionDTO(permissionDTO);
        
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found with id: " + id));
        
        // Check if name is being changed and if it already exists
        if (!permission.getName().equals(permissionDTO.getName()) && 
                permissionRepository.existsByName(permissionDTO.getName())) {
            throw new IllegalArgumentException("Permission with name '" + permissionDTO.getName() + "' already exists");
        }
        
        permission.setName(permissionDTO.getName());
        permission.setDescription(permissionDTO.getDescription());
        
        Permission updatedPermission = permissionRepository.save(permission);
        log.info("Updated permission: {} - {}", updatedPermission.getName(), updatedPermission.getDescription());
        
        return mapToDto(updatedPermission);
    }
    
    /**
     * Delete a permission (soft delete by setting active to false)
     */
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found with id: " + id));
        
        // Check if permission is being used by any role
        if (isPermissionUsedByRoles(id)) {
            throw new IllegalStateException("Cannot delete permission that is assigned to active roles");
        }
        
        // Check if permission is being used by security endpoints
        if (isPermissionUsedBySecurityEndpoints(id)) {
            throw new IllegalStateException("Cannot delete permission that is used by security endpoints");
        }
        
        // Soft delete by setting active to false
        permission.setActive(false);
        permissionRepository.save(permission);
        
        log.info("Deleted (deactivated) permission: {}", permission.getName());
    }
    
    /**
     * Activate a permission
     */
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionDTO activatePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found with id: " + id));
        
        permission.setActive(true);
        Permission activatedPermission = permissionRepository.save(permission);
        
        log.info("Activated permission: {}", activatedPermission.getName());
        return mapToDto(activatedPermission);
    }
    
    /**
     * Deactivate a permission
     */
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public PermissionDTO deactivatePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found with id: " + id));
        
        // Check if permission is being used by any active role
        if (isPermissionUsedByActiveRoles(id)) {
            throw new IllegalStateException("Cannot deactivate permission that is assigned to active roles");
        }
        
        // Check if permission is being used by active security endpoints
        if (isPermissionUsedByActiveSecurityEndpoints(id)) {
            throw new IllegalStateException("Cannot deactivate permission that is used by active security endpoints");
        }
        
        permission.setActive(false);
        Permission deactivatedPermission = permissionRepository.save(permission);
        
        log.info("Deactivated permission: {}", deactivatedPermission.getName());
        return mapToDto(deactivatedPermission);
    }
    
    /**
     * Bulk create permissions
     */
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public List<PermissionDTO> bulkCreatePermissions(List<PermissionDTO> permissionDTOs) {
        List<PermissionDTO> createdPermissions = new ArrayList<>();
        List<String> skippedPermissions = new ArrayList<>();
        
        for (PermissionDTO dto : permissionDTOs) {
            try {
                validatePermissionDTO(dto);
                
                if (!permissionRepository.existsByName(dto.getName())) {
                    Permission permission = new Permission();
                    permission.setName(dto.getName());
                    permission.setDescription(dto.getDescription());
                    permission.setActive(true);
                    
                    Permission savedPermission = permissionRepository.save(permission);
                    createdPermissions.add(mapToDto(savedPermission));
                    
                    log.info("Bulk created permission: {}", savedPermission.getName());
                } else {
                    skippedPermissions.add(dto.getName());
                }
            } catch (Exception e) {
                log.error("Error creating permission {}: {}", dto.getName(), e.getMessage());
                skippedPermissions.add(dto.getName());
            }
        }
        
        if (!skippedPermissions.isEmpty()) {
            log.info("Skipped existing/invalid permissions: {}", skippedPermissions);
        }
        
        log.info("Bulk created {} permissions, skipped {}", createdPermissions.size(), skippedPermissions.size());
        return createdPermissions;
    }
    
    /**
     * Search permissions by name or description
     */
    @Transactional(readOnly = true)
    public List<PermissionDTO> searchPermissions(String searchTerm) {
        String lowerSearchTerm = searchTerm.toLowerCase();
        return permissionRepository.findByActiveTrue().stream()
                .filter(permission -> 
                    permission.getName().toLowerCase().contains(lowerSearchTerm) ||
                    (permission.getDescription() != null && 
                     permission.getDescription().toLowerCase().contains(lowerSearchTerm)))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get permissions that are not assigned to any role
     */
    @Transactional(readOnly = true)
    public List<PermissionDTO> getUnassignedPermissions() {
        List<Permission> allPermissions = permissionRepository.findByActiveTrue();
        
        return allPermissions.stream()
                .filter(permission -> !isPermissionUsedByRoles(permission.getId()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get permission usage statistics
     */
    @Transactional(readOnly = true)
    public PermissionUsageDTO getPermissionUsage(Long permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found with id: " + permissionId));
        
        long roleCount = countRolesUsingPermission(permissionId);
        long endpointCount = countSecurityEndpointsUsingPermission(permissionId);
        
        return PermissionUsageDTO.builder()
                .permissionId(permissionId)
                .permissionName(permission.getName())
                .rolesCount(roleCount)
                .securityEndpointsCount(endpointCount)
                .isInUse(roleCount > 0 || endpointCount > 0)
                .build();
    }
    
    // Private helper methods
    
    /**
     * Validate permission DTO
     */
    private void validatePermissionDTO(PermissionDTO permissionDTO) {
        if (permissionDTO.getName() == null || permissionDTO.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Permission name is required");
        }
        
        // Validate permission name format (e.g., "resource:action")
        if (!isValidPermissionName(permissionDTO.getName())) {
            throw new IllegalArgumentException("Invalid permission name format. Expected format: 'resource:action'");
        }
    }
    
    /**
     * Validate permission name format
     */
    private boolean isValidPermissionName(String name) {
        // Permission names should follow the pattern: resource:action
        // Examples: user:read, course:create, admin:access
        return name.matches("^[a-z][a-z0-9]*:[a-z][a-z0-9]*$");
    }
    
    /**
     * Check if permission is used by any role
     */
    private boolean isPermissionUsedByRoles(Long permissionId) {
        return roleRepository.findAll().stream()
                .anyMatch(role -> role.getPermissions().stream()
                        .anyMatch(permission -> permission.getId().equals(permissionId)));
    }
    
    /**
     * Check if permission is used by any active role
     */
    private boolean isPermissionUsedByActiveRoles(Long permissionId) {
        return roleRepository.findByActiveTrue().stream()
                .anyMatch(role -> role.getPermissions().stream()
                        .anyMatch(permission -> permission.getId().equals(permissionId)));
    }
    
    /**
     * Check if permission is used by any security endpoint
     * Note: SecurityEndpoint feature removed for monolithic setup
     */
    private boolean isPermissionUsedBySecurityEndpoints(Long permissionId) {
        return false; // SecurityEndpoint feature removed
    }
    
    /**
     * Check if permission is used by any active security endpoint
     * Note: SecurityEndpoint feature removed for monolithic setup
     */
    private boolean isPermissionUsedByActiveSecurityEndpoints(Long permissionId) {
        return false; // SecurityEndpoint feature removed
    }
    
    /**
     * Count roles using the permission
     */
    private long countRolesUsingPermission(Long permissionId) {
        return roleRepository.findAll().stream()
                .filter(role -> role.getPermissions().stream()
                        .anyMatch(permission -> permission.getId().equals(permissionId)))
                .count();
    }
    
    /**
     * Count security endpoints using the permission
     * Note: SecurityEndpoint feature removed for monolithic setup
     */
    private long countSecurityEndpointsUsingPermission(Long permissionId) {
        return 0; // SecurityEndpoint feature removed
    }
    
    /**
     * Map Permission entity to DTO
     */
    private PermissionDTO mapToDto(Permission permission) {
        return PermissionDTO.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .active(permission.isActive())
                .build();
    }
    
    /**
     * Map DTO to Permission entity
     */
    private Permission mapToEntity(PermissionDTO dto) {
        Permission permission = new Permission();
        permission.setId(dto.getId());
        permission.setName(dto.getName());
        permission.setDescription(dto.getDescription());
        permission.setActive(dto.isActive());
        return permission;
    }
    
    /**
     * Permission usage statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PermissionUsageDTO {
        private Long permissionId;
        private String permissionName;
        private long rolesCount;
        private long securityEndpointsCount;
        private boolean isInUse;
    }
}