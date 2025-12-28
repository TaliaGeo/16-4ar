package group.g.graduation.backend.Security.repository;

import group.g.graduation.backend.Security.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    Optional<Permission> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Permission> findByActiveTrue();
    
    // For search functionality - using method naming
    List<Permission> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);
}