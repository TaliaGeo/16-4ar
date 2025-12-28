package group.g.graduation.backend.Security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import group.g.graduation.backend.Security.model.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Role> findByActiveTrue();
    
    boolean existsByNameAndActiveTrue(String name);
}