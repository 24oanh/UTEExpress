package ltweb.repository;

import ltweb.entity.Role;
import ltweb.entity.RoleType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
Optional<Role> findByName(RoleType name);
    
    @Query("SELECT r FROM Role r WHERE r.name = :name")
    Optional<Role> findRoleByName(@Param("name") RoleType name);
}