package group.g.graduation.backend.Security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import group.g.graduation.backend.Security.dto.PermissionDTO;
import group.g.graduation.backend.Security.dto.RoleDTO;
import group.g.graduation.backend.Security.model.Permission;
import group.g.graduation.backend.Security.model.Role;
import group.g.graduation.backend.Security.repository.PermissionRepository;
import group.g.graduation.backend.Security.repository.RoleRepository;
import group.g.graduation.backend.Security.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findByActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        if (!role.isActive()) {
            throw new RuntimeException("Role is inactive");
        }
        return mapToDto(role);
    }

    @Transactional
    public RoleDTO createRole(RoleDTO roleDTO) {
        if (roleRepository.existsByNameAndActiveTrue(roleDTO.getName())) {
            throw new IllegalArgumentException("Role name already exists");
        }

        Role role = new Role();
        role.setName(roleDTO.getName());
        role.setDescription(roleDTO.getDescription());
        role.setActive(true);

        // Assign permissions if provided
        if (roleDTO.getPermissions() != null && !roleDTO.getPermissions().isEmpty()) {
            Set<Permission> permissions = new HashSet<>();
            for (PermissionDTO permDto : roleDTO.getPermissions()) {
                Permission permission = permissionRepository.findById(permDto.getId())
                        .orElseThrow(() -> new RuntimeException("Permission not found"));
                if (!permission.isActive()) {
                    throw new IllegalArgumentException("Cannot assign inactive permission");
                }
                permissions.add(permission);
            }
            role.setPermissions(permissions);
        }

        Role savedRole = roleRepository.save(role);
        log.info("Role created: {}", savedRole.getName());
        return mapToDto(savedRole);
    }

    @Transactional
    public RoleDTO updateRole(Long id, RoleDTO roleDTO) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (!role.isActive()) {
            throw new RuntimeException("Cannot update inactive role");
        }

        // Check if name is being changed and if it already exists
        if (!role.getName().equals(roleDTO.getName()) &&
                roleRepository.existsByNameAndActiveTrue(roleDTO.getName())) {
            throw new IllegalArgumentException("Role name already exists");
        }

        role.setName(roleDTO.getName());
        role.setDescription(roleDTO.getDescription());

        // Update permissions if provided
        if (roleDTO.getPermissions() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (PermissionDTO permDto : roleDTO.getPermissions()) {
                Permission permission = permissionRepository.findById(permDto.getId())
                        .orElseThrow(() -> new RuntimeException("Permission not found"));
                if (!permission.isActive()) {
                    throw new IllegalArgumentException("Cannot assign inactive permission");
                }
                permissions.add(permission);
            }
            role.setPermissions(permissions);
        }

        Role updatedRole = roleRepository.save(role);
        log.info("Role updated: {}", updatedRole.getId());
        return mapToDto(updatedRole);
    }


    // Add these methods to your existing RoleService class

@Transactional(readOnly = true)
public RoleDTO getRoleByName(String name) {
    Role role = roleRepository.findByName(name)
            .orElseThrow(() -> new RuntimeException("Role not found with name: " + name));
    if (!role.isActive()) {
        throw new RuntimeException("Role is inactive");
    }
    return mapToDto(role);
}

@Transactional
public RoleDTO activateRole(Long id) {
    Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));
    role.setActive(true);
    Role activatedRole = roleRepository.save(role);
    return mapToDto(activatedRole);
}

@Transactional
public RoleDTO deactivateRole(Long id) {
    Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));
    role.setActive(false);
    Role deactivatedRole = roleRepository.save(role);
    return mapToDto(deactivatedRole);
}

// Add these methods to your existing RoleService class

/**
 * Delete a role (soft delete by setting active to false)
 */
@Transactional
@CacheEvict(value = "roles", allEntries = true)
public void deleteRole(Long id) {
    Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
    
    // Check if role is being used by any users
    if (isRoleInUse(id)) {
        throw new IllegalStateException("Cannot delete role that is assigned to users");
    }
    
    // Soft delete by setting active to false
    role.setActive(false);
    roleRepository.save(role);
    
    log.info("Deleted (deactivated) role: {}", role.getName());
}

/**
 * Assign permissions to a role
 */
@Transactional
@CacheEvict(value = "roles", allEntries = true)
public RoleDTO assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
    Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));
    
    if (!role.isActive()) {
        throw new RuntimeException("Cannot update inactive role");
    }
    
    Set<Permission> permissions = new HashSet<>();
    for (Long permissionId : permissionIds) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found with id: " + permissionId));
        if (!permission.isActive()) {
            throw new IllegalArgumentException("Cannot assign inactive permission: " + permission.getName());
        }
        permissions.add(permission);
    }
    
    // Add new permissions to existing ones (not replace)
    role.getPermissions().addAll(permissions);
    
    Role updatedRole = roleRepository.save(role);
    log.info("Assigned {} permissions to role: {}", permissions.size(), role.getName());
    
    return mapToDto(updatedRole);
}

/**
 * Add a single permission to a role
 */
@Transactional
@CacheEvict(value = "roles", allEntries = true)
public RoleDTO addPermissionToRole(Long roleId, Long permissionId) {
    Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));
    
    if (!role.isActive()) {
        throw new RuntimeException("Cannot update inactive role");
    }
    
    Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new RuntimeException("Permission not found with id: " + permissionId));
    
    if (!permission.isActive()) {
        throw new IllegalArgumentException("Cannot assign inactive permission: " + permission.getName());
    }
    
    role.getPermissions().add(permission);
    Role updatedRole = roleRepository.save(role);
    log.info("Added permission {} to role: {}", permission.getName(), role.getName());
    
    return mapToDto(updatedRole);
}

/**
 * Remove a permission from a role
 */
@Transactional
@CacheEvict(value = "roles", allEntries = true)
public RoleDTO removePermissionFromRole(Long roleId, Long permissionId) {
    Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));
    
    if (!role.isActive()) {
        throw new RuntimeException("Cannot update inactive role");
    }
    
    Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new RuntimeException("Permission not found with id: " + permissionId));
    
    // Remove permission from role
    boolean removed = role.getPermissions().remove(permission);
    
    if (!removed) {
        throw new IllegalArgumentException("Permission is not assigned to this role");
    }
    
    Role updatedRole = roleRepository.save(role);
    log.info("Removed permission {} from role: {}", permission.getName(), role.getName());
    
    return mapToDto(updatedRole);
}

/**
 * Check if a role is being used by any users
 */
private boolean isRoleInUse(Long roleId) {
    return userRepository.isRoleInUse(roleId);
}

@Transactional(readOnly = true)
public List<String> getRolePermissions(Long id) {
    Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));
    return role.getPermissions().stream()
            .filter(Permission::isActive)
            .map(Permission::getName)
            .collect(Collectors.toList());
}

    @Transactional(readOnly = true)
    public List<String> getPermissionsForRole(String roleName) {
        return roleRepository.findByName(roleName)
                .map(role -> role.getPermissions().stream()
                        .filter(Permission::isActive)
                        .map(Permission::getName)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    private RoleDTO mapToDto(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());

        Set<PermissionDTO> permissionDTOs = role.getPermissions().stream()
                .filter(Permission::isActive)
                .map(permission -> new PermissionDTO(
                        permission.getId(),
                        permission.getName(),
                        permission.getDescription()
                ))
                .collect(Collectors.toSet());

        dto.setPermissions(permissionDTOs);
        return dto;
    }
}